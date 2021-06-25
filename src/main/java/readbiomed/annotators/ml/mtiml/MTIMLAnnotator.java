package readbiomed.annotators.ml.mtiml;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;

import gov.nih.nlm.nls.mti.classifiers.ova.OVAClassifier;
import gov.nih.nlm.nls.mti.featuresextractors.FeatureExtractor;
import gov.nih.nlm.nls.mti.featuresextractors.FeatureExtractorFactory;
import gov.nih.nlm.nls.utils.Trie;
import readbiomed.annotators.dictionary.utils.Serialization;

public abstract class MTIMLAnnotator extends JCasAnnotator_ImplBase {
	public static final String PARAM_TRIE_FILE_NAME = "trie_file_name";
	public static final String PARAM_CLASSIFIERS_FILE_NAME = "classifiers_file_name";

	public static final String PARAM_FEATURE_EXTRACTOR_CLASS_NAME = "feature_extractor_class_name";
	public static final String PARAM_FEATURE_EXTRACTOR_PARAMETERS = "feature_extractor_parameters";

	private Trie<Integer> trieTerms = null;
	private OVAClassifier classifier = null;
	private FeatureExtractor fe = null;

	@SuppressWarnings("unchecked")
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		try {
			trieTerms = (Trie<Integer>) Serialization.readObject(context, PARAM_TRIE_FILE_NAME);
			classifier = ((Map<String, OVAClassifier>) Serialization.readObject(context, PARAM_CLASSIFIERS_FILE_NAME))
					.get("Y");

			String feClassName = (String) context.getConfigParameterValue(PARAM_FEATURE_EXTRACTOR_CLASS_NAME);
			String feParameters = (String) context.getConfigParameterValue(PARAM_FEATURE_EXTRACTOR_PARAMETERS);
			fe = FeatureExtractorFactory.create(feClassName, feParameters, trieTerms, new HashMap<Integer, String>());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public FeatureExtractor getFeatureExtractor() {
		return fe;
	}
	
	public OVAClassifier getClassifier() {
		return classifier;
	}
}