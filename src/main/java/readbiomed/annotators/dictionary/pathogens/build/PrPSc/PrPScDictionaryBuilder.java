package readbiomed.annotators.dictionary.pathogens.build.PrPSc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import readbiomed.bmip.dataset.PrPSc.PrPScDocuments;

public class PrPScDictionaryBuilder {
	public static void main(String[] argc) throws IOException, XMLStreamException {
		String folderName = argc[0];

		try (FileWriter w = new FileWriter(new File(folderName, "prpsc-dict.xml"));) {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = new IndentingXMLStreamWriter(xMLOutputFactory.createXMLStreamWriter(w));

			xmlWriter.writeStartDocument();
			xmlWriter.writeStartElement("synonym");

			xmlWriter.writeStartElement("token");
			xmlWriter.writeAttribute("id", "prpsc-prion");
			xmlWriter.writeAttribute("canonical", "prion");
			xmlWriter.writeStartElement("variant");
			xmlWriter.writeAttribute("base", "prion");
			xmlWriter.writeEndElement();
			xmlWriter.writeEndElement();

			xmlWriter.writeStartElement("token");
			xmlWriter.writeAttribute("id", "prpsc-prpsc");
			xmlWriter.writeAttribute("canonical", "PrPSc");
			xmlWriter.writeStartElement("variant");
			xmlWriter.writeAttribute("base", "PrPSc");
			xmlWriter.writeEndElement();
			xmlWriter.writeEndElement();

			for (String[] s : PrPScDocuments.species) {
				xmlWriter.writeStartElement("token");
				xmlWriter.writeAttribute("id", "prpsc-" + s[0]);
				xmlWriter.writeAttribute("canonical", s[0]);
				xmlWriter.writeStartElement("variant");
				xmlWriter.writeAttribute("base", s[0]);
				xmlWriter.writeEndElement();
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