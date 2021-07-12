package readbiomed.annotators.characterization;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
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

@Command(name = "PathogenExperimenter", mixinStandardHelpOptions = true, version = "PathogenExperimenter 0.1", description = "Experiments for pathogen characterization.")
public class PathogenExperimenter implements Callable<Integer> {

	private static void evaluate(Map<String, Set<String>> gt, Map<String, Set<String>> predictions) {
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
	}

	@Parameters(index = "0", description = "Dictionary file name.", defaultValue = "file:/Users/ajimeno/Documents/UoM/dictionaries/dict.xml")
	private String dictionaryFileName;
	@Parameters(index = "1", description = "Ground truth file name.", defaultValue = "/Users/ajimeno/Documents/git/readbiomed-bmip-datasets/manual-set/ground-truth/manual-annotation-gt.csv")
	private String gtFileName;
	@Parameters(index = "2", description = "Articles text folder name.", defaultValue = "/Users/ajimeno/Documents/git/readbiomed-bmip-datasets/manual-set/articles-txt-format")
	private String textFolderName;

	@Override
	public Integer call() throws Exception {
		Map<String, Set<String>> gt = readbiomed.annotators.dictionary.pathogens.PathogenExperimenter.getGT(gtFileName, textFolderName);

		Map<String, Set<String>> predictions = new HashMap<>();

		AggregateBuilder pa = PathogenCharacterizationAnnotator.getPipeline(dictionaryFileName);
		// pa.add(SentenceAnnotator.getDescription());
		// pa.add(SDTAnnotator.getDescription(SDTPredictionFolderName));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(pa.createAggregateDescription());

		for (File file : FileUtils.listFiles(new File(textFolderName), new TextFileFilter(), null)) {
			String fileName = file.getName().replaceAll(".txt$", "");

			JCas jCas = JCasFactory.createText(Files.readString(file.toPath()));
			ViewUriUtil.setURI(jCas, new URI(file.getName()));

			ae.process(jCas);

			predictions.put(fileName,
					JCasUtil.select(jCas, NamedEntityMention.class).stream()
							.filter(e -> e.getMentionType().equals("pathogen")).map(e -> e.getMentionId())
							.collect(Collectors.toSet()));
		}

		evaluate(gt, predictions);
		return 0;
	}

	public static void main(String[] argc) throws IOException, SAXException, UIMAException, URISyntaxException {
		int exitCode = new CommandLine(new PathogenExperimenter()).execute(argc);
		System.exit(exitCode);
	}
}