package readbiomed.annotators.dictionary.pathogens;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.conceptMapper.DictTerm;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ViewUriUtil;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.annotators.dictionary.utils.ConceptMapperFactory;
import readbiomed.pathogens.dataset.NCBITaxonomy.BuildDataset;
import readbiomed.readers.pmc.PMCReader;

@Command(name = "PathogenPMCNCBIExperimenter", mixinStandardHelpOptions = true, version = "PathogenPMCNCBIExperimenter 0.1", description = "Pathogen PMC experimenter.")
public class PathogenPMCNCBIExperimenter implements Callable<Integer> {
	@Parameters(index = "0", description = "Dictionary file name.", defaultValue = "file:/Users/ajimeno/Documents/UoM/dictionaries/dict.xml")
	private String dictFileName;
	@Parameters(index = "1", description = "Data folder name.", defaultValue = "/Users/ajimeno/Documents/UoM/PMC")
	private String dataFolderName;
	@Parameters(index = "2", description = "NCBI pathogen folder name.", defaultValue = "/Users/ajimeno/Documents/git/readbiomed-pathogens-dataset/ncbi/ncbi-pathogens-data")
	private String NCBIDataFolderName;

	@Override
	public Integer call() throws Exception {
		Map<String, Set<String>> gt = BuildDataset.readPathogenEntries(NCBIDataFolderName);

		Map<String, Set<String>> predictions = new HashMap<>();

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(ConceptMapperFactory.create(dictFileName));

		JCas jCas = JCasFactory.createJCas();

		// We only have the PMIDs of PMC articles
		Set<String> pmids = new HashSet<>();

		for (File file : new File(dataFolderName).listFiles()) {
			if (file.getName().endsWith(".gz")) {
				System.out.println(file.getName());

				JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
						.createReader(PMCReader.getDescriptionFromFiles(file.getAbsolutePath()));

				while (cr.hasNext()) {
					cr.getNext(jCas);
					ae.process(jCas);

					String pmid = ViewUriUtil.getURI(jCas).toString();

					JCasUtil.select(jCas, DictTerm.class).forEach(e -> predictions
							.computeIfAbsent(e.getDictCanon().toLowerCase(), o -> new HashSet<String>()).add(pmid));

					pmids.add(pmid);

					jCas.reset();
				}
			}
		}

		Map<String, Set<String>> newgt = new HashMap<>();

		// Clean the data set to keep only the available full text articles
		for (Map.Entry<String, Set<String>> entry : gt.entrySet()) {
			Set<String> newPMIDs = new HashSet<>(entry.getValue());
			newPMIDs.retainAll(pmids);

			if (newPMIDs.size() > 0) {
				newgt.put(entry.getKey(), newPMIDs);
			}
		}

		int total_common = 0;
		int total_fp = 0;
		int total_fn = 0;

		// Compare GT
		for (Map.Entry<String, Set<String>> entry : newgt.entrySet()) {
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

				total_common += common;
				total_fp += fp.size();
				total_fn += fn.size();
			}
		}
		
		System.out.println("Total: " + total_common + "|FN:" + total_fn + "|FP:" + total_fp);

		double recall = total_common / (double) (total_common + total_fn);
		double precision = total_common / (double) (total_common + total_fp);
		double f1 = (2 * precision * recall) / (precision + recall);

		System.out.println(precision + "|" + recall + "|" + f1);

		return 0;
	}

	public static void main(String[] argc) throws FileNotFoundException, IOException, InterruptedException {
		int exitCode = new CommandLine(new PathogenPMCNCBIExperimenter()).execute(argc);
		System.exit(exitCode);
	}
}