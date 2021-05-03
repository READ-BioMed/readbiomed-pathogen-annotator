package readbiomed.annotators.dictionary.pathogens.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CSVFileReader {

	private static final int Canonical_Column = 0;

	public static List<String> read(String fileName) throws IOException {

		Set<String> searchItemsSet = new HashSet<>();

		try (BufferedReader sc = new BufferedReader(new FileReader(new File(fileName)))) {
			String line;
			while ((line = sc.readLine()) != null) {
				String item = line.split(",")[Canonical_Column];
				searchItemsSet.add(item);
			}
		}

		return new ArrayList<String>(searchItemsSet);
	}
}
