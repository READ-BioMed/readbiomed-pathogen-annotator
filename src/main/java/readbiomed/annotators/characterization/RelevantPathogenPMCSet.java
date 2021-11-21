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
import java.util.concurrent.Callable;

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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import readbiomed.annotators.dictionary.pathogens.PathogenDictionaryAnnotator;
import readbiomed.document.Section;
import readbiomed.pathogens.dataset.NCBITaxonomy.BuildDataset;
import readbiomed.readers.pmc.PMCReader;

/**
 * 
 * Prepare collection of documents in which a pathogen when it is present or not
 * in MeSH annotation, defines its class. Mentions of pathogens are changed
 * to @PATHOGEN$
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
@Command(name = "RelevantPathogenPMCSet", mixinStandardHelpOptions = true, version = "RelevantPathogenPMCSet 0.1", description = "Generate relevant pathogen PMC set.")
public class RelevantPathogenPMCSet implements Callable<Integer> {
	class SortNamedEntityMentions implements Comparator<NamedEntityMention> {
		public int compare(NamedEntityMention a, NamedEntityMention b) {
			return b.getBegin() - a.getBegin();
		}
	}

	@Parameters(index = "0", description = "Input folder name.")
	private String inputFolderName;
	@Parameters(index = "1", description = "Pathogen folder name.")
	private String pathogenFolderName;
	@Parameters(index = "2", description = "Dictionary file name.")
	private String dictionaryFileName;

	@Override
	public Integer call() throws Exception {
		Map<String, Set<String>> mapNCBI = BuildDataset.readPathogenEntries(pathogenFolderName);

		AggregateBuilder pa = PathogenDictionaryAnnotator.getPipeline(dictionaryFileName);
		AnalysisEngine ae = AnalysisEngineFactory.createEngine(pa.createAggregateDescription());

		JCas jCas = JCasFactory.createJCas();

		try (PrintWriter w = new PrintWriter(new FileWriter(new File(inputFolderName, "dataset.pmc.pipe")))) {
			for (File file : new File(inputFolderName, "PMC").listFiles()) {
				if (file.getName().endsWith(".gz")) {
					System.err.println(file.getName());
					JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
							.createReader(PMCReader.getDescriptionFromFiles(file.getAbsolutePath()));

					while (cr.hasNext()) {
						cr.getNext(jCas);
						ae.process(jCas);

						String pmid = ViewUriUtil.getURI(jCas).toString();

						Set<String> ids = new HashSet<>();

						List<NamedEntityMention> list = new ArrayList<>();

						// Perform analysis per section
						for (Section section : JCasUtil.select(jCas, Section.class)) {

							for (NamedEntityMention ne : JCasUtil.selectCovered(jCas, NamedEntityMention.class,
									section)) {
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
						}

						ids.stream().filter(id -> id.startsWith("ncbi-")).forEach(id -> {
							Set<String> pmidsNCBI = mapNCBI.get(id);

							// The pathogen should exist in our set
							if (pmidsNCBI != null) {
								// Determine class based on presence in pmidsNCBI
								String category = (pmidsNCBI.contains(pmid) ? "Y" : "N");

								// Get new text string
								for (Section section : JCasUtil.select(jCas, Section.class)) {
									String text = section.getCoveredText();

									for (NamedEntityMention ne : list) {
										// If entity in section
										if (ne.getBegin() >= section.getBegin() && ne.getEnd() <= section.getEnd()) {
											if (ne.getMentionId().contentEquals(id)) {
												if (ne.getMentionId().equals(id)) {
													int begin = ne.getBegin() - section.getBegin();
													int end = ne.getEnd() - section.getBegin();

													text = text.substring(0, begin) + "@PATHOGEN$"
															+ text.substring(end);
												}
											}
										}
									}

									// Generate example
									w.println(pmid + "|" + section.getSectionType() + "|" + id + "|"
											+ text.replaceAll("\\|", " ").replaceAll("\n", " ").trim() + "|"
											+ category);
								}
							}
						});

						jCas.reset();
					}
				}
			}
		}
		return 0;
	}

	public static void main(String[] argc) throws IOException, SAXException, UIMAException, JAXBException {
		int exitCode = new CommandLine(new RelevantPathogenPMCSet()).execute(argc);
		System.exit(exitCode);
	}
}