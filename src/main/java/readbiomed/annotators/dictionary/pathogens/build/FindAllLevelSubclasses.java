package readbiomed.annotators.dictionary.pathogens.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import readbiomed.annotators.dictionary.utils.Utils;

public class FindAllLevelSubclasses {
	public static int traverse(List<String> searchItemsList_TaxonIDs_CurrentLevel, List<OwlClass> owlClassesResults)
			throws IOException {
		List<String> searchItemsList_TaxonIDs_NextLevel = new ArrayList<>();

		try (BufferedReader b = new BufferedReader(new FileReader(new File(InputParameters.OWLFileName)))) {

			String line;
			while ((line = b.readLine()) != null) {
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
					List<String> rawAxiomStringsforCurrentClass = new ArrayList<>();
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

					// Checking whether the owl class is subclass of our search item or not
					String stringToSearch_subclass = StringUtils.substringBetween(owlClassString,
							"<rdfs:subClassOf rdf:resource=\"", "\"");

					// check cloned_searchItemsList null or not
					if (searchItemsList_TaxonIDs_CurrentLevel.contains(stringToSearch_subclass)) {
						OwlClass owlclass = new OwlClass();
						owlclass.setRawOwlClassString(owlClassString);
						owlclass.setId(StringUtils.substringBetween(owlClassString, "rdf:about=\"", "\""));
						owlclass.setCanonical(StringUtils.substringBetween(owlClassString,
								"<rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">",
								"</rdfs:label>"));

						// Add Axiom Information to Owl Class Object
						for (String axiomString : rawAxiomStringsforCurrentClass) {
							owlclass.getRawAxiomStrings().add(axiomString);
							owlclass.getVariants().add(StringUtils.substringBetween(axiomString,
									"<owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">",
									"</owl:annotatedTarget>"));
						}

						////////////// Axiom by Stop Words//////////////
						Utils.addStopWordAxiom(owlclass);

						if (!Utils.hasSameOwlClass(owlclass, owlClassesResults)) {
							searchItemsList_TaxonIDs_NextLevel.add(owlclass.getId());
							owlClassesResults.add(owlclass);
						}
					}
					// Owl class is not a subclass
				}
			}
		}

		if (searchItemsList_TaxonIDs_NextLevel.isEmpty())
			return 0;
		else
			traverse(searchItemsList_TaxonIDs_NextLevel, owlClassesResults);

		return searchItemsList_TaxonIDs_NextLevel.size();
	}
}
