package readbiomed.annotators.dictionary.pathogens.build;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NCBIEntry {
	private String id;
	private String canonical;
	private Set<String> synonyms = new HashSet <> ();
	private List<String> parents = new ArrayList <> ();
	private List<String> children = new ArrayList <> ();

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	public NCBIEntry (String id)
	{ this.id = id; }
	
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
	public Set<String> getSynonyms() {
		return synonyms;
	}
	public void setSynonyms(Set<String> synonyms) {
		this.synonyms = synonyms;
	}
	public List<String> getParents() {
		return parents;
	}
	public void setParents(List<String> parents) {
		this.parents = parents;
	}
}