package readbiomed.annotators.dictionary.pathogens.build.toxins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import readbiomed.bmip.dataset.toxins.ToxinDocuments;

public class ToxinDictionaryBuilder {
	public static void dictionaryBuilder(PrintWriter p) {
		for (String[] toxin : ToxinDocuments.toxins) {
			p.println("<token id=\"toxin-" + toxin[1].toLowerCase() + "\" canonical=\"" + toxin[0] + "\">");
			p.println("    <variant base=\"" + toxin[0] + "\"/>");

			if (!toxin[0].equals(toxin[1])) {
				p.println("    <variant base=\"" + toxin[1].toLowerCase() + "\"/>");
			}

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