package readbiomed.ingestion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import javax.naming.NamingException;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;
import org.xml.sax.SAXException;

import readbiomed.annotators.dictionary.pathogens.PathogenDictionaryAnnotator;
import readbiomed.readers.medline.MedlineReader;

public class MedlinePathogenIngestion implements Runnable {

	private static Stack<File> stack = new Stack<File>();

	private static synchronized File getNext() {
		if (stack.isEmpty())
			return null;
		return stack.pop();
	}

	private String outputFolderName;
	private String dictionaryFileName;
	private static AnalysisEngine ae = null;

	public MedlinePathogenIngestion(String dictionaryFileName, String outputFolderName)
			throws ResourceInitializationException, InvalidXMLException, IOException, SAXException {
		this.outputFolderName = outputFolderName;
		this.dictionaryFileName = dictionaryFileName;
	}

	@Override
	public void run() {
		File file = null;
		
		try {
			ae = AnalysisEngineFactory
					.createEngine(PathogenDictionaryAnnotator.getPipeline(dictionaryFileName).createAggregateDescription());
		} catch (ResourceInitializationException | InvalidXMLException | IOException | SAXException e1) {
			e1.printStackTrace();
			return;
		}
		
		while ((file = getNext()) != null) {
			System.out.println("Indexing: " + file.getName());

			try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(
					new FileOutputStream(new File(outputFolderName, file.getName() + ".txt.gz")))))) {

				JCas jCas = JCasFactory.createJCas();

				JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
						.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

				while (cr.hasNext()) {
					cr.getNext(jCas);

					ae.process(jCas);

					String pmid = ViewUriUtil.getURI(jCas).toString();

					JCasUtil.select(jCas, NamedEntityMention.class).stream().forEach(e -> w.println(pmid + "|"
							+ e.getMentionId() + "|" + e.getBegin() + "|" + e.getEnd() + "|" + e.getCoveredText()));

					jCas.reset();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println("Finished: " + file.getName());
		}
	}

	public static void main(String[] argc) throws ClassNotFoundException, NamingException, SQLException,
			ResourceInitializationException, InvalidXMLException, IOException, SAXException {

		File folder = new File(argc[0]);
		int nThreads = Integer.parseInt(argc[3]);

		if (folder.isDirectory() && folder.listFiles() != null) {
			for (File file : folder.listFiles()) {
				if (file.getName().endsWith(".xml.gz")) {
					stack.push(file);
				}
			}
		}

		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		for (int i = 0; i < nThreads; i++) {
			Runnable worker = new MedlinePathogenIngestion(argc[1], argc[2]);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		System.out.println("Finished all threads");
	}
}