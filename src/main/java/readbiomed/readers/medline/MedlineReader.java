package readbiomed.readers.medline;

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

public class MedlineReader extends JCasCollectionReader_ImplBase {
	public static final String PARAM_FILE_NAME = "fileName";
	public Stack<MedlineCitation> documents = new Stack<>();

	public String fileName = null;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		fileName = (String) context.getConfigParameterValue(PARAM_FILE_NAME);
		try {
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			SAXParser saxParser = saxParserFactory.newSAXParser();
			MedlineSaxParser handler = new MedlineSaxParser();
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
			MedlineCitation d = documents.pop();

			Section title = new Section(jCas);
			title.setSectionType("title");
			title.setBegin(0);
			title.setEnd(d.getArticleTitle().length());
			title.addToIndexes(jCas);

			if (d.getAbstractText().length() > 0) {
				jCas.setDocumentText(d.getArticleTitle() + " " + d.getAbstractText());

				Section abstractText = new Section(jCas);
				abstractText.setSectionType("abstract");
				abstractText.setBegin(title.getEnd() + 1);
				abstractText.setEnd(abstractText.getBegin() + d.getAbstractText().length());
				abstractText.addToIndexes(jCas);
			} else {
				jCas.setDocumentText(d.getArticleTitle());
			}

			try {
				ViewUriUtil.setURI(jCas, new URI(d.getPMID()));
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static CollectionReaderDescription getDescriptionFromFiles(String fileName)
			throws ResourceInitializationException {
		return CollectionReaderFactory.createReaderDescription(MedlineReader.class, null, PARAM_FILE_NAME, fileName);
	}
}