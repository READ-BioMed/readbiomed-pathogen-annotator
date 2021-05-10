package readbiomed.annotators.dictionary.pathogens.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import readbiomed.annotators.dictionary.utils.Utils;

public class NCBIDictionaryBuilder {

	public static void main(String[] args) throws IOException {
		String folder = args[0];

		InputParameters.OWLFileName = folder + "/" + InputParameters.OWLFileName;
		InputParameters.XMLFileName = folder + "/" + InputParameters.XMLFileName;
		InputParameters.CSVFileName = folder + "/" + InputParameters.CSVFileName;
		InputParameters.StopWordsFileName = folder + "/" + InputParameters.StopWordsFileName;
		InputParameters.NotFoundItemsFileName = folder + "/" + InputParameters.NotFoundItemsFileName;

		List<String> searchItemsList = CSVFileReader.read(InputParameters.CSVFileName);

		List<OwlClass> owlClassesResults = new ArrayList<>();

		try (BufferedReader b = new BufferedReader(new FileReader(new File(InputParameters.OWLFileName)))) {
			String line;
			while ((line = b.readLine()) != null) {
				// Class
				if (line.contains("<owl:Class")) {
					String owlClassString = line + "\n";
					String classLine;
					while ((classLine = b.readLine()) != null) {
						owlClassString += classLine + "\n";
						if (classLine.endsWith("</owl:Class>")) {
							break;
						}
					} // Raw String of owl class is built

					// Axioms
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
					}

					// Checking whether the owl class is our search item or not (by checking class
					// name, axioms names and subclass field)
					String stringToSearch_class = "";

					if (InputParameters.searchMode == SearchMode.BY_LABEL) {
						stringToSearch_class = StringUtils.substringBetween(owlClassString,
								"<rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">",
								"</rdfs:label>");
					} else if (InputParameters.searchMode == SearchMode.BY_TAXONID) {
						stringToSearch_class = StringUtils.substringBetween(owlClassString, "rdf:about=\"", "\"");
					}

					// check cloned_searchItemsList null or not
					if (Utils.findPartialMatchItem_class(searchItemsList, stringToSearch_class)
							|| Utils.findPartialMatchItem_axiom(searchItemsList, rawAxiomStringsforCurrentClass)) {
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

						owlClassesResults.add(owlclass);
					}
					// Owl class is not our search Item (non of three conditions is satisfied)
				}
			}

		}

		////////////////// Find Unfound Search Items in Owl File///////////////////
		List<String> unFoundSearchItems = Utils.findUnfoundSearchItems(searchItemsList, owlClassesResults);

		System.out.println("Number of Exact Matches: " + Utils.numberOfExactMatches);

		//////////////////////// Find All level subclasses in Owl
		//////////////////////// file///////////////////////////////
		List<String> searchItemsList_TaxonIDs_CurrentLevel = null;
		if (InputParameters.searchMode == SearchMode.BY_LABEL)
			searchItemsList_TaxonIDs_CurrentLevel = ConvertLabelsToTaxonIDs.convert(searchItemsList);
		else if (InputParameters.searchMode == SearchMode.BY_TAXONID)
			searchItemsList_TaxonIDs_CurrentLevel = new ArrayList<String>(searchItemsList);

		FindAllLevelSubclasses.traverse(searchItemsList_TaxonIDs_CurrentLevel, owlClassesResults);

		// Writing ConceptMapper Dictionary
		XMLFileWriter.XMLWrite(InputParameters.XMLFileName, owlClassesResults, unFoundSearchItems);

		// Write the items not found, which could be used with MetaMap
		Files.write(Paths.get(InputParameters.NotFoundItemsFileName), unFoundSearchItems, Charset.defaultCharset());
	}
}
