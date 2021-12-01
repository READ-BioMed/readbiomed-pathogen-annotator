package readbiomed.annotators.discourse.sdt;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "MostFrequentBaselinePMC", mixinStandardHelpOptions = true, version = "MostFrequentBaselinePMC 0.1", description = "MostFrequentBaselinePMC.")
public class MostFrequentBaselinePMC implements Callable<Integer> {

	private static Pattern p = Pattern.compile("\\|");

	public static void main(String[] argc) throws FileNotFoundException, IOException {
		int exitCode = new CommandLine(new MostFrequentBaselinePMC()).execute(argc);
		System.exit(exitCode);
	}

	@Parameters(index = "0", description = "Input file name.", defaultValue = "/Users/ajimeno/Documents/UoM/dataset.pmc.pipe.gz")
	private String inputFileName;

	@Override
	public Integer call() throws Exception {
		Map<String, String> gt = new HashMap<>();

		Map<String, Map<String, Integer>> countPMIDPath = new HashMap<>();

		Set<String> predictions = new HashSet<>();

		try (BufferedReader b = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFileName))))) {
			for (String line; (line = b.readLine()) != null;) {
				String[] tokens = p.split(line);

				if (tokens.length == 5) {
					gt.put(tokens[0] + "|" + tokens[2], tokens[4]);

					int occurrences = tokens[3].split("@PATHOGEN$").length - 1;

					Map<String, Integer> counts = countPMIDPath.get(tokens[0]);

					if (counts == null) {
						counts = new HashMap<>();
						countPMIDPath.put(tokens[0], counts);
					}

					if (counts.get(tokens[2]) == null) {
						counts.put(tokens[2], occurrences);
					} else {
						counts.put(tokens[2], counts.get(tokens[2]) + occurrences);
					}
				}
			}
		}

		countPMIDPath.entrySet().forEach(e -> {
			String pathogen = null;
			int max = -1;

			for (Map.Entry<String, Integer> entry : e.getValue().entrySet()) {
				if (max == -1) {
					pathogen = entry.getKey();
					max = entry.getValue();
				}
			}

			String prediction = e.getKey() + "|" + pathogen;
			predictions.add(prediction);
			System.out.println(prediction + "|" + gt.get(prediction));
		});

		long positivesCount = gt.entrySet().stream().filter(e -> e.getValue().equals("Y")).count();
		long truePositivesCount = predictions.stream().filter(e -> gt.get(e).equals("Y")).count();
		long predictionsCount = predictions.size();

		System.out.println("Positives: " + positivesCount);
		System.out.println("True Positives: " + truePositivesCount);
		System.out.println("Predictions: " + predictionsCount);

		double precision = truePositivesCount / (double) predictionsCount;
		double recall = truePositivesCount / (double) positivesCount;
		double f1 = 2 * precision * recall / (precision + recall);

		System.out.println("Precision: " + precision);
		System.out.println("Recall: " + recall);
		System.out.println("F1: " + f1);

		return 0;
	}
}