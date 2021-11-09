package readbiomed.download;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.annotators.dictionary.pathogens.PathogenNCBIExperimenter;
import readbiomed.pathogens.dataset.utils.Utils;

@Command(name = "PMCDownload", mixinStandardHelpOptions = true, version = "PMCDownload 0.1", description = "PMCDownload")
public class PMCDownload implements Callable<Integer> {

	private static final int maxEFetchPMCIDs = 200;

	public static void recoverPMCArticles(Collection<String> pmcids, String folderName)
			throws IOException, InterruptedException {
		// If folder does not exist, create it
		Path path = Files.createDirectories(Paths.get(folderName + "/PMC"));

		StringBuilder pmcidList = new StringBuilder();

		int fileCount = 0;
		int pmcidCount = 0;
		int recovered = 0;

		System.out.println("Recovering " + pmcids.size() + " PMC articles");
		for (String pmcid : pmcids) {
			if (pmcidCount == maxEFetchPMCIDs) {
				String queryURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pmc&id="
						+ pmcidList.toString().substring(1);

				Utils.urlStreamToFile(queryURL, path.toString() + "/pmcids" + fileCount + ".xml.gz");

				fileCount++;
				pmcidCount = 0;
				pmcidList.setLength(0);
				System.out.println("Recovered " + recovered + " citations.");
			}

			pmcidList.append(",").append(pmcid);
			pmcidCount++;
			recovered++;
		}

		if (pmcidList.length() > 0) {
			String queryURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pmc&id="
					+ pmcidList.toString().substring(1);

			Utils.urlStreamToFile(queryURL, path.toString() + "/pmcids" + fileCount + ".xml.gz");
		}

		System.out.println("Finished recovering.");
	}

	private static List<String> getPMCIDs(String fileName) throws FileNotFoundException, IOException {
		List<String> PMID2PMCID = new ArrayList<>();

		try (BufferedReader b = new BufferedReader(new FileReader(fileName))) {
			for (String line; (line = b.readLine()) != null;) {
				String[] tokens = line.split(",");
				if (tokens.length == 3 && !tokens[1].equals("null")) {
					PMID2PMCID.add(tokens[1]);
				}
			}
		}

		return PMID2PMCID;
	}

	@Parameters(index = "0", description = "pmid2pmc list.", defaultValue = "/Users/ajimeno/Documents/UoM/pmid2pmcid.csv")
	private String pmid2pmcFileName;
	@Parameters(index = "1", description = "Output folder.", defaultValue = "/Users/ajimeno/Documents/UoM")
	private String outputFolderName;

	@Override
	public Integer call() throws Exception {
		List<String> pmcids = getPMCIDs(pmid2pmcFileName);
		System.out.println("Total number of ids: " + pmcids.size());
		recoverPMCArticles(pmcids, outputFolderName);
		
		return 0;
	}

	public static void main(String[] argc) throws FileNotFoundException, IOException, InterruptedException {
		int exitCode = new CommandLine(new PMCDownload()).execute(argc);
		System.exit(exitCode);
	}
}