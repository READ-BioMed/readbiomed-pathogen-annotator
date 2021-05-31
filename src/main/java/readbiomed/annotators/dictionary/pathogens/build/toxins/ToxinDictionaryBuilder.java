package readbiomed.annotators.dictionary.pathogens.build.toxins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import readbiomed.bmip.dataset.toxins.ToxinDocuments;

public class ToxinDictionaryBuilder {

	public static void main(String[] argc) throws IOException, XMLStreamException {
		String folderName = argc[0];

		try (FileWriter w = new FileWriter(new File(folderName, "toxin-dict.xml"))) {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = new IndentingXMLStreamWriter(xMLOutputFactory.createXMLStreamWriter(w));

			xmlWriter.writeStartDocument();
			xmlWriter.writeStartElement("synonym");

			for (String[] toxin : ToxinDocuments.toxins) {
				xmlWriter.writeStartElement("token");
				xmlWriter.writeAttribute("id", "toxin-" + toxin[1].toLowerCase());
				xmlWriter.writeAttribute("canonical", toxin[0]);
				xmlWriter.writeStartElement("variant");
				xmlWriter.writeAttribute("base", toxin[0]);
				xmlWriter.writeEndElement();

				if (!toxin[0].equalsIgnoreCase(toxin[1])) {
					xmlWriter.writeStartElement("variant");
					xmlWriter.writeAttribute("base", toxin[1].toLowerCase());
					xmlWriter.writeEndElement();
				}

				xmlWriter.writeEndElement();
			}

			// End Synonym
			xmlWriter.writeEndElement();
			xmlWriter.writeEndDocument();
			xmlWriter.flush();
			xmlWriter.close();
		}
	}
}