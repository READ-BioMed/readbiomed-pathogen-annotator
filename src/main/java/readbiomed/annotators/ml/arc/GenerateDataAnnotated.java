package readbiomed.annotators.ml.arc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.token.type.Sentence;
import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.annotators.discourse.sdt.SDTClient;
import readbiomed.document.Section;

@Command(name = "GenerateData", mixinStandardHelpOptions = true, version = "GenerateData 0.1", description = "Generate data for ARC experiments.")
public class GenerateDataAnnotated implements Callable<Integer> {
	@Parameters(index = "0", description = "Input file name.")
	private String inputFileName;

	@Parameters(index = "1", description = "Output name.")
	private String outputFileName;

	@Parameters(index = "2", description = "Server.", defaultValue = "http://localhost:5000")
	private String serverLocation;

	private void addSections(JCas jCas) {
		Sentence titleText = JCasUtil.select(jCas, Sentence.class).stream().toArray(Sentence[]::new)[0];
		Section titleSection = new Section(jCas, titleText.getBegin(), titleText.getEnd());
		titleSection.setSectionType("title");
		titleSection.addToIndexes();

		Section abstractSection = new Section(jCas, titleText.getEnd() + 1, jCas.getDocumentText().length());
		abstractSection.setSectionType("abstract");
		abstractSection.addToIndexes();
	}

	private static final String[] sdtTypes = { "method", "result", "fact", "implication", "goal", "problem",
			"hypothesis", "title", "none" };

	private static Pattern p = Pattern.compile("\\|");

	@Override
	public Integer call() throws Exception {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(SentenceAnnotator.getDescription());

		SDTClient sdt = new SDTClient(serverLocation);

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(builder.createAggregateDescription());

		try (BufferedReader b = new BufferedReader(new FileReader(inputFileName));
				BufferedWriter w = new BufferedWriter(new FileWriter(outputFileName))) {
			// Header
			for (String type : sdtTypes) {
				w.write(type);
				w.write(",");
			}
			w.write("focus");
			w.newLine();

			// Annotate and read the annotations
			for (String line; (line = b.readLine()) != null;) {
				String[] tokens = p.split(line);

				if (tokens.length != 3) {
					continue;
				}

				JCas jCas = JCasFactory.createText(tokens[1]);

				ae.process(jCas);
				addSections(jCas);

				List<Sentence> sentences = new ArrayList<>();

				String abstractText = "";

				for (Section section : JCasUtil.select(jCas, Section.class)) {
					if (section.getSectionType().equals("abstract")) {
						for (Sentence sentence : JCasUtil.selectCovered(jCas, Sentence.class, section)) {
							sentences.add(sentence);
							abstractText += sentence.getCoveredText() + "\r\n";
						}
						break;
					}
				}

				List<String> sentencesSDT = (sentences.size() > 0 ? Arrays.asList(sdt.predict(abstractText)) : null);

				Set<String> pathogenSections = new HashSet<>();

				for (Section section : JCasUtil.select(jCas, Section.class)) {
					if (section.getSectionType().equalsIgnoreCase("title")) {
						if (section.getCoveredText().contains("@PATHOGEN$")) {
							pathogenSections.add("title");
						}
					} else if (section.getSectionType().equalsIgnoreCase("abstract")) {
						for (int i = 0; i < sentences.size(); i++) {
							if (sentences.get(i).getCoveredText().contains("@PATHOGEN$")) {
								pathogenSections.add(sentencesSDT.get(i));
							}
						}
					}
				}

				for (String type : sdtTypes) {
					w.write((pathogenSections.contains(type) ? "1" : "0"));
					w.write(",");
				}
				w.write(tokens[2].equals("Y") ? "1" : "0");
				w.newLine();
				w.flush();
			}
		}
		ae.collectionProcessComplete();

		return 0;

	}

	public static void main(String[] argc) throws UIMAException, IOException, URISyntaxException, SAXException {
		int exitCode = new CommandLine(new GenerateDataAnnotated()).execute(argc);
		System.exit(exitCode);
	}
}