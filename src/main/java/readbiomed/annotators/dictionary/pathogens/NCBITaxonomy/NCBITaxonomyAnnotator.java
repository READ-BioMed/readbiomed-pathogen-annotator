package readbiomed.annotators.dictionary.pathogens.NCBITaxonomy;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.conceptMapper.DictTerm;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ne.type.NamedEntityMention;

public class NCBITaxonomyAnnotator extends JCasAnnotator_ImplBase {
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		JCasUtil.select(jCas, DictTerm.class).stream().filter(e -> e.getDictCanon().startsWith("ncbi-")).forEach(e -> {
			NamedEntityMention n = new NamedEntityMention(jCas);
			n.setBegin(e.getBegin());
			n.setEnd(e.getEnd());
			n.setMentionId(e.getDictCanon());
			n.setMentionType("pathogen");
			n.addToIndexes(jCas);
		});
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(NCBITaxonomyAnnotator.class);
	}
}