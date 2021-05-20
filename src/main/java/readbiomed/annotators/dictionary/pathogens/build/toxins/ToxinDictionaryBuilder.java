package readbiomed.annotators.dictionary.pathogens.build.toxins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import readbiomed.bmip.dataset.toxins.ToxinDocuments;

public class ToxinDictionaryBuilder {
	public static void dictionaryBuilder(PrintWriter p) {
		for (String[] toxin : ToxinDocuments.toxins) {
			p.print("<token ");
			p.print("id=\"toxin-" + toxin[0] + "\" ");
			p.print("canonical=\"" + toxin[0] + "\"");
			p.println(">");
			p.print("    <variant ");
			p.print("base=\"" + toxin[0] + "\"");
			p.println("/>");
			p.println("</token>");
		}
	}

	public static void main(String[] argc) throws IOException {
		String folderName = argc[0];

		try (PrintWriter p = new PrintWriter(new FileWriter(new File(folderName, "toxin-dict.xml")))) {
			p.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			p.println("<synonym>");

			dictionaryBuilder(p);

			p.println("</synonym>");
		}
	}
}