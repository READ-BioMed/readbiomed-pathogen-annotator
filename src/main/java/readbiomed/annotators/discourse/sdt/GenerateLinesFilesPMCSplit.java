package readbiomed.annotators.discourse.sdt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "GenerateLinesFilesPMCSplit", mixinStandardHelpOptions = true, version = "GenerateLinesFilesPMCSplit 0.1", description = "SDT line train/test generation.")
public class GenerateLinesFilesPMCSplit implements Callable<Integer> {

	private static Pattern p = Pattern.compile("\\|");

	public static void main(String[] argc) throws FileNotFoundException, IOException {
		int exitCode = new CommandLine(new GenerateLinesFilesPMCSplit()).execute(argc);
		System.exit(exitCode);
	}

	@Parameters(index = "0", description = "Input file name.", defaultValue = "/Users/ajimeno/Documents/UoM/sdt.pmc.csv")
	private String inputFileName;
	@Parameters(index = "1", description = "Output file name Train.", defaultValue = "/Users/ajimeno/Documents/UoM/sdt.pmc.clean.train.csv")
	private String outputFileNameTrain;
	@Parameters(index = "1", description = "Output file name Test.", defaultValue = "/Users/ajimeno/Documents/UoM/sdt.pmc.clean.test.csv")
	private String outputFileNameTest;

	@Override
	public Integer call() throws Exception {
		// Read PMIDs
		Set<String> PMID = new HashSet<String>();

		String header = "";

		try (BufferedReader b = new BufferedReader(new FileReader(inputFileName))) {
			boolean first = true;

			for (String line; (line = b.readLine()) != null;) {
				if (line.trim().length() > 0) {
					if (first) {
						first = false;
						header = line;
					} else {
						String[] tokens = p.split(line);
						PMID.add(tokens[0]);
					}
				}
			}
		}

		LinkedList<String> list = new LinkedList<>(PMID);
		Collections.shuffle(list);

		Set<String> testPMID = new HashSet<>();

		int count = 0;
		for (String s : list) {
			if (count % 3 == 0) {
				testPMID.add(s);
			}

			count++;
		}

		try (BufferedReader b = new BufferedReader(new FileReader(inputFileName));
				BufferedWriter wtrain = new BufferedWriter(new FileWriter(outputFileNameTrain));
				BufferedWriter wtest = new BufferedWriter(new FileWriter(outputFileNameTest));) {
			wtrain.write(header);
			wtrain.newLine();
			wtest.write(header);
			wtest.newLine();

			boolean first = true;

			for (String line; (line = b.readLine()) != null;) {
				if (first) {
					first = false;
				} else {
					if (line.trim().length() > 0) {
						String[] tokens = p.split(line);

						if (testPMID.contains(tokens[0])) {
							wtest.write(line);
							wtest.newLine();
						} else {
							wtrain.write(line);
							wtrain.newLine();
						}
					}
				}
			}
		}
		return null;
	}
}