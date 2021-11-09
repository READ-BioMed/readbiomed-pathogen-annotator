package readbiomed.readers.pmc;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.cleartk.util.ViewUriUtil;
import org.xml.sax.XMLReader;

import readbiomed.document.Section;

public class PMCReader extends JCasCollectionReader_ImplBase {
	public static final String PARAM_FILE_NAME = "fileName";
	public Stack<PMCArticle> documents = new Stack<>();

	public String fileName = null;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		fileName = (String) context.getConfigParameterValue(PARAM_FILE_NAME);
		try {
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			SAXParser saxParser = saxParserFactory.newSAXParser();
			PMCSaxParser handler = new PMCSaxParser();
			XMLReader reader = saxParser.getXMLReader();
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			saxParser.parse(new GZIPInputStream(new FileInputStream(fileName), 65536), handler);

			documents = handler.getDocuments();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return !documents.empty();
	}

	@Override
	public Progress[] getProgress() {
		return null;
	}

	public void getNext(JCas jCas) throws IOException, CollectionException {
		if (this.hasNext()) {
			PMCArticle d = documents.pop();

			StringBuilder sb = new StringBuilder();

			Section title = new Section(jCas);
			title.setSectionType("title");
			title.setBegin(0);
			title.setEnd(d.getArticleTitle().length());
			title.addToIndexes(jCas);

			sb.append(d.getArticleTitle());

			if (d.getAbstractText().length() > 0) {
				Section abstractText = new Section(jCas);
				abstractText.setSectionType("abstract");
				abstractText.setBegin(sb.length() + 1);
				abstractText.setEnd(abstractText.getBegin() + d.getAbstractText().length());
				abstractText.addToIndexes(jCas);

				sb.append(" ").append(d.getAbstractText());
			}

			for (PMCSection s : d.getSections()) {
				Section section = new Section(jCas);
				section.setSectionType(s.getTitle());
				section.setBegin(sb.length() + 1);
				section.setEnd(section.getBegin() + s.getText().length());
				section.addToIndexes(jCas);

				sb.append(" ").append(s.getText());
			}

			jCas.setDocumentText(sb.toString());

			try {
				ViewUriUtil.setURI(jCas, new URI(d.getPMID()));
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static CollectionReaderDescription getDescriptionFromFiles(String fileName)
			throws ResourceInitializationException {
		return CollectionReaderFactory.createReaderDescription(PMCReader.class, null, PARAM_FILE_NAME, fileName);
	}
}