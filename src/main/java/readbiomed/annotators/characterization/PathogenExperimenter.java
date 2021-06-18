package readbiomed.annotators.characterization;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.util.ViewUriUtil;
import org.xml.sax.SAXException;

import readbiomed.annotators.dictionary.pathogens.PathogenAnnotator;
import readbiomed.annotators.discourse.sdt.SDTAnnotator;
import readbiomed.document.SDTSentence;
import readbiomed.document.Section;
import readbiomed.readers.medline.MedlineReader;

public class PathogenExperimenter {
	public static void main(String[] argc) throws IOException, SAXException, UIMAException {
		String inputFolderName = argc[0];
		String dictionaryFileName = argc[1];
		String SDTPredictionFolderName = argc[2];

		AggregateBuilder pa = PathogenAnnotator.getPipeline(dictionaryFileName);
		pa.add(SentenceAnnotator.getDescription());
		pa.add(SDTAnnotator.getDescription(SDTPredictionFolderName));

		AnalysisEngine ae = AnalysisEngineFactory.createEngine(pa.createAggregateDescription());

		JCas jCas = JCasFactory.createJCas();

		for (File file : new File(inputFolderName).listFiles()) {
			JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
					.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

			while (cr.hasNext()) {
				cr.getNext(jCas);
				ae.process(jCas);

				String pmid = ViewUriUtil.getURI(jCas).toString();

				JCasUtil.select(jCas, Section.class).forEach(e -> {
					if (e.getSectionType().equalsIgnoreCase("title"))
						System.out.println(pmid + "|" + e.getCoveredText() + "|" + e.getSectionType());
					JCasUtil.selectCovered(jCas, NamedEntityMention.class, e).forEach(ne -> System.out.println(ne));
				});

				JCasUtil.select(jCas, SDTSentence.class).forEach(e -> {
					System.out.println(pmid + "|" + e.getCoveredText() + "|" + e.getSdtType());
					JCasUtil.selectCovered(jCas, NamedEntityMention.class, e).forEach(ne -> System.out.println(ne));
				});

				jCas.reset();
			}
		}
	}
}