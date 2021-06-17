package readbiomed.annotators.dictionary.utils;

import static org.apache.uima.fit.factory.ExternalResourceFactory.createDependencyAndBind;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.conceptMapper.ConceptMapper;
import org.apache.uima.conceptMapper.support.dictionaryResource.DictionaryResource_impl;
import org.apache.uima.conceptMapper.support.tokenizer.OffsetTokenizer;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

import edu.ucdenver.ccp.nlp.wrapper.conceptmapper.stemmer.ConceptMapperStemmerFactory;

public class ConceptMapperFactory {

	public static AnalysisEngineDescription create(String dictionaryFileName)
			throws InvalidXMLException, IOException, ResourceInitializationException, SAXException {

		AggregateBuilder builder = new AggregateBuilder();

		AnalysisEngineDescription offsetTokenizer = AnalysisEngineFactory.createEngineDescription(OffsetTokenizer.class,
				OffsetTokenizer.PARAM_CASE_MATCH, "ignoreall", OffsetTokenizer.PARAM_TOKEN_DELIM,
				"/-*&@(){}|[]<>\\'`\":;,$%+.?!", OffsetTokenizer.PARAM_STEMMER_CLASS,
				//ConceptMapperStemmerFactory.getStemmerClass(ConceptMapperStemmerFactory.StemmerType.BIOLEMMATIZER));
                ConceptMapperBioLemmatizerPool.class);

		File tmpTokenizerDescription = File.createTempFile("preffix_", "_suffix");
		tmpTokenizerDescription.deleteOnExit();

		try {
			offsetTokenizer.toXML(new FileWriter(tmpTokenizerDescription));
		} catch (SAXException e) {
		}

		AnalysisEngineDescription conceptMapper = AnalysisEngineFactory.createEngineDescription(ConceptMapper.class,
				"TokenizerDescriptorPath", tmpTokenizerDescription.getAbsolutePath(), ConceptMapper.PARAM_FEATURE_LIST,
				new String[] { "DictCanon" }, ConceptMapper.PARAM_ATTRIBUTE_LIST, new String[] { "id" }, "LanguageID",
				"en", ConceptMapper.PARAM_TOKENANNOTATION, "uima.tt.TokenAnnotation",
				ConceptMapper.PARAM_ANNOTATION_NAME, "org.apache.uima.conceptMapper.DictTerm", "SpanFeatureStructure",
				"uima.tcas.DocumentAnnotation", ConceptMapper.PARAM_SEARCHSTRATEGY, "ContiguousMatch",
				ConceptMapper.PARAM_FINDALLMATCHES, false, ConceptMapper.PARAM_ORDERINDEPENDENTLOOKUP, false,
				OffsetTokenizer.PARAM_CASE_MATCH, "ignoreall", OffsetTokenizer.PARAM_TOKEN_DELIM,
				"/-*&@(){}|[]<>\\'`\":;,$%+.?!", OffsetTokenizer.PARAM_STEMMER_CLASS,
				//ConceptMapperStemmerFactory.getStemmerClass(ConceptMapperStemmerFactory.StemmerType.BIOLEMMATIZER));
		        ConceptMapperBioLemmatizerPool.class);

		createDependencyAndBind(conceptMapper, ConceptMapper.PARAM_DICT_FILE, DictionaryResource_impl.class,
				dictionaryFileName);

		builder.add(offsetTokenizer);
		builder.add(conceptMapper);

		return builder.createAggregateDescription();
	}
}
