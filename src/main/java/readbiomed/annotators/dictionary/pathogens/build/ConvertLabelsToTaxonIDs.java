package readbiomed.annotators.dictionary.pathogens.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.commons.lang3.StringUtils;

import readbiomed.annotators.dictionary.utils.Utils;

public class ConvertLabelsToTaxonIDs {

	public static List<String> convert(List<String> searchItemsList) throws IOException {

		List<String> searchItemsList_TaxonIDs = new ArrayList<>();

		List<String> searchItemsList_Temp = new ArrayList<>(searchItemsList);

		try (BufferedReader b = new BufferedReader(new FileReader(new File(InputParameters.OWLFileName)))) {

			String line;
			while ((line = b.readLine()) != null && !searchItemsList_Temp.isEmpty()) {
				/////// If a new owl class is found ///////////
				if (line.contains("<owl:Class")) {
					String owlClassString = line + "\n";
					String classLine;
					while ((classLine = b.readLine()) != null) {
						owlClassString += classLine + "\n";
						if (classLine.endsWith("</owl:Class>")) {
							break;
						}
					} // Raw String of owl class is built

					// Finding Potential Axioms After Class
					ArrayList<String> rawAxiomStringsforCurrentClass = new ArrayList<>();
					while ((line = b.readLine()) != null) {
						if (line.contains("<owl:Axiom>")) {
							String axiomString = line + "\n";
							String axiomLine;
							while ((axiomLine = b.readLine()) != null) {
								axiomString += axiomLine + "\n";
								if (axiomLine.endsWith("</owl:Axiom>")) {
									rawAxiomStringsforCurrentClass.add(axiomString);
									break;
								}
							}
						} else
							break;
					} // End Finding Potential Axioms After Class

					// Checking whether the owl class is our search item or not
					// String stringToSearch_Axiom =
					// Utils.findSameItemIfExists(searchItemsList_Temp,
					// rawAxiomStringsforCurrentClass);;
					String stringToSearch_class = "";

					stringToSearch_class = StringUtils.substringBetween(owlClassString,
							"<rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">", "</rdfs:label>");

					if (Utils.findPartialMatchItem_class(searchItemsList_Temp, stringToSearch_class)
							|| Utils.findPartialMatchItem_axiom(searchItemsList_Temp, rawAxiomStringsforCurrentClass)) {
						searchItemsList_TaxonIDs
								.add(StringUtils.substringBetween(owlClassString, "rdf:about=\"", "\""));

//						System.out.println("************************************");
//						System.out.println(Utils.findPartialMatchItem_class(searchItemsList_Temp, stringToSearch_class));
//						System.out.println(Utils.findPartialMatchItem_axiom(searchItemsList_Temp, rawAxiomStringsforCurrentClass));
//						System.out.println(owlClassString);
					}
				}
			}

		}

		return searchItemsList_TaxonIDs;
	}
}
