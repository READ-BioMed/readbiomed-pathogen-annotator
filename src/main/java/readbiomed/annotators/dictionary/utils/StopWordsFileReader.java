package readbiomed.annotators.dictionary.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StopWordsFileReader {
	public static List<String> read(String fileName) throws FileNotFoundException, IOException {
		List<String> stopWordsItems = new ArrayList<>();

		try (BufferedReader b = new BufferedReader(new FileReader(new File(fileName)))) {
			String line;
			while ((line = b.readLine()) != null) {
				stopWordsItems.add(line.trim());
			}
		}

		return stopWordsItems;
	}
}
