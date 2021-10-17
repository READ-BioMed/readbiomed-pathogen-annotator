package readbiomed.annotators.dictionary.pathogens.toxins;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.pathogens.dataset.toxins.ToxinBuildDataset;
import readbiomed.readers.medline.MedlineReader;

@Command(name = "ToxinExperimenter", mixinStandardHelpOptions = true, version = "ToxinExperimenter 0.1", description = "Toxin experimenter.")
public class ToxinExperimenter implements Callable<Integer> {

	public static Map<String, Set<String>> annotateNCBISet(String dictURI, String folderName) throws Exception {
		Map<String, Set<String>> prediction = new HashMap<>();

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(ToxinRegexAnnotator.class);

		JCas jCas = JCasFactory.createJCas();

		for (File file : new File(folderName).listFiles()) {
			if (file.getName().endsWith(".xml.gz")) {
				System.out.println(file.getName());

				JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
						.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

				while (cr.hasNext()) {
					cr.getNext(jCas);
					ae.process(jCas);

					String pmid = ViewUriUtil.getURI(jCas).toString();

					JCasUtil.select(jCas, NamedEntityMention.class).forEach(
							e -> prediction.computeIfAbsent(e.getMentionId(), o -> new HashSet<String>()).add(pmid));

					jCas.reset();
				}
			}
		}

		ae.collectionProcessComplete();

		return prediction;
	}

	@Parameters(index = "0", description = "Toxin dictionary file name.", defaultValue = "file:/home/antonio/Documents/UoM/toxin-dict.xml")
	private String dictFileName;
	@Parameters(index = "1", description = "Output folder name.", defaultValue = "/home/antonio/Documents/UoM/toxin-ncbi-data/PubMed")
	private String outputFolderName;
	@Parameters(index = "2", description = "NCBI pathogen folder name.", defaultValue = "/home/antonio/Documents/UoM/toxin-ncbi-data")
	private String toxinNCBIDataFolderName;

	@Override
	public Integer call() throws Exception {
		Map<String, Set<String>> predictions = annotateNCBISet(dictFileName, outputFolderName);

		predictions.entrySet().stream().forEach(e -> System.out.println(e.getKey() + " " + e.getValue().size()));

		Map<String, Set<String>> gt = ToxinBuildDataset.readToxinEntries(toxinNCBIDataFolderName);

		// Compare GT
		for (Map.Entry<String, Set<String>> entry : gt.entrySet()) {
			// System.out.println(entry.getKey() + "|" + predictions.get(entry.getKey()));
			if (entry.getValue().size() > 0) {
				long common = entry.getValue().stream()
						.filter(predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>())::contains).count();

				Set<String> fp = predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>()).stream()
						.filter(e -> !entry.getValue().contains(e)).collect(Collectors.toSet());

				Set<String> fn = entry.getValue().stream()
						.filter(e -> !predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>()).contains(e))
						.collect(Collectors.toSet());

				System.out.println(entry.getKey() + "|" + common + "|" + entry.getValue().size() + "|"
						+ predictions.get(entry.getKey()).size());

				double recall = common / (double) (common + fn.size());
				double precision = common / (double) (common + fp.size());
				double f1 = (2 * precision * recall) / (precision + recall);

				System.out.println(entry.getKey() + "|" + precision + "|" + recall + "|" + f1);

				System.out.println("FP:" + fp);
				System.out.println("FN:" + fn);
			}
		}
		return 0;
	}

	public static void main(String[] argc) throws Exception {
		int exitCode = new CommandLine(new ToxinExperimenter()).execute(argc);
		System.exit(exitCode);
	}
}