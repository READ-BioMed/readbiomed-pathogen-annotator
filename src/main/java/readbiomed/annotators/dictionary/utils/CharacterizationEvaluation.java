package readbiomed.annotators.dictionary.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class CharacterizationEvaluation {

	private static final Pattern p = Pattern.compile(",");

	public static Map<String, Set<String>> getGT(String fileName) throws IOException {
		Map<String, Set<String>> gt = new HashMap<>();

		FileUtils.listFiles(new File(
				"/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/corpora/bmip-pubmed-corpus/articles-txt-format"),
				new TextFileFilter(), null).stream()
				.forEach(e -> gt.put(e.getName().replace(".txt", ""), new HashSet<>()));

		// Read CSV. The first line is skipped
		Files.lines(Paths.get(fileName)).map(line -> line.split(",")).skip(1).filter(e -> e.length == 5).forEach(e -> {
			gt.putIfAbsent(e[0], new HashSet<>());
			gt.get(e[0]).add(e[4]);
		});

		return gt;
	}
	
	public static Map<String, Set<String>> getGroundTruth(String fileName) throws FileNotFoundException, IOException {
		Map<String, Set<String>> mappings = new HashMap<>();

		try (BufferedReader b = new BufferedReader(new FileReader(fileName))) {
			String line;
			// Skip first line
			b.readLine();

			while ((line = b.readLine()) != null) {
				String[] tokens = p.split(line);
				String id = tokens[0];
				Set<String> set = new HashSet<>();
				mappings.put(id, set);

				for (int i = 1; i < tokens.length; i += 2) {
					set.add(tokens[i + 1]);
				}
			}
		}

		return mappings;
	}

	public static Map<String, Set<String>> getConcepMapper(String fileName) throws FileNotFoundException, IOException {
		Map<String, Set<String>> mappings = new HashMap<>();

		try (BufferedReader b = new BufferedReader(new FileReader(fileName))) {
			String line;
			// Skip first line
			b.readLine();

			while ((line = b.readLine()) != null) {
				String[] tokens = p.split(line);
				String id = tokens[0];
				Set<String> set = mappings.get(id);

				if (set == null) {
					set = new HashSet<>();
					mappings.put(id, set);
				}

				set.add(tokens[1]);
			}
		}

		return mappings;
	}

	private static double divide(double n, double d) {
		try {
			return n / d;
		} catch (Exception e) {
			return 0.0;
		}
	}

	public static void evaluate(Map<String, Set<String>> gt, Map<String, Set<String>> prediction) {
		// Count total gt
		int positives = gt.entrySet().stream().mapToInt(x -> x.getValue().size()).sum();

		// Count total predicted
		int predicted = prediction.entrySet().stream().mapToInt(x -> x.getValue().size()).sum();

		int truePositives = 0;
		// Count total matched
		for (Map.Entry<String, Set<String>> entry : gt.entrySet()) {
			for (String s : entry.getValue()) {
				if (prediction.get(entry.getKey()) != null && prediction.get(entry.getKey()).contains(s)) {
					truePositives++;
				} else {
					//System.err.println("Missed PMID:" + entry.getKey() + " Tax Id:" + s);
				}
			}
		}

		// Recall
		double recall = divide(truePositives, positives);
		// Precision
		double precision = divide(truePositives, predicted);
		// F1
		double f1 = divide(2 * recall * precision, precision + recall);

		System.out.println("Precision: " + precision);
		System.out.println("Recall:    " + recall);
		System.out.println("F1:        " + f1);
	}

	public static void main(String[] argc) throws FileNotFoundException, IOException {
		evaluate(getGroundTruth(
				"/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/annotations/pubmed-pathogen-characerization-annotations.csv"),
				getConcepMapper(
						"/home/antonio/Downloads/bmip/readbiomed-bmip-8648708be55b/data/annotations/pmib_taxon_sdt_labels.csv"));
	}
}