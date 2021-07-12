package readbiomed.annotators.ml.mtiml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.token.type.Sentence;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.annotators.discourse.sdt.SDTClient;

@Command(name = "MTIMLWithSDT", mixinStandardHelpOptions = true, version = "MTIMLWithSDT 0.1", description = "Annotate text with SDT to be used with MTIML.")
public class MTIMLWithSDT implements Callable <Integer> {

	private static String[] discourse = { "method", "implication", "none", "result", "hypothesis", "problem", "result",
			"fact", "goal" };

	@Parameters(index = "0", description = "Input file name.")
	private String inputFileName;
	@Parameters(index = "1", description = "Output file name.")
	private String outputFileName;
	
	
	@Override
	public Integer call() throws Exception {
		try (BufferedReader b = new BufferedReader(new FileReader(inputFileName));
				BufferedWriter w = new BufferedWriter(new FileWriter(outputFileName))) {
			String line;

			AnalysisEngine ae = AnalysisEngineFactory.createEngine(SentenceAnnotator.getDescription());

			SDTClient sdt = new SDTClient("http://localhost:5000");

			// Header line
			b.readLine();

			StringBuilder header = new StringBuilder();
			for (String d : discourse) {
				header.append(d).append("|");
			}
			header.append("category");
			System.out.println(header);
			w.write(header.toString());
			w.newLine();

			JCas jCas = JCasFactory.createJCas();

			while ((line = b.readLine()) != null) {
				String[] tokens = line.split("\\|");

				if (tokens.length == 2) {
					jCas.setDocumentText(tokens[0]);

					ae.process(jCas);

					String string = "";

					for (Sentence s : JCasUtil.select(jCas, Sentence.class)) {
						string += s.getCoveredText() + "\n";
					}

					String[] prediction = sdt.predict(string);

					Map<String, String> map = new HashMap<String, String>();

					int i = 0;
					for (Sentence s : JCasUtil.select(jCas, Sentence.class)) {
						String sentence = map.get(prediction[i]);

						if (sentence == null) {
							sentence = "";
						}

						map.put(prediction[i], sentence + " " + s.getCoveredText());

						i++;
					}

					StringBuilder output = new StringBuilder();

					for (String d : discourse) {
						output.append((map.get(d) == null ? "" : map.get(d).trim())).append("|");
					}

					output.append(tokens[1]);

					System.out.println(output);
					w.write(output.toString());
					w.newLine();

					jCas.reset();
				}
			}
		}
		
		return 0;
	}
	
	public static void main(String[] argc) throws FileNotFoundException, IOException, UIMAException {
		int exitCode = new CommandLine(new MTIMLWithSDT()).execute(argc);
		System.exit(exitCode);
	}
}