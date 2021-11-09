package readbiomed.readers.pmc;

import java.util.LinkedList;

public class PMCArticle {
	private String pmid;
	private String articleTitle;
	private String abstractText;
	
	private LinkedList <PMCSection> sections;

	public PMCArticle(String pmid, String articleTitle, String abstractText, LinkedList <PMCSection> sections) {
		this.pmid = pmid;
		this.articleTitle = articleTitle;
		this.abstractText = abstractText;
		this.sections = sections;
	}

	public String getPMID() {
		return pmid;
	}

	public void setPMID(String pmid) {
		this.pmid = pmid;
	}

	public String getArticleTitle() {
		return articleTitle;
	}

	public void setArticleTitle(String articleTitle) {
		this.articleTitle = articleTitle;
	}

	public String getAbstractText() {
		return abstractText;
	}

	public void setAbstractText(String abstractText) {
		this.abstractText = abstractText;
	}
	
	public LinkedList<PMCSection> getSections() {
		return sections;
	}

	public void setSections(LinkedList<PMCSection> sections) {
		this.sections = sections;
	}
}