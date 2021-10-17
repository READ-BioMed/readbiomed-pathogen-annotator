package readbiomed.annotators.characterization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;

import gov.nih.nlm.nls.mti.documents.Document;
import gov.nih.nlm.nls.mti.instances.Instance;
import readbiomed.annotators.ml.mtiml.MTIMLAnnotator;

public class PathogenNotRelevantAnnotator extends MTIMLAnnotator {

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		List<NamedEntityMention> list = new ArrayList<>();
		Set<String> ids = new HashSet<>();

		for (NamedEntityMention ne : JCasUtil.select(jCas, NamedEntityMention.class)) {
			ids.add(ne.getMentionId());
			list.add(ne);
		}

		String pmid = ViewUriUtil.getURI(jCas).toString();
		System.out.println(pmid);

		Set<NamedEntityMention> removal = new HashSet<>();

		// Remove potential overlapping mentions of the same pathogen
		for (NamedEntityMention ne : list) {
			for (NamedEntityMention neIn : list) {
				if (ne != neIn) {
					if (ne.getMentionId().equals(neIn.getMentionId())) {
						if (ne.getBegin() == neIn.getBegin() || ne.getEnd() == neIn.getEnd()) {
							if (!(removal.contains(ne) || removal.contains(neIn))) {
								removal.add(ne);
							}
						}
					}
				}

			}
		}

		for (NamedEntityMention ne : removal) {
			list.remove(ne);
		}

		Collections.sort(list, new RelevantPathogenSet().new SortNamedEntityMentions());

		for (String id : ids) {
			String text = jCas.getDocumentText();

			for (NamedEntityMention ne : list) {
				if (ne.getMentionId().contentEquals(id)) {
					if (ne.getMentionId().equals(id)) {
						text = text.substring(0, ne.getBegin()) + "@PATHOGEN$" + text.substring(ne.getEnd());
					}
				}
			}

			Document d = new Document();
			d.addField("TEXT", text);

			Instance i = getFeatureExtractor().prepareInstance(d);

			System.out.println("Predicted "
					+ ((readbiomed.mme.classifiers.SGD) getClassifier()).predictProbability(i).getConfidence()
					+ " for " + id);
			// Remove all pathogen mentions if document classified as not relevant and it is
			// an NCBI pathogen
			if (((readbiomed.mme.classifiers.SGD) getClassifier()).predictProbability(i).getConfidence() < 0.5) {
				new ArrayList<NamedEntityMention>(JCasUtil.select(jCas, NamedEntityMention.class)).stream()
						.filter(e -> e.getMentionId().equals(id) && e.getMentionId().startsWith("ncbi"))
						.forEach(ne -> ne.removeFromIndexes());
				;
			}
		}
	}

	public static AnalysisEngineDescription getDescription(String trieFileName, String classifiersFileName,
			String featureExtractorClassName, String featureExtractorParameters)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(PathogenNotRelevantAnnotator.class,
				PARAM_TRIE_FILE_NAME, trieFileName, PARAM_CLASSIFIERS_FILE_NAME, classifiersFileName,
				PARAM_FEATURE_EXTRACTOR_CLASS_NAME, featureExtractorClassName, PARAM_FEATURE_EXTRACTOR_PARAMETERS,
				featureExtractorParameters);
	}
}