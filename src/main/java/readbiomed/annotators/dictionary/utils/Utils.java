package readbiomed.annotators.dictionary.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import readbiomed.annotators.dictionary.pathogens.build.InputParameters;
import readbiomed.annotators.dictionary.pathogens.build.OwlClass;

public class Utils {

	public static int numberOfExactMatches = 0;

	public static String findSameItemInAxiomIfExists(List<String> searchItemsList,
			List<String> rawAxiomStringsforCurrentClass) {

		for (int i = 0; i < rawAxiomStringsforCurrentClass.size(); i++) {
			String searchItem = StringUtils.substringBetween(rawAxiomStringsforCurrentClass.get(i),
					"<owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">",
					"</owl:annotatedTarget>");
			if (searchItemsList.contains(searchItem))
				return searchItem;

		}

		return null;

	}

	public static boolean findPartialMatchItem_class(List<String> searchItemsList, String stringToSearch_class) {
		for (String searchItem : searchItemsList) {
			if (stringToSearch_class.startsWith(searchItem + " ")
					|| stringToSearch_class.contains(" " + searchItem + " ")
					|| stringToSearch_class.endsWith(" " + searchItem) || stringToSearch_class.equals(searchItem)) {
				if (stringToSearch_class.equals(searchItem)) {
					numberOfExactMatches++;
				}
				return true;
			}
		}

		return false;

	}

	public static boolean findPartialMatchItem_axiom(List<String> searchItemsList,
			List<String> rawAxiomStringsforCurrentClass) {

		for (int i = 0; i < rawAxiomStringsforCurrentClass.size(); i++) {
			String searchItem_axiom = StringUtils.substringBetween(rawAxiomStringsforCurrentClass.get(i),
					"<owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">",
					"</owl:annotatedTarget>");

			for (int j = 0; j < searchItemsList.size(); j++) {
				String searchItem = searchItemsList.get(j);
				if (searchItem_axiom.startsWith(searchItem + " ") || searchItem_axiom.contains(" " + searchItem + " ")
						|| searchItem_axiom.endsWith(" " + searchItem) || searchItem_axiom.equals(searchItem)) {
					if (searchItem_axiom.equals(searchItem)) {
						numberOfExactMatches++;
					}
					return true;
				}
			}

		}
		return false;
	}

	public static List<String> findUnfoundSearchItems(List<String> searchItemsList, List<OwlClass> owlClassesResuts) {

		List<String> unFoundItems = new ArrayList<>(searchItemsList);

		for (int i = 0; i < searchItemsList.size(); i++) {
			String searchItem = searchItemsList.get(i);

			outerloop: for (int j = 0; j < owlClassesResuts.size(); j++) {
				OwlClass owlclass = owlClassesResuts.get(j);
				if (owlclass.getCanonical().contains(" " + searchItem + " ")
						|| owlclass.getCanonical().startsWith(searchItem + " ")
						|| owlclass.getCanonical().endsWith(" " + searchItem)
						|| owlclass.getCanonical().equals(searchItem)) {
					unFoundItems.remove(searchItem);
					break outerloop;
				}
				for (int j2 = 0; j2 < owlclass.getVariants().size(); j2++) {
					String variant = owlclass.getVariants().get(j2);
					if (variant.contains(" " + searchItem + " ") || variant.startsWith(searchItem + " ")
							|| variant.endsWith(" " + searchItem) || variant.equals(searchItem)) {
						unFoundItems.remove(searchItem);
						break outerloop;
					}
				}
			}

		}

		return unFoundItems;
	}

	public static void addStopWordAxiom(OwlClass owlclass) throws FileNotFoundException, IOException {
		List<String> stopWords = StopWordsFileReader.read(InputParameters.StopWordsFileName);
		for (String stopWord : stopWords) {
			if (owlclass.getCanonical().endsWith(" " + stopWord)) {
				String newVariant = StringUtils.substringBefore(owlclass.getCanonical(), " " + stopWord);
				if (!owlclass.getVariants().contains(newVariant))
					owlclass.getVariants().add(newVariant);
				break;
			}

			if (owlclass.getCanonical().contains(" " + stopWord + " ")) {
				String newVariant = owlclass.getCanonical().replace(stopWord + " ", "");
				if (!owlclass.getVariants().contains(newVariant))
					owlclass.getVariants().add(newVariant);
				break;
			}

			if (owlclass.getCanonical().startsWith(stopWord + " ")) {
				String newVariant = StringUtils.substringBefore(owlclass.getCanonical(), stopWord + " ");
				if (!owlclass.getVariants().contains(newVariant))
					owlclass.getVariants().add(newVariant);
				break;
			}

		}

	}

	public static boolean hasSameOwlClass(OwlClass owlclass, List<OwlClass> owlClassesResults) {
		for (OwlClass owlClassResult : owlClassesResults) {
			if (owlClassResult.getId().equals(owlclass.getId()))
				return true;
		}
		return false;
	}
}
