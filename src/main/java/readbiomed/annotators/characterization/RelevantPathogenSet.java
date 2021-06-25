package readbiomed.annotators.characterization;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.util.ViewUriUtil;
import org.xml.sax.SAXException;

import readbiomed.annotators.dictionary.pathogens.PathogenDictionaryAnnotator;
import readbiomed.bmip.dataset.NCBITaxonomy.BuildDataset;
import readbiomed.readers.medline.MedlineReader;

/**
 * 
 * Prepare collection of documents in which a pathogen but it is present or not
 * in MeSH annotation, which defines its class. Mentions of pathogens are
 * changed to @PATHOGEN$
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class RelevantPathogenSet {
	class SortNamedEntityMentions implements Comparator<NamedEntityMention> {
		public int compare(NamedEntityMention a, NamedEntityMention b) {
			return b.getBegin() - a.getBegin();
		}
	}

	public static void main(String[] argc) throws IOException, SAXException, UIMAException, JAXBException {
		String inputFolderName = argc[0];
		String dictionaryFileName = argc[1];

		Map<String, Set<String>> mapNCBI = BuildDataset.readPathogenEntries(inputFolderName);

		AggregateBuilder pa = PathogenDictionaryAnnotator.getPipeline(dictionaryFileName);
		AnalysisEngine ae = AnalysisEngineFactory.createEngine(pa.createAggregateDescription());

		JCas jCas = JCasFactory.createJCas();

		try (PrintWriter w = new PrintWriter(new FileWriter(new File(inputFolderName, "dataset.pipe")))) {
			for (File file : new File(inputFolderName, "PubMed").listFiles()) {
				if (file.getName().endsWith(".gz")) {
					JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
							.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

					while (cr.hasNext()) {
						cr.getNext(jCas);
						ae.process(jCas);

						String pmid = ViewUriUtil.getURI(jCas).toString();

						List<NamedEntityMention> list = new ArrayList<>();
						Set<String> ids = new HashSet<>();

						for (NamedEntityMention ne : JCasUtil.select(jCas, NamedEntityMention.class)) {
							ids.add(ne.getMentionId());
							list.add(ne);
						}

						Set<NamedEntityMention> removal = new HashSet<>();

						// Remove potential overlapping mentions of the same pathogen
						for (NamedEntityMention ne : list) {
							for (NamedEntityMention neIn : list) {
								if (ne != neIn) {
									if (ne.getMentionId().equals(neIn.getMentionId())) {
										if (ne.getBegin() == neIn.getBegin() || ne.getEnd() == neIn.getEnd()) {
											if (!(removal.contains(ne) || removal.contains(neIn))) {
												removal.add(ne);
											}
										}
									}
								}

							}
						}

						for (NamedEntityMention ne : removal) {
							list.remove(ne);
						}

						Collections.sort(list, new RelevantPathogenSet().new SortNamedEntityMentions());

						for (String id : ids) {
							Set<String> pmidsNCBI = mapNCBI.get(id);

							// The pathogen should exist in our set
							if (pmidsNCBI != null) {
								// Get new text string

								String text = jCas.getDocumentText();

								for (NamedEntityMention ne : list) {
									if (ne.getMentionId().contentEquals(id)) {
										if (ne.getMentionId().equals(id)) {
											text = text.substring(0, ne.getBegin()) + "@PATHOGEN$"
													+ text.substring(ne.getEnd());
										}
									}
								}

								// Determine class based on presence in pmidsNCBI
								String category = (pmidsNCBI.contains(pmid) ? "Y" : "N");

								// Generate example
								w.println(pmid + "|" + text.replaceAll("\\|", " ").replaceAll("\n", " ").trim() + "|"
										+ category);
							}
						}

						jCas.reset();
					}
				}
			}
		}
	}
}