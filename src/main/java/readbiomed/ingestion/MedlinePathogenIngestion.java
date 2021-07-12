package readbiomed.ingestion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.annotators.dictionary.pathogens.PathogenDictionaryAnnotator;
import readbiomed.readers.medline.MedlineReader;

@Command(name = "MedlinePathogenIngestion", mixinStandardHelpOptions = true, version = "MedlinePathogenIngestion 0.1", description = "Pathogen MEDLINE ingester.")
public class MedlinePathogenIngestion implements Runnable, Callable<Integer> {

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
	
	public MedlinePathogenIngestion()
	{}

	@Override
	public void run() {
		File file = null;

		try {
			ae = AnalysisEngineFactory.createEngine(
					PathogenDictionaryAnnotator.getPipeline(dictionaryFileName).createAggregateDescription());
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

	@Parameters(index = "0", description = "MEDLINE folder name.")
	private String MEDLINEFolderName;
	@Parameters(index = "1", description = "Dictionary file name.")
	private String dictionaryMainFileName;
	@Parameters(index = "2", description = "Output folder name.")
	private String outputMainFolderName;
	@Parameters(index = "3", description = "Number of threads.")
	private Integer nThreads;

	@Override
	public Integer call() throws Exception {
		File folder = new File(MEDLINEFolderName);

		if (folder.isDirectory() && folder.listFiles() != null) {
			for (File file : folder.listFiles()) {
				if (file.getName().endsWith(".xml.gz")) {
					stack.push(file);
				}
			}
		}

		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		for (int i = 0; i < nThreads; i++) {
			Runnable worker = new MedlinePathogenIngestion(dictionaryMainFileName, outputMainFolderName);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		System.out.println("Finished all threads");
		return 0;
	}

	public static void main(String[] argc) {
		int exitCode = new CommandLine(new MedlinePathogenIngestion()).execute(argc);
		System.exit(exitCode);
	}
}