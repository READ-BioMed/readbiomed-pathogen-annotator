package readbiomed.ingestion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ViewUriUtil;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.document.Section;
import readbiomed.readers.medline.MedlineReader;

@Command(name = "CheckTitlePMC", mixinStandardHelpOptions = true, version = "CheckTitlePMC 0.1", description = "CheckTitlePMC.")
public class CheckTitlePMC implements Callable<Integer> {

	private static final Pattern p = Pattern.compile(",");

	private static Map<String, String> getPMID2PMC(String fileName) throws FileNotFoundException, IOException {
		Map<String, String> map = new HashMap<>();

		try (BufferedReader b = new BufferedReader(new FileReader(fileName))) {
			// Skip header
			b.readLine();
			for (String line; (line = b.readLine()) != null;) {
				String[] tokens = p.split(line);
				if (tokens.length == 2)
					map.put(tokens[1], tokens[0]);
			}
		}

		return map;
	}

	@Parameters(index = "0", description = "pmcid2pmid file name.", defaultValue = "/Users/ajimeno/Documents/UoM/pmcid2pmid.csv")
	private String pmc2pmidFileName;
	@Parameters(index = "1", description = "PubMed folder name.", defaultValue = "/Users/ajimeno/Documents/UoM/pathogens-ncbi/PubMed")
	private String medlineFolderName;

	public static void main(String[] argc) throws UIMAException, CASAdminException, IOException {
		int exitCode = new CommandLine(new CheckTitlePMC()).execute(argc);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		Map<String, String> map = getPMID2PMC(pmc2pmidFileName);

		JCas jCas = JCasFactory.createJCas();

		for (File file : new File(medlineFolderName).listFiles()) {
			if (file.getName().endsWith(".gz")) {
				// System.out.println(file.getName());

				JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
						.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

				while (cr.hasNext()) {
					cr.getNext(jCas);

					String pmid = ViewUriUtil.getURI(jCas).toString();

					boolean hasAbstract = false;
					if (JCasUtil.select(jCas, Section.class).stream().filter(e -> e.getSectionType().equals("abstract"))
							.count() > 0) {
						hasAbstract = true;
					}

					System.out.print(pmid);
					System.out.print(",");
					System.out.print(map.get(pmid));
					System.out.print(",");
					System.out.print((hasAbstract ? "abstract" : "noabstract"));
					System.out.println();

					jCas.reset();
				}
			}
		}
		return 0;
	}
}