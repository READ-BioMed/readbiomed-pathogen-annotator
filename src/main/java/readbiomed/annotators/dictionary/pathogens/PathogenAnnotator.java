package readbiomed.annotators.dictionary.pathogens;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.conceptMapper.DictTerm;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.Train;
import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
import org.cleartk.util.ViewUriUtil;

import com.ibm.au.research.nlp.ingestion.uima.reader.MedlineReader;

import readbiomed.annotators.dictionary.utils.CharacterizationEvaluation;
import readbiomed.annotators.dictionary.utils.ConceptMapperFactory;
import readbiomed.annotators.dictionary.utils.TextFileFilter;
import readbiomed.bmip.dataset.BuildDataset;
import readbiomed.bmip.dataset.DocumentEntry;
import uima.tt.TokenAnnotation;

public class PathogenAnnotator extends CleartkAnnotator<String> {
	public static final String PARAM_GROUND_TRUTH = "gt";

	private Map<String, Set<String>> gt = null;

	private static String serialize(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	private static Object deserialize(String s) throws IOException, ClassNotFoundException {
		byte[] data = Base64.getDecoder().decode(s);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		Object o = ois.readObject();
		ois.close();
		return o;
	}

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		try {
			if (context.getConfigParameterValue(PARAM_GROUND_TRUTH) != null) {
				gt = (Map<String, Set<String>>) deserialize(
						(String) context.getConfigParameterValue(PARAM_GROUND_TRUTH));
			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	private List<Feature> getFrequency(Integer frequency) {
		return Collections.singletonList(new Feature("frequency", frequency));
	}

	private List<Feature> getMax(String id, Map<String, Integer> ids) {
		int frequency = ids.get(id);

		for (Map.Entry<String, Integer> e : ids.entrySet()) {
			if (!e.getKey().equals(id) && e.getValue() > frequency) {
				return Collections.singletonList(new Feature("maxFrequency", 0));
			}
		}
		return Collections.singletonList(new Feature("maxFrequency", 1));
	}

	private List<Feature> getTokens(JCas jCas) {
		return JCasUtil.select(jCas, TokenAnnotation.class).stream()
				.map((TokenAnnotation token) -> new Feature("tokenBetweenEntities", token.getCoveredText()))
				.collect(Collectors.toList());
	}

	private static String getId(String string) {
		return string.replace("http://purl.obolibrary.org/obo/NCBITaxon_", "").replaceAll("D33", "D148")
				.replaceAll("D98", "D148");
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		// Collect the annotated pathogens and their frequency
		Map<String, Integer> ids = new HashMap<>();

		JCasUtil.select(jCas, DictTerm.class).stream()
				.forEach(e -> ids.merge(getId(e.getDictCanon()), 1, Integer::sum));

		for (Entry<String, Integer> id : ids.entrySet()) {
			List<Feature> features = new ArrayList<Feature>();

			// features.addAll(getFrequency(id.getValue()));
			// features.addAll(getTokens(jCas));
			features.addAll(getMax(id.getKey(), ids));

			if (isTraining()) {
				String docId = ViewUriUtil.getURI(jCas).toString().replaceAll(".txt$", "");
				String category = (gt.get(docId) != null && gt.get(docId).contains(id.getKey()) ? "1" : "-1");
				// System.out.println(ViewUriUtil.getURI(jCas).toString().replaceAll(".txt$",
				// "") + "/" + id.toString()
				// + "/" + category);
				this.dataWriter.write(new Instance<String>(category, features));
			} else {
				// If background, remove it from the list of elements
				if (this.classifier.classify(features).equals("-1")) {
					List<DictTerm> removeDict = new ArrayList<DictTerm>();

					for (DictTerm e : JCasUtil.select(jCas, DictTerm.class)) {
						if (getId(e.getDictCanon()).equals(id.getKey())) {
							removeDict.add(e);
						}
					}

					removeDict.stream().forEach(e -> e.removeFromIndexes());
				}
			}
		}
	}

	public static AnalysisEngineDescription getClassifierDescription(String modelFileName)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(PathogenAnnotator.class,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelFileName);
	}

	public static AnalysisEngineDescription getWriterDescription(String outputDirectory, String gt)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(PathogenAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING, true, DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory, DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				LibSvmStringOutcomeDataWriter.class, PARAM_GROUND_TRUTH, gt);
	}

	public static void train(Map<String, Set<String>> gt, String dictURI, String modelFolderName) throws Exception {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(ConceptMapperFactory.create(dictURI));
		builder.add(PathogenAnnotator.getWriterDescription(modelFolderName, serialize((Serializable) gt)));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(builder.createAggregateDescription());

		// Annotate and read the annotations
		for (File file : FileUtils.listFiles(new File(
				"/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/corpora/bmip-pubmed-corpus/articles-txt-format"),
				new TextFileFilter(), null)) {
			String fileName = file.getName().replaceAll(".txt$", "");
			if (gt.containsKey(fileName)) {
				JCas jCas = JCasFactory.createText(Files.readString(file.toPath()));
				ViewUriUtil.setURI(jCas, new URI(file.getName()));

				ae.process(jCas);
			}
		}

		ae.collectionProcessComplete();

		Train.main(modelFolderName, "-s", "0", "-t", "1", "-c", "1.0");
	}

	public static Map<String, Set<String>> test(Map<String, Set<String>> gt, String dictURI, String modelFileName)
			throws Exception {
		Map<String, Set<String>> prediction = new HashMap<>();

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(ConceptMapperFactory.create(dictURI));
		builder.add(PathogenAnnotator.getClassifierDescription(modelFileName));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(builder.createAggregateDescription());

		// Annotate and read the annotations
		for (File file : FileUtils.listFiles(new File(
				"/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/corpora/bmip-pubmed-corpus/articles-txt-format"),
				new TextFileFilter(), null)) {
			String fileName = file.getName().replaceAll(".txt$", "");

			if (gt.containsKey(fileName)) {
				JCas jCas = JCasFactory.createText(Files.readString(file.toPath()));
				ViewUriUtil.setURI(jCas, new URI(file.getName()));

				ae.process(jCas);

				Set<String> ids = new HashSet<>();
				prediction.put(fileName, ids);

				for (DictTerm e : JCasUtil.select(jCas, DictTerm.class)) {
					ids.add(getId(e.getDictCanon()));
				}
			}
		}

		ae.collectionProcessComplete();

		return prediction;
	}

	public static Map<String, Set<String>> annotate(Map<String, Set<String>> gt, String dictURI) throws Exception {
		Map<String, Set<String>> prediction = new HashMap<>();

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(ConceptMapperFactory.create(dictURI));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(builder.createAggregateDescription());

		// Annotate and read the annotations
		for (File file : FileUtils.listFiles(new File(
				"/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/corpora/bmip-pubmed-corpus/articles-txt-format"),
				new TextFileFilter(), null)) {
			String fileName = file.getName().replaceAll(".txt$", "");

			if (gt.containsKey(fileName)) {
				JCas jCas = JCasFactory.createText(Files.readString(file.toPath()));
				ViewUriUtil.setURI(jCas, new URI(file.getName()));

				ae.process(jCas);

				Set<String> ids = new HashSet<>();
				prediction.put(fileName, ids);

				for (DictTerm e : JCasUtil.select(jCas, DictTerm.class)) {
					ids.add(getId(e.getDictCanon()));
				}
			}
		}

		ae.collectionProcessComplete();

		return prediction;
	}

	private static final Pattern p = Pattern.compile("/");

	/**
	 * Read MEDLINE citations from documents collected using the NCBI web services
	 * 
	 * @param gt
	 * @param dictURI
	 * @param folderName
	 * @return
	 * @throws Exception
	 * 
	 */
	public static Map<String, Set<String>> annotateNCBISet(Map<String, Set<String>> gt, String dictURI,
			String folderName) throws Exception {
		Map<String, Set<String>> prediction = new HashMap<>();

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(ConceptMapperFactory.create(dictURI));

		JCas jCas = JCasFactory.createJCas();

		for (File file : new File(folderName).listFiles()) {
			if (file.getName().endsWith(".gz")) {
				System.out.println(file.getName());

				JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
						.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

				while (cr.hasNext()) {
					cr.getNext(jCas);
					ae.process(jCas);

					String pmid = p.split(ViewUriUtil.getURI(jCas).toString())[1].split("-")[0];

					// Several sections of the citation might be considered
					Set<String> ids = prediction.get(pmid);
					if (ids == null) {
						ids = new HashSet<>();
						prediction.put(pmid, ids);
					}

					for (DictTerm e : JCasUtil.select(jCas, DictTerm.class)) {
						ids.add(getId(e.getDictCanon()));
					}

					//System.out.println(ViewUriUtil.getURI(jCas).toString());
					//System.out.println(jCas.getDocumentText());
					
					jCas.reset();
				}
			}

			// Let's start with only one file for testing, remove when happy
			break;
		}
		
		ae.collectionProcessComplete();

		return prediction;
	}

	public static List<Map<String, Set<String>>[]> getFolds(Map<String, Set<String>> gt) {
		int foldsNumber = 10;

		List<Map<String, Set<String>>[]> folds = new ArrayList<>();

		List<String> ids = new ArrayList<String>(gt.keySet());
		Collections.shuffle(ids);

		int n = (int) ids.size() / foldsNumber;

		for (int i = 0; i < foldsNumber; i++) {
			// Create testing set
			Map<String, Set<String>> testing = new HashMap<>();
			for (int j = i * n; j < (i * n) + n && (i * n) + n < ids.size(); j++) {
				testing.put(ids.get(j), gt.get(ids.get(j)));
			}

			// The rest is training set
			Map<String, Set<String>> training = new HashMap<>();
			for (Map.Entry<String, Set<String>> e : gt.entrySet()) {
				if (!testing.containsKey(e.getKey())) {
					training.put(e.getKey(), e.getValue());
				}
			}

			folds.add(new Map[] { training, testing });
		}

		return folds;
	}

	public static void main(String[] argc) throws Exception {
		Map<String, Set<String>> gt = CharacterizationEvaluation.getGroundTruth(
				"/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/annotations/pubmed-pathogen-characerization-annotations.csv");

		//String dictFileName = "file:/home/antonio/Documents/UoM/testDict.xml";
		String dictFileName = "file:/home/antonio/Documents/UoM/cmDict-NCBI_TAXON.xml.001";

		//Map<String, DocumentEntry> documentMap = BuildDataset
		//		.readDocumentEntries("/home/antonio/Documents/UoM/pathogens-ncbi");
	    CharacterizationEvaluation.evaluate(gt, annotate(gt, dictFileName));
	    /*
		Map<String, Set<String>> predictions = annotateNCBISet(gt, dictFileName,
				"/home/antonio/Documents/UoM/documents/PubMed");

		for (Map.Entry<String, Set<String>> prediction : predictions.entrySet()) {
			DocumentEntry de = documentMap.get(prediction.getKey());
			System.out.println(prediction.getKey());

			Set<String> fp = new HashSet<>(prediction.getValue());

			if (de != null) {
				// What has not been matched from MeSH?
				for (String taxon : de.getMeSHTaxon()) {
					fp.remove(taxon);
					if (!prediction.getValue().contains(taxon)) {
						System.out.println("Missed MeSH " + prediction.getKey() + "/" + taxon);
					} else {
						System.out.println("Found MeSH " + prediction.getKey() + "/" + taxon);
					}
				}

				// What has not been matched from GeneBank?
				for (String taxon : de.getGeneBankTaxon()) {
					fp.remove(taxon);
					if (!prediction.getValue().contains(taxon)) {
						System.out.println("Missed GeneBank " + prediction.getKey() + "/" + taxon);
					} else {
						System.out.println("Found GeneBank " + prediction.getKey() + "/" + taxon);
					}
				}
			}

			for (String taxon : fp) {
				System.out.println("Potential FP " + prediction.getKey() + "/" + taxon);
			}
		}*/

		/*
		 * for (Map<String, Set<String>>[] sets : getFolds(gt)) { Map<String,
		 * Set<String>> trainingSet = sets[0]; Map<String, Set<String>> testingSet =
		 * sets[1];
		 * 
		 * train(trainingSet, dictFileName, "/home/antonio/Documents/UoM/model");
		 * CharacterizationEvaluation.evaluate(testingSet, test(testingSet,
		 * dictFileName, "/home/antonio/Documents/UoM/model/model.jar")); }
		 */
	}
}