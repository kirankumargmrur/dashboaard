package com.capitalone.dashboard.model;

public class CommitData {
    private String commitId = "";
    private String commitURL = "";
    private String commitDescription = "";
    private String commitAuthor = "";
    private String commitDate = "";


    public CommitData(String commitId, String url, String description, String author, String date) {
    	this.commitId = commitId;
        this.commitURL = url;
        this.commitDescription = description;
        this.commitAuthor = author;
        this.commitDate = date;
    }

   
    public CommitData() {
    }

    public String getCommitId() {
		return commitId;
	}

	public void setCommitId(String commitId) {
		this.commitId = commitId;
	}

	public String getCommitURL() {
		return commitURL;
	}

	public void setCommitURL(String commitURL) {
		this.commitURL = commitURL;
	}

	public String getCommitDescription() {
		return commitDescription;
	}

	public void setCommitDescription(String commitDescription) {
		this.commitDescription = commitDescription;
	}

	public String getCommitAuthor() {
		return commitAuthor;
	}

	public void setCommitAuthor(String commitAuthor) {
		this.commitAuthor = commitAuthor;
	}

	public String getCommitDate() {
		return commitDate;
	}

	public void setCommitDate(String commitDate) {
		this.commitDate = commitDate;
	}


	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommitData that = (CommitData) o;

        return getCommitId().equals(that.getCommitId());
    }

    @Override
    public int hashCode() {
        int result = commitURL.hashCode();
        result = 31 * result + commitURL.hashCode();
        return result;
    }
}
