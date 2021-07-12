package readbiomed.annotators.discourse.sdt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.token.type.Sentence;
import org.cleartk.util.ViewUriUtil;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.document.Section;
import readbiomed.readers.medline.MedlineReader;

@Command(name = "GenerateLineFiles", mixinStandardHelpOptions = true, version = "GenerateLineFiles 0.1", description = "SDT line generation.")
public class GenerateLineFiles implements Callable<Integer> {

	@Parameters(index = "0", description = "Input data folder name.")
	private String inputFolderName;
	@Parameters(index = "1", description = "Output folder name.")
	private String outputFolderName;

	@Override
	public Integer call() throws Exception {
		AnalysisEngine ae = AnalysisEngineFactory.createEngine(SentenceAnnotator.getDescription());

		JCas jCas = JCasFactory.createJCas();

		for (File file : new File(inputFolderName).listFiles()) {

			JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
					.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

			while (cr.hasNext()) {
				cr.getNext(jCas);

				ae.process(jCas);

				String pmid = ViewUriUtil.getURI(jCas).toString();

				try (FileWriter w = new FileWriter(new File(outputFolderName, pmid))) {
					JCasUtil.select(jCas, Section.class).stream()
							.filter(e -> e.getSectionType().equalsIgnoreCase("abstract"))
							.map(e -> JCasUtil.selectCovered(jCas, Sentence.class, e)).forEach(e -> {
								for (Sentence s : e) {
									System.out.println(pmid + "|" + s.getBegin() + "|" + s.getCoveredText());

									try {
										w.write(s.getCoveredText().toLowerCase());
										w.write("\n");
									} catch (IOException e1) {
										e1.printStackTrace();
									}

								}
							});
					;

				}
				jCas.reset();
			}
		}
		return 0;
	}

	public static void main(String[] argc) throws UIMAException, FileNotFoundException, CASAdminException, IOException {
		int exitCode = new CommandLine(new GenerateLineFiles()).execute(argc);
		System.exit(exitCode);
	}
}