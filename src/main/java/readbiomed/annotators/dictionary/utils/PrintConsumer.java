package readbiomed.annotators.dictionary.utils;

import java.io.File;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.conceptMapper.DictTerm;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;

public class PrintConsumer extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		String fileName = new File(ViewUriUtil.getURI(jCas)).getName();

		System.out.println(fileName);
		for (DictTerm e : JCasUtil.select(jCas, DictTerm.class)) {
			System.out.println(e.getDictCanon());
			System.out.println(e.getBegin() + "|" + e.getEnd());
			System.out.println(e.getCoveredText());
		}
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(PrintConsumer.class);
	}
}