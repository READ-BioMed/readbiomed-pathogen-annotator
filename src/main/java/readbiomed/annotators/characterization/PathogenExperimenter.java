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

import readbiomed.annotators.dictionary.utils.CharacterizationEvaluation;
import readbiomed.annotators.dictionary.utils.TextFileFilter;

public class PathogenExperimenter {

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

	public static void main(String[] argc) throws IOException, SAXException, UIMAException, URISyntaxException {
		String inputFolderName = argc[0];
		String dictionaryFileName = argc[1];
		String SDTPredictionFolderName = argc[2];

		Map<String, Set<String>> gt = CharacterizationEvaluation.getGT(
				"/home/antonio/Documents/git/readbiomed-bmip-datasets/manual-set/ground-truth/manual-annotation-gt.csv");

		Map<String, Set<String>> predictions = new HashMap<>();

		AggregateBuilder pa = PathogenCharacterizationAnnotator.getPipeline(dictionaryFileName);
		// pa.add(SentenceAnnotator.getDescription());
		// pa.add(SDTAnnotator.getDescription(SDTPredictionFolderName));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(pa.createAggregateDescription());

		for (File file : FileUtils.listFiles(new File(
				"/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/corpora/bmip-pubmed-corpus/articles-txt-format"),
				new TextFileFilter(), null)) {
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
	}
}