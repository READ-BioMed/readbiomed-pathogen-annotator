package readbiomed.readers.medline;

public class MedlineCitation {
	private String pmid;
	private String articleTitle;
	private String abstractText;

	public MedlineCitation(String pmid, String articleTitle, String abstractText) {
		this.pmid = pmid;
		this.articleTitle = articleTitle;
		this.abstractText = abstractText;
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
}