package readbiomed.readers.medline;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MedlineSaxParser extends DefaultHandler {
	private enum Tag {
		PMID, ArticleTitle, AbstractText, NoTag
	}

	private Tag tag = null;

	private Stack<MedlineCitation> docs = new Stack<>();

	private StringBuilder pmid = new StringBuilder();
	private StringBuilder articleTitle = new StringBuilder();
	private StringBuilder abstractText = new StringBuilder();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("PMID")) {
			if (pmid.length() == 0) {
				tag = Tag.PMID;
			}
		} else if (qName.equalsIgnoreCase("ArticleTitle")) {
			tag = Tag.ArticleTitle;
		} else if (qName.equalsIgnoreCase("AbstractText")) {
			tag = Tag.AbstractText;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("MedlineCitation")) {
			// Generate document
			docs.push(new MedlineCitation(pmid.toString(), articleTitle.toString().trim(),
					abstractText.toString().trim()));

			pmid.setLength(0);
			articleTitle.setLength(0);
			abstractText.setLength(0);
		} else if (qName.equalsIgnoreCase("PMID") || qName.equalsIgnoreCase("ArticleTitle")
				|| qName.equalsIgnoreCase("AbstractText")) { // Clear tag
			tag = null;
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if (tag != null) {
			switch (tag) {
			case PMID:
				pmid.append(new String(ch, start, length));
				break;
			case ArticleTitle:
				articleTitle.append(new String(ch, start, length)).append(" ");
				break;
			case AbstractText:
				abstractText.append(new String(ch, start, length)).append(" ");
				break;
			default:
				break;
			}
		}
	}

	public Stack<MedlineCitation> getDocuments() {
		return docs;
	}
}