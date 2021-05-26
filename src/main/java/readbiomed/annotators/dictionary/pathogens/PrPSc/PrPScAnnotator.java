package readbiomed.annotators.dictionary.pathogens.PrPSc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.conceptMapper.DictTerm;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
import org.cleartk.ne.type.NamedEntityMention;
import org.cleartk.token.type.Sentence;
import org.cleartk.util.ViewUriUtil;

import readbiomed.annotators.dictionary.pathogens.PathogenAnnotator;
import readbiomed.annotators.dictionary.utils.Serialization;
import readbiomed.document.Section;
import uima.tt.TokenAnnotation;

public class PrPScAnnotator extends CleartkAnnotator<String> {

	private Map<String, Set<String>> gt = null;

	@SuppressWarnings("unchecked")
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		try {
			if (context.getConfigParameterValue(PathogenAnnotator.PARAM_GROUND_TRUTH) != null) {
				gt = (Map<String, Set<String>>) Serialization
						.deserialize((String) context.getConfigParameterValue(PathogenAnnotator.PARAM_GROUND_TRUTH));
			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean checkPrion(JCas jCas) {
		return JCasUtil.select(jCas, DictTerm.class).stream()
				.anyMatch(e -> e.getDictCanon().equalsIgnoreCase("prpsc-prion")
						|| e.getDictCanon().equalsIgnoreCase("prpsc-prpsc"));
	}

	private List<Feature> getInTitle(String candidate, JCas jCas) {
		for (Section s : JCasUtil.select(jCas, Section.class)) {
			if (s.getSectionType().equalsIgnoreCase("title")) {
				if (JCasUtil.selectCovered(jCas, DictTerm.class, s).stream()
						.anyMatch(e -> e.getDictCanon().equalsIgnoreCase(candidate)))
					return Collections.singletonList(new Feature("InTitle", "1"));
			}
		}

		return Collections.singletonList(new Feature("InTitle", "0"));
	}

	private List<Feature> getAnotherInTitle(String candidate, JCas jCas) {
		for (Section s : JCasUtil.select(jCas, Section.class)) {
			if (s.getSectionType().equalsIgnoreCase("title")) {
				if (JCasUtil.selectCovered(jCas, DictTerm.class, s).stream()
						.anyMatch(e -> !e.getDictCanon().equalsIgnoreCase(candidate)
								&& !(e.getDictCanon().equalsIgnoreCase("prpsc-prion")
										|| e.getDictCanon().equalsIgnoreCase("prpsc-prpsc"))))
					return Collections.singletonList(new Feature("AnotherInTitle", "1"));
			}
		}

		return Collections.singletonList(new Feature("AnotherInTitle", "0"));
	}

	private List<Feature> getCandidateAbstractText(String candidate, JCas jCas) {
		List<Feature> tokens = new ArrayList<>();

		for (Section s : JCasUtil.select(jCas, Section.class)) {
			if (s.getSectionType().equalsIgnoreCase("abstract")) {
				for (Sentence st : JCasUtil.selectCovered(jCas, Sentence.class, s)) {
					if (JCasUtil.selectCovered(jCas, DictTerm.class, st).stream()
							.anyMatch(e -> !e.getDictCanon().equalsIgnoreCase(candidate))) {
						tokens.addAll(JCasUtil.selectCovered(jCas, TokenAnnotation.class, st).stream()
								.map((TokenAnnotation token) -> new Feature("tokenAbstract", token.getCoveredText()))
								.collect(Collectors.toList()));
					}
				}
			}
		}

		return tokens;
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		if (!checkPrion(jCas))
			return;

		Set<String> candidates = JCasUtil.select(jCas, DictTerm.class).stream()
				.filter(e -> !(e.getDictCanon().equalsIgnoreCase("prpsc-prion")
						|| e.getDictCanon().equalsIgnoreCase("prpsc-prpsc")))
				.map(e -> e.getDictCanon()).collect(Collectors.toSet());

		Set<String> keepList = new HashSet<String>();

		// For each candidate look at the evidence and select which ones to delete
		for (String candidate : candidates) {
			List<Feature> features = new ArrayList<Feature>();
			features.addAll(getInTitle(candidate, jCas));
			features.addAll(getAnotherInTitle(candidate, jCas));
			//features.addAll(getCandidateAbstractText(candidate, jCas));

			if (isTraining()) {
				String pmid = ViewUriUtil.getURI(jCas).toString();
				String category = (gt.get(candidate) != null && gt.get(candidate).contains(pmid) ? "1" : "-1");
				// System.out.println(pmid + "|" + category + "|" + features);
				this.dataWriter.write(new Instance<String>(category, features));
			} else {
				// If background, remove it from the list of elements
				if (this.classifier.classify(features).equals("1")) 
				{
					keepList.add(candidate);
				}
			}
		}

		JCasUtil.select(jCas, DictTerm.class).stream().filter(e -> keepList.contains(e.getDictCanon())).forEach(e -> {
			NamedEntityMention n = new NamedEntityMention(jCas);
			n.setBegin(e.getBegin());
			n.setEnd(e.getEnd());
			n.setMentionId(e.getDictCanon());
			n.setMentionType("pathogen");
			n.addToIndexes(jCas);
		});
	}

	public static AnalysisEngineDescription getClassifierDescription(String modelFileName)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(PrPScAnnotator.class,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelFileName);
	}

	public static AnalysisEngineDescription getWriterDescription(String outputDirectory, String gt)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(PrPScAnnotator.class, CleartkAnnotator.PARAM_IS_TRAINING,
				true, DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, outputDirectory,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, LibSvmStringOutcomeDataWriter.class,
				PathogenAnnotator.PARAM_GROUND_TRUTH, gt);
	}

}