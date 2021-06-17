package edu.northwestern.at.utils.corpuslinguistics.tokenizer;

import edu.northwestern.at.utils.ListFactory;
import edu.northwestern.at.utils.PatternReplacer;
import java.util.List;
import java.util.StringTokenizer;

public class PennTreebankTokenizer extends AbstractWordTokenizer implements WordTokenizer {
	protected List<PatternReplacer> pennPatterns = ListFactory.createNewList();

	public String prepareTextForTokenization(String s) {
		for (int i = 0; i < pennPatterns.size(); ++i) {
			s = ((PatternReplacer) pennPatterns.get(i)).replace(s);
		}

		return s.trim();
	}

	public List<String> extractWords(String text) {
		List<String> result = ListFactory.createNewList();
		String fixedText = prepareTextForTokenization(text);
		StringTokenizer tokenizer = new StringTokenizer(fixedText);

		while (tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken());
		}

		return result;
	}

	public PennTreebankTokenizer() {
		pennPatterns.add(new PatternReplacer("``", "`` "));
		pennPatterns.add(new PatternReplacer("''", "  ''"));
		pennPatterns.add(new PatternReplacer("([?!\".,;:@#$%&])", " $1 "));
		pennPatterns.add(new PatternReplacer("\\.\\.\\.", " ... "));
		pennPatterns.add(new PatternReplacer("\\s+", " "));
		pennPatterns.add(new PatternReplacer(",([^0-9])", " , $1"));
		pennPatterns.add(new PatternReplacer("([^.])([.])([\\])}>\"']*)\\s*$", "$1 $2$3 "));
		pennPatterns.add(new PatternReplacer("([\\[\\](){}<>])", " $1 "));
		pennPatterns.add(new PatternReplacer("--", " -- "));
		pennPatterns.add(new PatternReplacer("$", " "));
		pennPatterns.add(new PatternReplacer("^", " "));
		pennPatterns.add(new PatternReplacer("([^'])' ", "$1 ' "));
		pennPatterns.add(new PatternReplacer("'([sSmMdD]) ", " '$1 "));
		pennPatterns.add(new PatternReplacer("'ll ", " 'll "));
		pennPatterns.add(new PatternReplacer("'re ", " 're "));
		pennPatterns.add(new PatternReplacer("'ve ", " 've "));
		pennPatterns.add(new PatternReplacer("'em ", " 'em "));
		pennPatterns.add(new PatternReplacer("n't ", " n't "));
		pennPatterns.add(new PatternReplacer("'LL ", " 'LL "));
		pennPatterns.add(new PatternReplacer("'RE ", " 'RE "));
		pennPatterns.add(new PatternReplacer("'EM ", " 'EM "));
		pennPatterns.add(new PatternReplacer("'VE ", " 'VE "));
		pennPatterns.add(new PatternReplacer("N'T ", " N'T "));
		pennPatterns.add(new PatternReplacer(" ([Cc])annot ", " $1an not "));
		pennPatterns.add(new PatternReplacer(" ([Dd])'ye ", " $1' ye "));
		pennPatterns.add(new PatternReplacer(" ([Gg])imme ", " $1im me "));
		pennPatterns.add(new PatternReplacer(" ([Gg])onna ", " $1on na "));
		pennPatterns.add(new PatternReplacer(" ([Gg])otta ", " $1ot ta "));
		pennPatterns.add(new PatternReplacer(" ([Ll])emme ", " $1em me "));
		pennPatterns.add(new PatternReplacer(" ([Mm])ore'n ", " $1ore 'n "));
		pennPatterns.add(new PatternReplacer(" '([Tt])is ", " '$1 is "));
		pennPatterns.add(new PatternReplacer(" '([Tt])was ", " '$1 was "));
		pennPatterns.add(new PatternReplacer(" ([Ww])anna ", " $1an na "));
		pennPatterns.add(new PatternReplacer(" ([Ww])anna ", " $1an na "));
		pennPatterns.add(new PatternReplacer(" ([Ww])haddya ", " $1ha dd ya "));
		pennPatterns.add(new PatternReplacer(" ([Ww])hatcha ", " $1ha t cha "));
		pennPatterns.add(new PatternReplacer("([A-MO-Za-mo-z])'([tT])", "$1 '$2"));
		pennPatterns.add(new PatternReplacer(" ([A-Z]) \\.", " $1. "));
		pennPatterns.add(new PatternReplacer("\\s+", " "));
		pennPatterns.add(new PatternReplacer("^\\s+", ""));
	}
}