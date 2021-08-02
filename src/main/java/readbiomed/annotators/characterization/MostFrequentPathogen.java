package readbiomed.annotators.characterization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * 
 * Perform classification based on the most frequent pathogen
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
@Command(name = "MostFrequentPathogen", mixinStandardHelpOptions = true, version = "MostFrequentPathogen 0.1", description = "Evaluation of focus entity based on the most frequent entity.")
public class MostFrequentPathogen implements Callable<Integer> {

	@Parameters(index = "0", description = "Input file name.")
	private String inputFileName;

	private static Pattern p = Pattern.compile("\\|");

	private int countPathogen(String text) {
		return StringUtils.countMatches(text, "@PATHOGEN$");
	}

	@Override
	public Integer call() throws Exception {

		try (BufferedReader b = new BufferedReader(new FileReader(inputFileName))) {
			// Remove first line - header
			if (b.readLine() != null) {
				String pmid = null;
				int doc_count = 0;
				int tp = 0;
				int positives = 0;
				int fp = 0;
				int max_count = 0;
				String max_class = "";

				for (String line; (line = b.readLine()) != null;) {
					// PMID|Test|Category
					String[] tokens = p.split(line);

					// @PATHOGEN$ mentions count
					int count = countPathogen(tokens[1]);

					if (tokens[2].equals("Y")) {
						positives++;
					}

					if (tokens.length == 3) {
						if (pmid == null) {
							pmid = tokens[0];
							doc_count++;
							max_count = count;
							max_class = tokens[2];
						} else {
							if (pmid.equals(tokens[0])) {
								if (max_count < count) {
									max_count = count;
									max_class = tokens[2];
								}
							} else {
								if (max_class.equals("Y")) {
									tp++;
								} else {
									fp++;
								}

								doc_count++;

								pmid = tokens[0];
								max_count = count;
								max_class = tokens[2];
							}
						}
					}
				}

				if (pmid != null) {
					if (max_class.equals("Y")) {
						tp++;
					} else {
						fp++;
					}
				}

				double precision = (double)tp / (tp + fp);
				double recall = (double)tp / positives;
				double f1 = 2 * precision * recall / (precision + recall);

				System.out.println("TP: " + tp);
				System.out.println("FP: " + fp);
				System.out.println("Positives: " + positives);
				
				System.out.println("Precision: " + precision);
				System.out.println("Recall: " + recall);
				System.out.println("F1: " + f1);
				
				System.out.println("Documents: " + doc_count);
			}
		}

		return 0;
	}

	public static void main(String[] argc) throws IOException, SAXException, UIMAException, JAXBException {
		int exitCode = new CommandLine(new MostFrequentPathogen()).execute(argc);
		System.exit(exitCode);
	}
}