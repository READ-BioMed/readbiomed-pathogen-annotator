package readbiomed.annotators.dictionary.pathogens.build;

import java.util.List;
import java.util.ArrayList;

public class OwlClass {
	
	private String id;
	private String canonical;
	private List<String> variants;
	
	private String rawOwlClassString;
	private List<String> rawAxiomStrings;
	
	public OwlClass() {
		rawAxiomStrings = new ArrayList<>();
		variants = new ArrayList<>();
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCanonical() {
		return canonical;
	}
	public void setCanonical(String canonical) {
		this.canonical = canonical;
	}
	public List<String> getVariants() {
		return variants;
	}
	public void setVariants(List<String> variants) {
		this.variants = variants;
	}

	public String getRawOwlClassString() {
		return rawOwlClassString;
	}

	public void setRawOwlClassString(String rawOwlClassString) {
		this.rawOwlClassString = rawOwlClassString;
	}

	public List<String> getRawAxiomStrings() {
		return rawAxiomStrings;
	}

	public void setRawAxiomStrings(List<String> rawAxiomStrings) {
		this.rawAxiomStrings = rawAxiomStrings;
	}
}
