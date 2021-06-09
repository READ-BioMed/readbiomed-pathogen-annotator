package readbiomed.ingestion;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.NamingException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

import readbiomed.annotators.dictionary.pathogens.PathogenAnnotator;
import readbiomed.annotators.dictionary.utils.PrintConsumer;
import readbiomed.readers.medline.MedlineReader;

public class MedlinePathogenIngestion implements Runnable {

	private static Stack<File> stack = new Stack<File>();

	private static synchronized File getNext() {
		if (stack.isEmpty())
			return null;
		return stack.pop();
	}

	public static Map<String, Integer> mapCanonical = Collections.synchronizedMap(new HashMap<>());

	private String PrPSCDictionaryFileName;
	private String NCBITaxonomyDictionaryFileName;

	public MedlinePathogenIngestion(String PrPSCDictionaryFileName, String NCBITaxonomyDictionaryFileName) {
		this.PrPSCDictionaryFileName = PrPSCDictionaryFileName;
		this.NCBITaxonomyDictionaryFileName = NCBITaxonomyDictionaryFileName;
	}

	@Override
	public void run() {
		File file = null;
		while ((file = getNext()) != null) {
			System.out.println("Indexing: " + file.getName());

			AggregateBuilder builder;
			try {
				builder = PathogenAnnotator.getPipeline(PrPSCDictionaryFileName, NCBITaxonomyDictionaryFileName);
				builder.add(PrintConsumer.getDescription());
				builder.createAggregateDescription();

				SimplePipeline.runPipeline(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()),
						builder.createAggregateDescription());

			} catch (InvalidXMLException | ResourceInitializationException | IOException | SAXException e1) {
				e1.printStackTrace();
			} catch (UIMAException e) {
				e.printStackTrace();
			}

			System.out.println("Finished: " + file.getName());
		}
	}

	public static void main(String[] argc) throws ClassNotFoundException, NamingException, SQLException {

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