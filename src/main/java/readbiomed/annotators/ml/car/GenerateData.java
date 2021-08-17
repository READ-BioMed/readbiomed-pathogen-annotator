package readbiomed.annotators.ml.car;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.token.type.Sentence;
import org.cleartk.util.ViewUriUtil;
import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.annotators.dictionary.pathogens.PathogenDictionaryAnnotator;
import readbiomed.annotators.dictionary.pathogens.PathogenExperimenter;
import readbiomed.annotators.dictionary.utils.TextFileFilter;
import readbiomed.annotators.discourse.sdt.SDTClient;
import readbiomed.document.Section;

@Command(name = "GenerateData", mixinStandardHelpOptions = true, version = "GenerateData 0.1", description = "Generate data for ARC experiments.")
public class GenerateData implements Callable<Integer> {
	@Parameters(index = "0", description = "Dictionary file name.", defaultValue = "file:/Users/ajimeno/Documents/UoM/dictionaries/dict.xml")
	private String dictFileName;

	@Parameters(index = "1", description = "Ground truth file name.", defaultValue = "/Users/ajimeno/Documents/git/readbiomed-bmip-datasets/manual-set/ground-truth/manual-annotation-gt.csv")
	private String gtFileName;

	@Parameters(index = "2", description = "Text folder name.", defaultValue = "/Users/ajimeno/Documents/git/readbiomed-bmip-datasets/manual-set/articles-txt-format")
	private String textFolderName;

	@Parameters(index = "3", description = "Output name.", defaultValue = "/Users/ajimeno/Documents/UoM/sdt-arc-test.csv")
	private String outputFileName;

	private void addSections(JCas jCas) {
		String titleText = jCas.getDocumentText().lines().toArray(String[]::new)[1];
		Section titleSection = new Section(jCas, 0, titleText.length());
		titleSection.setSectionType("title");
		titleSection.addToIndexes();

		Section abstractSection = new Section(jCas, titleText.length() + 1, jCas.getDocumentText().length());
		abstractSection.setSectionType("abstract");
		abstractSection.addToIndexes();
	}

	private static final String[] sdtTypes = { "method", "result", "fact", "implication", "goal", "problem",
			"hypothesis", "title", "none" };

	@Override
	public Integer call() throws Exception {
		Map<String, Set<String>> gt = PathogenExperimenter.getGT(gtFileName, textFolderName);

		// Annotate documents and generate CSV file
		AggregateBuilder builder = PathogenDictionaryAnnotator.getPipeline(dictFileName);

		// AggregateBuilder builder = new AggregateBuilder();
		builder.add(SentenceAnnotator.getDescription());

		SDTClient sdt = new SDTClient("http://localhost:5000");

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(builder.createAggregateDescription());

		try (BufferedWriter w = new BufferedWriter(new FileWriter(outputFileName))) {
			// Header
			for (String type : sdtTypes) {
				w.write(type);
				w.write(",");
			}
			w.write("focus");
			w.newLine();

			// Annotate and read the annotations
			for (File file : FileUtils.listFiles(new File(textFolderName), new TextFileFilter(), null)) {
				String fileName = file.getName().replaceAll(".txt$", "");

				System.out.println(fileName);
				if (gt.get(fileName).size() == 0) {
					System.out.println("No pathogens in ground truth");
					continue;
				}

				JCas jCas = JCasFactory.createText(Files.readString(file.toPath()));
				ViewUriUtil.setURI(jCas, new URI(file.getName()));

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

				Map<String, Set<String>> pathogenSections = new HashMap<>();

				for (Section section : JCasUtil.select(jCas, Section.class)) {
					if (section.getSectionType().equalsIgnoreCase("title")) {
						for (NamedEntityMention nem : JCasUtil.selectCovered(jCas, NamedEntityMention.class, section)) {
							Set<String> types = pathogenSections.get(nem.getMentionId());
							if (types == null) {
								types = new HashSet<String>();
								pathogenSections.put(nem.getMentionId(), types);
							}
							types.add("title");

						}
					} else if (section.getSectionType().equalsIgnoreCase("abstract")) {
						for (int i = 0; i < sentences.size(); i++) {
							for (NamedEntityMention nem : JCasUtil.selectCovered(jCas, NamedEntityMention.class,
									sentences.get(i))) {
								Set<String> types = pathogenSections.get(nem.getMentionId());
								if (types == null) {
									types = new HashSet<String>();
									pathogenSections.put(nem.getMentionId(), types);
								}
								types.add(sentencesSDT.get(i));

							}
						}
					}
				}

				for (Map.Entry<String, Set<String>> entry : pathogenSections.entrySet()) {
					for (String type : sdtTypes) {
						w.write((entry.getValue().contains(type) ? "1" : "0"));
						w.write(",");
					}
					w.write((gt.get(fileName).contains(entry.getKey()) ? "1" : "0"));
					w.newLine();
					w.flush();
				}

				// For each pathogen in document
				// for (int i = 0; i < sentences.size();i++)
				// {
				// System.out.println(sentencesSDT.get(i) + ": "+
				// sentences.get(i).getCoveredText());
				// }

				// SDT over abstract

				// prediction.put(fileName,
				// JCasUtil.select(jCas, NamedEntityMention.class).stream()
				// .filter(e -> e.getMentionType().equals("pathogen")).map(e ->
				// e.getMentionId())
				// .collect(Collectors.toSet()));
			}
		}
		ae.collectionProcessComplete();

		return 0;

	}

	public static void main(String[] argc) throws UIMAException, IOException, URISyntaxException, SAXException {
		int exitCode = new CommandLine(new GenerateData()).execute(argc);
		System.exit(exitCode);
	}
}