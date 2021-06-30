package readbiomed.annotators.characterization;

import java.io.IOException;

import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

import readbiomed.annotators.dictionary.pathogens.PathogenDictionaryAnnotator;

public class PathogenCharacterizationAnnotator {
	public static AggregateBuilder getPipeline(String dictFileName)
			throws InvalidXMLException, ResourceInitializationException, IOException, SAXException {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(PathogenDictionaryAnnotator.getPipeline(dictFileName).createAggregateDescription());

		/*
		 * builder.add(BMIPPathogenNotRelevantAnnotator.getDescription(
		 * "/home/antonio/Downloads/mti-ml/MTI_ML/trie.gz",
		 * "/home/antonio/Downloads/mti-ml/MTI_ML/classifiers.gz",
		 * "gov.nih.nlm.nls.mti.featuresextractors.BinaryFeatureExtractor",
		 * "-l -n -c"));
		 */
		builder.add(BMIPDocumentNotRelevantAnnotator.getDescription("/Users/ajimeno/Documents/MTI_ML/trie.excel.gz",
				"/Users/ajimeno/Documents/MTI_ML/classifiers.excel.gz",
				"gov.nih.nlm.nls.mti.featuresextractors.BinaryFeatureExtractor", "-l -n -c"));

		return builder;
	}
}