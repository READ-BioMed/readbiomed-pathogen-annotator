package readbiomed.annotators.characterization;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ViewUriUtil;

import readbiomed.bmip.dataset.NCBITaxonomy.BuildDataset;
import readbiomed.bmip.dataset.PrPSc.PrPScBuildDataset;
import readbiomed.bmip.dataset.toxins.ToxinBuildDataset;
import readbiomed.bmip.dataset.utils.Utils;
import readbiomed.document.Section;
import readbiomed.readers.medline.MedlineReader;

public class RandomPMIDSet {
	public static void main(String[] argc) throws JAXBException, IOException, InterruptedException, UIMAException {
		String ncbiFolderName = "/home/antonio/Documents/git/readbiomed-bmip-datasets/ncbi/ncbi-pathogens-data";
		String PrPScFolderName = "/home/antonio/Documents/git/readbiomed-bmip-datasets/ncbi/prpsc-ncbi-data";
		String toxinsFolderName = "/home/antonio/Documents/git/readbiomed-bmip-datasets/ncbi/toxin-ncbi-data";

		String outputFolderName = "/home/antonio/Documents/UoM/manual-annotation";

		Set<String> pmids = BuildDataset.readPathogenEntries(ncbiFolderName).entrySet().stream().map(e -> e.getValue())
				.flatMap(Collection::stream).collect(Collectors.toSet());

		pmids.addAll(PrPScBuildDataset.readPrPScEntries(PrPScFolderName).entrySet().stream().map(e -> e.getValue())
				.flatMap(Collection::stream).collect(Collectors.toSet()));

		pmids.addAll(ToxinBuildDataset.readToxinEntries(toxinsFolderName).entrySet().stream().map(e -> e.getValue())
				.flatMap(Collection::stream).collect(Collectors.toSet()));

		System.out.println(pmids.size());

		// Recover 1000 documents randomly
		Utils.recoverPubMedCitations(pmids.stream().limit(1000).collect(Collectors.toList()), outputFolderName);

		// Turn to text for manual annotation using Excel
		try (PrintWriter w = new PrintWriter(new FileWriter(new File(outputFolderName, "output.pipe")))) {
			for (File file : new File(outputFolderName, "PubMed").listFiles()) {
				if (file.getName().endsWith(".gz")) {
					System.out.println(file.getName());
					
					JCas jCas = JCasFactory.createJCas();

					JCasCollectionReader_ImplBase cr = (JCasCollectionReader_ImplBase) org.apache.uima.fit.factory.CollectionReaderFactory
							.createReader(MedlineReader.getDescriptionFromFiles(file.getAbsolutePath()));

					while (cr.hasNext()) {
						cr.getNext(jCas);

						String pmid = ViewUriUtil.getURI(jCas).toString();

						Map<String, String> map = JCasUtil.select(jCas, Section.class).stream()
								.collect(Collectors.toMap(Section::getSectionType, Section::getCoveredText));

						w.println(pmid + "|" + map.get("title") + "|" + map.get("abstract"));

						jCas.reset();
					}
				}
			}
		}
	}
}