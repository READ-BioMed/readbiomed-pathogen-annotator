package readbiomed.annotators.dictionary.pathogens.build.toxins;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.conceptMapper.DictTerm;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ViewUriUtil;

import com.ibm.au.research.nlp.ingestion.uima.reader.MedlineReader;

import readbiomed.annotators.dictionary.utils.ConceptMapperFactory;

public class ToxinAnnotator {
	private static final Pattern p = Pattern.compile("/");

	public static Map<String, Set<String>> annotateNCBISet(String dictURI, String folderName) throws Exception {
		Map<String, Set<String>> prediction = new HashMap<>();

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(ConceptMapperFactory.create(dictURI));

		JCas jCas = JCasFactory.createJCas();

		for (File file : new File(folderName).listFiles()) {
			if (file.getName().endsWith(".xml.gz")) {
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
						ids.add(e.getDictCanon());
						//System.out.println(e);
					}

					// System.out.println(ViewUriUtil.getURI(jCas).toString());
					// System.out.println(jCas.getDocumentText());

					jCas.reset();
				}

				// Let's start with only one file for testing, remove when happy
				//break;
			}
		}

		ae.collectionProcessComplete();

		return prediction;
	}

	public static void main(String[] argc) throws Exception {
		// Map<String, Set<String>> gt = CharacterizationEvaluation.getGroundTruth(
		// "/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/annotations/pubmed-pathogen-characerization-annotations.csv");

		// String dictFileName = "file:/home/antonio/Documents/UoM/testDict.xml";
		String dictFileName = "file:/home/antonio/Documents/UoM/toxin-dict.xml";
		String folderName = "/home/antonio/Documents/UoM/toxin-ncbi-data/PubMed";

		annotateNCBISet(dictFileName, folderName);
	}
}