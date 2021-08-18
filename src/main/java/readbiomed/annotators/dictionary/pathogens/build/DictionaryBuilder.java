package readbiomed.annotators.dictionary.pathogens.build;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.bmip.dataset.PrPSc.PrPScDocuments;

@Command(name = "DictionaryBuilder", mixinStandardHelpOptions = true, version = "DictionaryBuilder 0.1", description = "Build pathogen dictionary based on the NCBI taxonomy data set.")
public class DictionaryBuilder extends DefaultHandler implements Callable<Integer> {

	private enum Tag {
		Class, Axiom
	}

	private enum TermTag {
		Canonical, Variant
	}

	private Tag tag = null;
	private TermTag termTag = null;
	private String currentId = null;
	private StringBuilder currentTerm = new StringBuilder();

	private Map<String, NCBIEntry> map = new HashMap<>();

	List<String> searchItemsList = null;

	public DictionaryBuilder() throws IOException {
	}

	private static String removeOBOURL(String string) {
		return string.substring(41);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("owl:Class")) {
			tag = Tag.Class;
			currentId = removeOBOURL(attributes.getValue("rdf:about"));
			map.put(currentId, new NCBIEntry(currentId));
		} else if (tag == Tag.Class && qName.equalsIgnoreCase("rdfs:subClassOf")) {
			map.get(currentId).getParents().add(removeOBOURL(attributes.getValue("rdf:resource")));
		} else if (tag == Tag.Class && qName.equalsIgnoreCase("rdfs:label")) {
			termTag = TermTag.Canonical;
		} else if (qName.equalsIgnoreCase("owl:Axiom")) {
			tag = Tag.Axiom;
		} else if (tag == Tag.Axiom && qName.equalsIgnoreCase("owl:annotatedSource")) {
			currentId = removeOBOURL(attributes.getValue("rdf:resource"));
		} else if (tag == Tag.Axiom && qName.equalsIgnoreCase("owl:annotatedTarget")) {
			termTag = TermTag.Variant;
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if (termTag != null) {
			currentTerm.append(new String(ch, start, length));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("owl:Class") || qName.equalsIgnoreCase("owl:Axiom")) {
			tag = null;
			currentId = null;
		} else if (termTag == TermTag.Canonical && qName.equalsIgnoreCase("rdfs:label")) {
			map.get(currentId).setCanonical(currentTerm.toString());
			termTag = null;
			currentTerm.setLength(0);
		} else if (termTag == TermTag.Variant && qName.equalsIgnoreCase("owl:annotatedTarget")) {
			map.get(currentId).getSynonyms().add(currentTerm.toString());
			termTag = null;
			currentTerm.setLength(0);
		}
	}

	private void buildTree() {
		map.entrySet().stream().forEach(e -> {
			e.getValue().getParents().stream().forEach(c -> {
				if (map.get(c) != null) {
					map.get(c).getChildren().add(e.getKey());
				}
			});
		});
	}

	private void printTerm(String term, XMLStreamWriter xmlWriter) throws XMLStreamException {
		if (term.length() > 3 && !term.equalsIgnoreCase("unidentified subtype")
				&& !term.toLowerCase().startsWith("influenza a virus (")
				&& !term.toLowerCase().startsWith("influenza b virus (")
				&& !term.toLowerCase().startsWith("influenza c virus (")) {
			xmlWriter.writeStartElement("variant");
			xmlWriter.writeAttribute("base", term);
			xmlWriter.writeEndElement();

			// Remove subtype to identify specific pathogen names
			if (term.contains("subtype")) {
				if (term.replaceAll("subtype", "").trim().length() > 3) {
					xmlWriter.writeStartElement("variant");
					xmlWriter.writeAttribute("base", term.replaceAll("subtype", "").trim());
					xmlWriter.writeEndElement();
				}
			}

			if (term.endsWith(" virus")) {
				if (term.replaceAll(" virus$", "").trim().length() > 3) {
					xmlWriter.writeStartElement("variant");
					xmlWriter.writeAttribute("base", term.replaceAll(" virus$", "").trim());
					xmlWriter.writeEndElement();
				}
			}
		}
	}

	private void writeDictionaryChildren(NCBIEntry ncbiEntry, XMLStreamWriter xmlWriter) throws XMLStreamException {
		printTerm(ncbiEntry.getCanonical(), xmlWriter);

		for (String term : ncbiEntry.getSynonyms()) {
			printTerm(term, xmlWriter);
		}

		for (String child : ncbiEntry.getChildren()) {
			writeDictionaryChildren(map.get(child), xmlWriter);
		}
	}

	private void writeDictionary(String outputFileName, String termFileName) throws IOException, XMLStreamException {
		Set<String> searchItemsList = Files.readAllLines(Paths.get(termFileName)).stream().filter(e -> e.length() > 0)
				.map(e -> e.toLowerCase()).collect(Collectors.toSet());

		try (FileWriter w = new FileWriter(outputFileName)) {
			XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter xmlWriter = new IndentingXMLStreamWriter(xMLOutputFactory.createXMLStreamWriter(w));

			xmlWriter.writeStartDocument();
			xmlWriter.writeStartElement("synonym");

			// NCBI dictionary
			for (Map.Entry<String, NCBIEntry> entry : map.entrySet()) {
				if (searchItemsList.contains(entry.getValue().getCanonical().toLowerCase()) || entry.getValue()
						.getSynonyms().parallelStream().anyMatch(e -> searchItemsList.contains(e.toLowerCase()))) {
					xmlWriter.writeStartElement("token");
					xmlWriter.writeAttribute("id", "ncbi-" + entry.getValue().getId());
					xmlWriter.writeAttribute("canonical", entry.getValue().getCanonical());

					writeDictionaryChildren(entry.getValue(), xmlWriter);

					xmlWriter.writeEndElement();
				}
			}

			// PrPSc dictionary
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

	@Parameters(index = "0", description = "NCBI taxonomy file name.", defaultValue = "/Users/ajimeno/Documents/UoM/ncbitaxon.owl.gz")
	private String NCBITaxonomyFileName;
	@Parameters(index = "1", description = "Output dictionary file name.", defaultValue = "/Users/ajimeno/Documents/UoM/dict.xml")
	private String outputDictionaryFileName;
	@Parameters(index = "2", description = "NCBI pathogen file name.", defaultValue = "/Users/ajimeno/Documents/UoM/ncbi-pathogens.txt")
	private String ncbiPathogenFileName;

	@Override
	public Integer call() throws Exception {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser saxParser = saxParserFactory.newSAXParser();
		DictionaryBuilder handler = new DictionaryBuilder();
		XMLReader reader = saxParser.getXMLReader();
		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		saxParser.parse(new GZIPInputStream(new FileInputStream(NCBITaxonomyFileName), 65536), handler);

		handler.buildTree();

		handler.writeDictionary(outputDictionaryFileName, ncbiPathogenFileName);

		System.out.println(handler.map.size());
		return 0;
	}

	public static void main(String[] argc) throws IOException {
		int exitCode = new CommandLine(new DictionaryBuilder()).execute(argc);
		System.exit(exitCode);
	}
}