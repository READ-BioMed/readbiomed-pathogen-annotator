package readbiomed.annotators.dictionary.utils;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Stack;

import org.apache.uima.conceptMapper.support.stemmer.Stemmer;

import edu.ucdenver.ccp.nlp.wrapper.conceptmapper.stemmer.ConceptMapperBioLemmatizer;

public class ConceptMapperBioLemmatizerPool implements Stemmer {

	private static int poolSize = 10;

	private static Stack<ConceptMapperBioLemmatizer> pool = new Stack<>();

	static {
		for (int i = 0; i < poolSize; i++) {
			pool.add(new ConceptMapperBioLemmatizer());
		}
	}

	private static synchronized ConceptMapperBioLemmatizer getStemmer() {
		synchronized (pool) {
			while (pool.size() == 0);

			return pool.pop();
		}
	}

	private static void returnStemmer(ConceptMapperBioLemmatizer stemmer) {
		synchronized (pool) {
			pool.add(stemmer);
		}
	}

	@Override
	public String stem(String token) {
		// Get stemmer
		ConceptMapperBioLemmatizer o = getStemmer();

		String output = o.stem(token);

		// return stemmer
		returnStemmer(o);

		return output;
	}

	@Override
	public void initialize(String dictionary) throws FileNotFoundException, ParseException {
	}
}
