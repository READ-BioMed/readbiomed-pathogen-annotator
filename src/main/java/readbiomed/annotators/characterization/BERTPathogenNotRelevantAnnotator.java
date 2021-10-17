package readbiomed.annotators.characterization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;

import readbiomed.annotators.ml.bert.BERTClient;

public class BERTPathogenNotRelevantAnnotator extends JCasAnnotator_ImplBase {

	private static final String PARAM_BERT_SERVER_PREFIX = "PARAM_BERT_SERVER_PREFIX";

	private BERTClient bert = null;
	
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		String bertServerPrefix = (String) context.getConfigParameterValue(PARAM_BERT_SERVER_PREFIX);
        bert = new BERTClient(bertServerPrefix);
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
			
			Double prediction;
			try {
				prediction = bert.predict(text);
			} catch (IOException e1) {
				throw new AnalysisEngineProcessException(e1);
			}

			System.out.println("Predicted "
					+ prediction
					+ " for " + id);
			// Remove all pathogen mentions if document classified as not relevant and it is
			// an NCBI pathogen
			if (prediction < 0.5) {
				new ArrayList<NamedEntityMention>(JCasUtil.select(jCas, NamedEntityMention.class)).stream()
						.filter(e -> e.getMentionId().equals(id) && e.getMentionId().startsWith("ncbi"))
						.forEach(ne -> ne.removeFromIndexes());
				;
			}
		}
	}

	public static AnalysisEngineDescription getDescription(String bertServerPrefix)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(BERTPathogenNotRelevantAnnotator.class,
				PARAM_BERT_SERVER_PREFIX, bertServerPrefix);
	}
}