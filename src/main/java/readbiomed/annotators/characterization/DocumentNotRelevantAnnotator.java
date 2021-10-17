package readbiomed.annotators.characterization;

import java.util.ArrayList;

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

public class DocumentNotRelevantAnnotator extends MTIMLAnnotator {

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		//Map <String, String> map = JCasUtil.select(jCas, Section.class).stream().collect(Collectors.toMap(Section::getSectionType, Section::getCoveredText));
		
		Document d = new Document();
		d.addField("TEXT", jCas.getDocumentText());

		Instance i = getFeatureExtractor().prepareInstance(d);

		String pmid = ViewUriUtil.getURI(jCas).toString();
		System.out.println(pmid);

		// Remove all pathogen mentions if document classified as not relevant
		System.out.println("Predicted " + getClassifier().predict(i));

		if (((readbiomed.mme.classifiers.SGD)getClassifier()).predictProbability(i).getConfidence() < 0.4) {
			new ArrayList<NamedEntityMention>(JCasUtil.select(jCas, NamedEntityMention.class)).stream()
					// Remove only NCBI annotations
					.filter(e -> e.getMentionType().contentEquals("pathogen") && e.getMentionId().startsWith("ncbi-"))
					.forEach(e -> e.removeFromIndexes());
		}
	}

	public static AnalysisEngineDescription getDescription(String trieFileName, String classifiersFileName,
			String featureExtractorClassName, String featureExtractorParameters)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(DocumentNotRelevantAnnotator.class,
				PARAM_TRIE_FILE_NAME, trieFileName, PARAM_CLASSIFIERS_FILE_NAME, classifiersFileName,
				PARAM_FEATURE_EXTRACTOR_CLASS_NAME, featureExtractorClassName, PARAM_FEATURE_EXTRACTOR_PARAMETERS,
				featureExtractorParameters);
	}
}