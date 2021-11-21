package readbiomed.annotators.discourse.sdt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.admin.CASAdminException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "GenerateTextPMC", mixinStandardHelpOptions = true, version = "GenerateTextPMC 0.1", description = "SDT line generation.")
public class GenerateTextPMC implements Callable<Integer> {

	@Parameters(index = "0", description = "Input file name.", defaultValue = "/Users/ajimeno/Documents/UoM/dataset.pmc.pipe.gz")
	private String inputFileName;
	@Parameters(index = "1", description = "Output file name.", defaultValue = "/Users/ajimeno/Documents/UoM/sdt/PMC/training.all.pipe")
	private String outputFileName;
	@Parameters(index = "2", description = "Key file name.", defaultValue = "/Users/ajimeno/Documents/UoM/sdt/PMC/sdt.pmc.clean.train.csv")
	private String keyFileName;

	@Override
	public Integer call() throws Exception {
		// Read the keys
		Map<String, String> keys = new HashMap<>();

		Map<String, Integer> tagSet = new HashMap<>();

		Pattern pComma = Pattern.compile(",");
		Pattern pPipe = Pattern.compile("\\|");

		try (BufferedReader b = new BufferedReader(new FileReader(keyFileName))) {
			boolean first = true;

			for (String line; (line = b.readLine()) != null;) {
				if (!first) {
					String[] tokens = pComma.split(line);
					keys.put(tokens[0], tokens[tokens.length - 1]);
				} else {
					String[] tokens = pComma.split(line);

					for (int i = 1; i < tokens.length - 1; i++) {
						tagSet.put(tokens[i], i - 1);
					}

					first = false;
				}
			}
		}

		System.out.println(keys.size());

		try (BufferedWriter w = new BufferedWriter(new FileWriter(outputFileName))) {

			w.write("Text|Category");
			w.newLine();

			Map<String, StringBuilder> keyStrings = new HashMap<>();

			// Read the file with the text
			try (BufferedReader b = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFileName))))) {

				for (String line; (line = b.readLine()) != null;) {
					String[] tokens = pPipe.split(line);

					String key = tokens[0] + "|" + tokens[2];

					if (keys.get(key) != null) {
						if (tokens[1].length() < 20) {
							String discourseName = GenerateLineFilesPMC.cleanHeader(tokens[1]);

							if (tagSet.get(discourseName) != null) {
								StringBuilder string = keyStrings.get(key);

								if (string == null) {
									string = new StringBuilder();
									keyStrings.put(key, string);
								}

								string.append(" @").append(tokens[1]).append(": ").append(tokens[3]);
							}
						}
					}
				}
			}

			// Print output
			keyStrings.entrySet().stream().forEach(e -> {
				try {
					w.write(e.getValue().toString().trim());
					w.write("|");
					w.write(keys.get(e.getKey()));
					w.newLine();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			});
		}

		return 0;
	}

	public static void main(String[] argc) throws UIMAException, FileNotFoundException, CASAdminException, IOException {
		int exitCode = new CommandLine(new GenerateTextPMC()).execute(argc);
		System.exit(exitCode);
	}
}