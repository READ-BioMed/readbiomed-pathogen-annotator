package readbiomed.annotators.dictionary.pathogens.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

public class Test {

	public static void main(String[] args) throws IOException {

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

					// Checking whether the owl class is subclass of our search item or not

					String stringToSearch_class = StringUtils.substringBetween(owlClassString,
							"<rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">", "</rdfs:label>");
					// check cloned_searchItemsList null or not
					if (stringToSearch_class.equals("Silvanigrella sp.")) {
						System.out.println(StringUtils.substringBetween(owlClassString, "rdf:about=\"", "\""));
					}
					// Owl class is not a subclass
				}

			}
		}
	}

}
