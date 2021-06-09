package readbiomed.annotators.dictionary.pathogens.PrPSc;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.jar.Train;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.util.ViewUriUtil;

import readbiomed.annotators.dictionary.utils.ConceptMapperFactory;
import readbiomed.annotators.dictionary.utils.Serialization;
import readbiomed.bmip.dataset.PrPSc.PrPScBuildDataset;
import readbiomed.readers.medline.MedlineReader;

public class PrPScExperimenter {
	public static void train(String dictURI, String folderName, Map<String, Set<String>> gt) throws Exception {

		String modelFolderName = folderName + "/model";

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(ConceptMapperFactory.create(dictURI));
		builder.add(SentenceAnnotator.getDescription());
		builder.add(PrPScMLAnnotator.getWriterDescription(modelFolderName, Serialization.serialize((Serializable) gt)));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(builder.createAggregateDescription());

		JCas jCas = JCasFactory.createJCas();

		for (File file : new File(folderName).listFiles()) {
			if (file.getName().endsWith(".xml.gz")) {
				System.out.println(file.getName());

				JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
						.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

				while (cr.hasNext()) {
					cr.getNext(jCas);
					ae.process(jCas);

					jCas.reset();
				}
			}
		}

		ae.collectionProcessComplete();

		Train.main(modelFolderName, "-s", "0", "-t", "0", "-c", "10000");
	}

	public static Map<String, Set<String>> annotate(String dictURI, String folderName) throws Exception {
		Map<String, Set<String>> prediction = new HashMap<>();

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(ConceptMapperFactory.create(dictURI));
		builder.add(SentenceAnnotator.getDescription());
		builder.add(PrPScMLAnnotator.getClassifierDescription(folderName + "/model/model.jar"));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(builder.createAggregateDescription());

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

					JCasUtil.select(jCas, NamedEntityMention.class).forEach(e -> prediction
							.computeIfAbsent(e.getMentionId().toLowerCase(), o -> new HashSet<String>()).add(pmid));

					jCas.reset();
				}
			}
		}

		ae.collectionProcessComplete();

		return prediction;
	}

	public static void main(String[] argc) throws Exception {
		String dictFileName = "file:/home/antonio/Documents/UoM/prpsc-dict.xml";
		String folderName = "/home/antonio/Documents/UoM/prpsc-ncbi-data/PubMed";

		String[] trainingCandidates = { "prpsc-cattle", "prpsc-cat", "prpsc-deer", "prpsc-goat" };
		Set<String> trainingSet = new HashSet<>(Arrays.asList(trainingCandidates));

		// Read GT
		Map<String, Set<String>> gt = PrPScBuildDataset.readPrPScEntries("/home/antonio/Documents/UoM/prpsc-ncbi-data");

		// Train
		train(dictFileName, folderName, gt.entrySet().stream().filter(e -> trainingSet.contains(e.getKey()))
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

		// Annotate
		Map<String, Set<String>> predictions = annotate(dictFileName, folderName);

		predictions.entrySet().stream().forEach(e -> System.out.println(e.getKey() + " " + e.getValue().size()));

		// Compare GT
		for (Map.Entry<String, Set<String>> entry : gt.entrySet()) {
			// System.out.println(entry.getKey() + "|" + predictions.get(entry.getKey()));
			if (entry.getValue().size() > 0) {
				Set<String> tp = entry.getValue().stream()
						.filter(predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>())::contains)
						.collect(Collectors.toSet());

				Set<String> fp = predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>()).stream()
						.filter(e -> !entry.getValue().contains(e)).collect(Collectors.toSet());

				Set<String> fn = entry.getValue().stream()
						.filter(e -> !predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>()).contains(e))
						.collect(Collectors.toSet());

				System.out.println(entry.getKey() + "|" + tp.size() + "|" + entry.getValue().size() + "|"
						+ predictions.get(entry.getKey()).size());

				double recall = tp.size() / (double) (tp.size() + fn.size());
				double precision = tp.size() / (double) (tp.size() + fp.size());
				double f1 = (2 * precision * recall) / (precision + recall);

				System.out.println(entry.getKey() + "|" + precision + "|" + recall + "|" + f1);

				System.out.println("TP:" + tp);
				System.out.println("FP:" + fp);
				System.out.println("FN:" + fn);
			}
		}
	}
}