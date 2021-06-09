package readbiomed.annotators.dictionary.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import org.apache.uima.UimaContext;
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
	private static final String OUTPUT_FILE_NAME = "outputFileName";

	private PrintWriter w = null;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		if (context.getConfigParameterValue(OUTPUT_FILE_NAME) != null) {
			try {
				w = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(
						new FileOutputStream((String) context.getConfigParameterValue(OUTPUT_FILE_NAME) + ".gz"))));
			} catch (IOException e) {
				throw new ResourceInitializationException(e);
			}
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		w.close();
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		String pmid = ViewUriUtil.getURI(jCas).toString();

		JCasUtil.select(jCas, NamedEntityMention.class).stream().forEach(e -> w.println(
				pmid + "|" + e.getMentionId() + "|" + e.getBegin() + "|" + e.getEnd() + "|" + e.getCoveredText()));
	}

	public static AnalysisEngineDescription getDescription(String outputFolderName)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(PrintConsumer.class, OUTPUT_FILE_NAME, outputFolderName);
	}
}