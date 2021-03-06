package readbiomed.annotators.dictionary.pathogens;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;
import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.annotators.dictionary.utils.TextFileFilter;

@Command(name = "PathogenExperimenter", mixinStandardHelpOptions = true, version = "PathogenExperimenter 0.1", description = "Pathogen experimenter.")
public class PathogenExperimenter implements Callable<Integer> {

	private static Map<String, Set<String>> annotate(String dictFileName, String textFolderName)
			throws UIMAException, IOException, URISyntaxException, SAXException {
		Map<String, Set<String>> prediction = new HashMap<>();

		AggregateBuilder builder = PathogenDictionaryAnnotator.getPipeline(dictFileName);

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(builder.createAggregateDescription());

		// Annotate and read the annotations
		for (File file : FileUtils.listFiles(new File(textFolderName), new TextFileFilter(), null)) {
			String fileName = file.getName().replaceAll(".txt$", "");

			JCas jCas = JCasFactory.createText(Files.readString(file.toPath()));
			ViewUriUtil.setURI(jCas, new URI(file.getName()));

			ae.process(jCas);

			prediction.put(fileName,
					JCasUtil.select(jCas, NamedEntityMention.class).stream()
							.filter(e -> e.getMentionType().equals("pathogen")).map(e -> e.getMentionId())
							.collect(Collectors.toSet()));
		}

		ae.collectionProcessComplete();

		return prediction;

	}

	@Parameters(index = "0", description = "Dictionary file name.", defaultValue = "file:/home/antonio/Documents/UoM/dictionaries/dict.xml")
	private String dictFileName;

	@Parameters(index = "1", description = "Ground truth file name.", defaultValue = "/home/antonio/Documents/git/readbiomed-bmip-datasets/manual-set/ground-truth/manual-annotation-gt.csv")
	private String gtFileName;

	@Parameters(index = "2", description = "Text folder name.", defaultValue = "/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/corpora/bmip-pubmed-corpus/articles-txt-format")
	private String textFolderName;

	public static Map<String, Set<String>> getGT(String fileName, String textFolderName) throws IOException {
		Map<String, Set<String>> gt = new HashMap<>();

		FileUtils.listFiles(new File(textFolderName), new TextFileFilter(), null).stream()
				.forEach(e -> gt.put(e.getName().replace(".txt", ""), new HashSet<>()));

		// Read CSV. The first line is skipped
		Files.lines(Paths.get(fileName)).map(line -> line.split(",")).skip(1).filter(e -> e.length == 5).forEach(e -> {
			gt.putIfAbsent(e[0], new HashSet<>());
			gt.get(e[0]).add(e[4]);
		});

		return gt;
	}

	@Override
	public Integer call() throws Exception {
		Map<String, Set<String>> gt = getGT(gtFileName, textFolderName);

		System.out.println(gt.size());
		System.out.println(gt);

		Map<String, Set<String>> predictions = annotate(dictFileName, textFolderName);

		double tps = 0.0;
		double fns = 0.0;
		double fps = 0.0;

		// Compare GT
		for (Map.Entry<String, Set<String>> entry : gt.entrySet()) {
			long common = entry.getValue().stream()
					.filter(predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>())::contains).count();

			Set<String> fp = predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>()).stream()
					.filter(e -> !entry.getValue().contains(e)).collect(Collectors.toSet());

			Set<String> fn = entry.getValue().stream()
					.filter(e -> !predictions.computeIfAbsent(entry.getKey(), o -> new HashSet<>()).contains(e))
					.collect(Collectors.toSet());

			System.out.println(entry.getKey() + "|" + common + "|" + entry.getValue().size() + "|"
					+ predictions.get(entry.getKey()).size());

			double recall = common / (double) (common + fn.size());
			double precision = common / (double) (common + fp.size());
			double f1 = (2 * precision * recall) / (precision + recall);

			tps += common;
			fns += fn.size();
			fps += fp.size();

			System.out.println(entry.getKey() + "|" + precision + "|" + recall + "|" + f1);
			System.out.println("FP:" + fp);
			System.out.println("FN:" + fn);
		}

		double recalls = tps / (tps + fns);
		double precisions = tps / (tps + fps);
		double f1s = (2 * precisions * recalls) / (precisions + recalls);

		System.out.println("Overall recall: " + recalls);
		System.out.println("Overall precision: " + precisions);
		System.out.println("Overall f1: " + f1s);
		return 0;
	}

	public static void main(String[] argc) throws UIMAException, IOException, URISyntaxException, SAXException {
		int exitCode = new CommandLine(new PathogenExperimenter()).execute(argc);
		System.exit(exitCode);
	}
}