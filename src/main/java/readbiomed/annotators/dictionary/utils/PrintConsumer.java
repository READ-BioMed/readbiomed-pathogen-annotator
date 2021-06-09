package readbiomed.annotators.dictionary.utils;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;

public class PrintConsumer extends JCasAnnotator_ImplBase {
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {		
		String pmid = ViewUriUtil.getURI(jCas).toString();

		JCasUtil.select(jCas, NamedEntityMention.class).stream()
				.forEach(e -> System.out.println(pmid + "|" + e.getMentionId() + "|" + e.getBegin()
						+ "|" + e.getEnd() + "|" + e.getCoveredText()));
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(PrintConsumer.class);
	}
}