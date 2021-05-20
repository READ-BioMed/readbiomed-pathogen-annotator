package readbiomed.annotators.dictionary.pathogens.build.NCBITaxonomy;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class XMLFileWriter {

	private static final String staticTaxonID_Pref = "http://purl.obolibrary.org/obo/NCBITaxon_D";

	public static void XMLWrite(String xmlFileName, List<OwlClass> owlClassesResults, List<String> unFoundSearchItems)
			throws IOException {
		int staticTaxonID_num = 0;

		try (PrintWriter p = new PrintWriter(new FileWriter(xmlFileName))) {
			p.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			p.println("<synonym>");

			for (OwlClass o : owlClassesResults) {
				String ID = o.getId();
				String canonical = o.getCanonical();
				List<String> variants = o.getVariants();

				p.print("<token ");
				p.print("id= " + "\"" + ID + "\"" + " ");
				p.print("canonical= " + "\"" + canonical + "\"");
				p.println(">");

				for (int i = 0; i < variants.size(); i++) {
					p.print("    <variant ");
					p.print("base= " + "\"" + variants.get(i) + "\"");
					p.println("/>");
				}

				// Add Canonical as a Variant if no Variant exist
				if (!variants.contains(canonical)) {
					p.print("    <variant ");
					p.print("base= " + "\"" + canonical + "\"");
					p.println("/>");
				}

				// Improve Accuracy of Dictionary
				String canonicalPlus = canonical.replace("_", " ");
				if (canonical.contains("_") && !variants.contains(canonicalPlus)) {
					p.print("    <variant ");
					p.print("base= " + "\"" + canonicalPlus + "\"");
					p.println("/>");
				}

				p.println("</token>");
			}

			for (String canonical : unFoundSearchItems) {
				staticTaxonID_num++;

				p.print("<token ");
				p.print("id= " + "\"" + staticTaxonID_Pref + staticTaxonID_num + "\"" + " ");
				p.print("canonical= " + "\"" + canonical + "\"");
				p.println(">");
				// Add Canonical as a Variant if no Variant exist
				p.print("    <variant ");
				p.print("base= " + "\"" + canonical + "\"");
				p.println("/>");
				p.println("</token>");
			}

			p.print("</synonym>");
		}
	}

}
