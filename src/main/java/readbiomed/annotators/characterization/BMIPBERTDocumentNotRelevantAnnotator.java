package readbiomed.annotators.characterization;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;

import readbiomed.annotators.ml.bert.BERTClient;

public class BMIPBERTDocumentNotRelevantAnnotator extends JCasAnnotator_ImplBase {

	private static final String PARAM_BERT_SERVER_PREFIX = "PARAM_BERT_SERVER_PREFIX";

	private BERTClient bert = null;
	
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		String bertServerPrefix = (String) context.getConfigParameterValue(PARAM_BERT_SERVER_PREFIX);
        bert = new BERTClient(bertServerPrefix);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
        Double prediction;
		try {
			prediction = bert.predict(jCas.getDocumentText());
		} catch (IOException e1) {
			throw new AnalysisEngineProcessException(e1);
		}

		// Remove all pathogen mentions if document classified as not relevant
		System.out.println("Predicted " + prediction);

		if (prediction < 0.005) {
			new ArrayList<NamedEntityMention>(JCasUtil.select(jCas, NamedEntityMention.class)).stream()
					// Remove only NCBI annotations
					.filter(e -> e.getMentionType().contentEquals("pathogen") && e.getMentionId().startsWith("ncbi-"))
					.forEach(e -> e.removeFromIndexes());
		}
	}

	public static AnalysisEngineDescription getDescription(String bertServerPrefix)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(BMIPBERTDocumentNotRelevantAnnotator.class,
				PARAM_BERT_SERVER_PREFIX, bertServerPrefix);
	}
}