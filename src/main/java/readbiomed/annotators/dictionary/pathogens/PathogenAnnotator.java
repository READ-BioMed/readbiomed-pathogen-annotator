package readbiomed.annotators.dictionary.pathogens;

import java.io.IOException;

import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

import readbiomed.annotators.dictionary.pathogens.NCBITaxonomy.NCBITaxonomyAnnotator;
import readbiomed.annotators.dictionary.pathogens.PrPSc.PrPScDictionaryAnnotator;
import readbiomed.annotators.dictionary.pathogens.toxins.ToxinRegexAnnotator;
import readbiomed.annotators.dictionary.utils.ConceptMapperFactory;

public class PathogenAnnotator {

	public static AggregateBuilder getPipeline(String dictFileName)
			throws InvalidXMLException, ResourceInitializationException, IOException, SAXException
	{
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(ConceptMapperFactory.create(dictFileName));
		builder.add(NCBITaxonomyAnnotator.getDescription());
		builder.add(PrPScDictionaryAnnotator.getDescription());
		builder.add(ToxinRegexAnnotator.getDescription());

		return builder;
	}
}