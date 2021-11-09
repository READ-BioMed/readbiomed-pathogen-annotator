package readbiomed.readers.pmc;

import java.util.LinkedList;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PMCSaxParser extends DefaultHandler {
	private enum Tag {
		PMCID, ArticleTitle, AbstractText, NoTag, SecTitle, SecText
	}

	private Tag tag = null;

	private boolean articleMeta = false;
	private boolean body = false;
	private int secCount = 0;
	private static PMCSection currentSection = new PMCSection();

	private Stack<PMCArticle> docs = new Stack<>();

	private StringBuilder pmid = new StringBuilder();
	private StringBuilder articleTitle = new StringBuilder();
	private StringBuilder abstractText = new StringBuilder();
	private LinkedList<PMCSection> sections = new LinkedList<>();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("article-id") && attributes.getValue("pub-id-type") != null
				&& attributes.getValue("pub-id-type").equals("pmid")) {
			if (pmid.length() == 0) {
				tag = Tag.PMCID;
			}
		} else if (articleMeta && qName.equalsIgnoreCase("article-title")) {
			tag = Tag.ArticleTitle;
		} else if (articleMeta && qName.equalsIgnoreCase("abstract") && abstractText.length() == 0) {
			tag = Tag.AbstractText;
		} else if (qName.equalsIgnoreCase("article-meta")) {
			articleMeta = true;
		} else if (qName.equalsIgnoreCase("body")) {
			body = true;
		} else if (body && qName.equalsIgnoreCase("sec")) {
			secCount++;
		} else if (body && secCount == 1 && qName.equalsIgnoreCase("title") && currentSection.getTitle().length() == 0) {
			tag = Tag.SecTitle;
		} else if (body && secCount > 0 && qName.equalsIgnoreCase("p")) {
			tag = Tag.SecText;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("article")) {
			// Generate document
			docs.push(new PMCArticle(pmid.toString(),
					articleTitle.toString().replaceAll("\n", " ").replaceAll("  *", " ").trim(),
					abstractText.toString().replaceAll("\n", " ").replaceAll("  *", " ").trim(), sections));

			pmid.setLength(0);
			articleTitle.setLength(0);
			abstractText.setLength(0);
			sections = new LinkedList<>();
		} else if (qName.equalsIgnoreCase("article-id") || qName.equalsIgnoreCase("article-title")
				|| qName.equalsIgnoreCase("abstract") || qName.equalsIgnoreCase("title")
				|| qName.equalsIgnoreCase("p")) { // Clear tag
			tag = null;
		} else if (qName.equalsIgnoreCase("article-meta")) {
			articleMeta = false;
		} else if (qName.equalsIgnoreCase("body")) {
			body = false;
		} else if (body && qName.equalsIgnoreCase("sec")) {
			secCount--;

			if (secCount == 0) {
				// Many articles have the text for the introduction section out of any section
				if (sections.size() == 0 && currentSection.getTitle().length() == 0) {
					currentSection.setTitle("Introduction");
				}

				currentSection.setTitle(currentSection.getTitle().replaceAll("\n", " ").replaceAll("  *", " ").trim());
				currentSection.setText(currentSection.getText().replaceAll("\n", " ").replaceAll("  *", " ").trim());
				
				sections.add(currentSection);
				currentSection = new PMCSection();
			}
		}
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if (tag != null) {
			switch (tag) {
			case PMCID:
				pmid.append(new String(ch, start, length));
				break;
			case ArticleTitle:
				articleTitle.append(new String(ch, start, length)).append(" ");
				break;
			case AbstractText:
				abstractText.append(new String(ch, start, length)).append(" ");
				break;
			case SecTitle:
				currentSection.setTitle(currentSection.getTitle() + new String(ch, start, length) + " ");
				break;
			case SecText:
				currentSection.setText(currentSection.getText() + new String(ch, start, length) + " ");
				break;
			default:
				break;
			}
		}
	}

	public Stack<PMCArticle> getDocuments() {
		return docs;
	}
}