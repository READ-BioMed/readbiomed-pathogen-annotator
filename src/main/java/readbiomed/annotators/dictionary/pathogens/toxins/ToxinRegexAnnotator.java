package readbiomed.annotators.dictionary.pathogens.toxins;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;

import readbiomed.bmip.dataset.toxins.ToxinDocuments;

public class ToxinRegexAnnotator extends JCasAnnotator_ImplBase {

	private Map<String, Pattern> patterns = new HashMap<>();

	private static Pattern compileToxinExpression(String[] toxin) {
		String regex = "(?i)(" + toxin[0].replaceAll(" ", ".*").replaceAll("s$", "") + "|"
				+ toxin[1].replaceAll(" ", ".*").replaceAll("s$", "") + ")";
		return Pattern.compile(regex);
	}

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		for (String[] toxin : ToxinDocuments.toxins) {
			patterns.put("toxin-" + toxin[1].toLowerCase(), compileToxinExpression(toxin));
		}
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
			Pattern p = entry.getValue();
			Matcher m = p.matcher(jCas.getDocumentText());

			while (m.find()) {
				NamedEntityMention ne = new NamedEntityMention(jCas, m.start(), m.end());
				ne.setMentionType("pathogen");
				ne.setMentionId(entry.getKey());
				jCas.addFsToIndexes(ne);
			}

		}
	}

	public static AnalysisEngineDescription getDescription()
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(ToxinRegexAnnotator.class);
	}
}