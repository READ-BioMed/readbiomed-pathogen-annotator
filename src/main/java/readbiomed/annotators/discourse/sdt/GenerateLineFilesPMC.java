package readbiomed.annotators.discourse.sdt;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

public class GenerateLineFilesPMC implements Callable<Integer> {

	private static Pattern p = Pattern.compile("\\|");

	private static String cleanHeader(String string) {
		return string.trim().toLowerCase().replaceAll("^[0-9i]+.? ", "").trim();
	}

	@Parameters(index = "0", description = "Input file name.", defaultValue = "/Users/ajimeno/Documents/UoM/dataset.pmc.pipe.gz")
	private String inputFileName;

	private static Map<String, Integer> getHeaders(String fileName) throws FileNotFoundException, IOException {
		Map<String, Integer> tagCount = new HashMap<>();

		try (BufferedReader b = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))))) {

			for (String line; (line = b.readLine()) != null;) {
				String[] tokens = p.split(line);

				if (tokens[1].length() < 20) {
					String cleanString = cleanHeader(tokens[1]);

					if (tagCount.get(cleanString) == null) {
						tagCount.put(cleanString, 1);
					} else {
						tagCount.put(cleanString, tagCount.get(cleanString) + 1);
					}
				}
			}
		}

		Map<String, Integer> output = new HashMap<>();

		tagCount.entrySet().forEach(e -> {
			if (e.getValue() > 50) {
				output.put(e.getKey(), output.size());
			}
		});

		return output;
	}

	public static void main(String[] argc) throws FileNotFoundException, IOException {
		int exitCode = new CommandLine(new GenerateLineFilesPMC()).execute(argc);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		// Read headers
		Map<String, Integer> headers = getHeaders(inputFileName);

		// Print header
		headers.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
			System.out.print(e.getKey());
			System.out.print(",");
		});
		System.out.println("focus");

		// Collect data
		try (BufferedReader b = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFileName))))) {
			Map<String, String> keyOutput = new HashMap<>();
			Map<String, int[]> keyDiscourse = new HashMap<>();

			for (String line; (line = b.readLine()) != null;) {
				// Check for change in PMID
				String[] tokens = p.split(line);

				if (tokens[1].length() < 20) {
					String discourseName = cleanHeader(tokens[1]);

					if (headers.get(discourseName) != null) {
						String key = tokens[0] + "|" + tokens[2];
						String output = tokens[4];

						keyOutput.put(key, output);

						int[] discourse = keyDiscourse.get(key);

						if (discourse == null) {
							discourse = new int[headers.size()];
							keyDiscourse.put(key, discourse);
						}

						discourse[headers.get(discourseName)] = 1;
					}
				}
			}

			// Print data
			keyOutput.entrySet().stream().forEach(e -> {
				for (int d : keyDiscourse.get(e.getKey())) {
					System.out.print(d);
					System.out.print(",");
				}

				System.out.println(e.getValue().equals("Y") ? 1 : 0);
			});
		}

		return 0;
	}
}
