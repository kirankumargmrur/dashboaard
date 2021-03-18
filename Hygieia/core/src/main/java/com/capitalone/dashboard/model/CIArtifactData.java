package com.capitalone.dashboard.model;

public class CIArtifactData {
    private String artifactName = "";
    private String artifactURL = "";
    private String artifactDescription = "";


    public CIArtifactData(String name, String url, String description) {
    	this.artifactName = name;
        this.artifactURL = url;
        this.artifactDescription = description;
    }
    
    public CIArtifactData() {
    }

	public String getArtifactName() {
		return artifactName;
	}

	public void setArtifactName(String artifactName) {
		this.artifactName = artifactName;
	}

	public String getArtifactURL() {
		return artifactURL;
	}

	public void setArtifactURL(String artifactURL) {
		this.artifactURL = artifactURL;
	}

	public String getArtifactDescription() {
		return artifactDescription;
	}

	public void setArtifactDescription(String artifactDescription) {
		this.artifactDescription = artifactDescription;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CIArtifactData that = (CIArtifactData) o;

        return getArtifactName().equals(that.getArtifactName());
    }

    @Override
    public int hashCode() {
        int result = artifactURL.hashCode();
        result = 31 * result + artifactURL.hashCode();
        return result;
    }
}
