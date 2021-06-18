package readbiomed.annotators.discourse.sdt;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.token.type.Sentence;
import org.cleartk.util.ViewUriUtil;

import readbiomed.document.SDTSentence;
import readbiomed.document.Section;

/***
 * Read from folder with SDT annotations
 * 
 * @author antonio.jimeno@gmail.com
 *
 */
public class SDTAnnotator extends JCasAnnotator_ImplBase {
	static Logger logger = UIMAFramework.getLogger(SDTAnnotator.class);

	public static final String PARAM_SDT_ANNOTATION_FOLDER_NAME = "SDTAnnotationFolderName";

	private String annotationFolderName;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		annotationFolderName = (String) context.getConfigParameterValue(PARAM_SDT_ANNOTATION_FOLDER_NAME);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		String pmid = ViewUriUtil.getURI(jCas).toString();

		try {
			// Read all lines
			List<String> lines = Files.readAllLines(new File(annotationFolderName).listFiles(new FilenameFilter() {
				public boolean accept(File directory, String filename) {
					return filename.startsWith(pmid);
				}
			})[0].toPath());

			// Assign lines to SDT annotations
			JCasUtil.select(jCas, Section.class).stream().filter(e -> e.getSectionType().equalsIgnoreCase("abstract"))
					.map(e -> JCasUtil.selectCovered(jCas, Sentence.class, e)).forEach(e -> {
						for (Sentence s : e) {
							SDTSentence sdt = new SDTSentence(jCas, s.getBegin(), s.getEnd());
							sdt.setSdtType(lines.remove(0));
							sdt.addToIndexes();
						}
					});
		} catch (IOException e) {
			logger.log(Level.WARNING, "No SDT annotation for " + pmid);
		}
	}

	public static AnalysisEngineDescription getDescription(String SDTAnnotationFolderName)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(SDTAnnotator.class, PARAM_SDT_ANNOTATION_FOLDER_NAME,
				SDTAnnotationFolderName);
	}
}
