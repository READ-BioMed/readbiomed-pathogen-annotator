package readbiomed.annotators.dictionary.pathogens.build.PrPSc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import readbiomed.bmip.dataset.PrPSc.PrPScDocuments;

public class PrPScDictionaryBuilder {
	public static void dictionaryBuilder(PrintWriter p) {
		p.println("<token id=\"prpsc-prion\" canonical=\"prion\">");
		p.println("    <variant base=\"prion\"/>");
		p.println("</token>");
		
		p.println("<token id=\"prpsc-prpsc\" canonical=\"PrPSc\">");
		p.println("    <variant base=\"PrPSc\"/>");
		p.println("</token>");
		
		for (String[] s : PrPScDocuments.species) {
			p.print("<token ");
			p.print("id=\"prpsc-" + s[0] + "\" ");
			p.print("canonical=\"" + s[0] + "\"");
			p.println(">");
			p.print("    <variant ");
			p.print("base=\"" + s[0] + "\"");
			p.println("/>");
			p.println("</token>");
		}
	}

	public static void main(String[] argc) throws IOException {
		String folderName = argc[0];

		try (PrintWriter p = new PrintWriter(new FileWriter(new File(folderName, "prpsc-dict.xml")))) {
			p.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			p.println("<synonym>");

			dictionaryBuilder(p);

			p.println("</synonym>");
		}
	}
}