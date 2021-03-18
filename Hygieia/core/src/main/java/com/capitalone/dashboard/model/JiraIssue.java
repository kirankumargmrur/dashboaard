package com.capitalone.dashboard.model;

public class JiraIssue {
    private String jiraId = "";
    private String jiraURL = "";
    private String jiraDescription = "";
    private String jiraState = "";
    private String crucibleId = "";


    public String getCrucible() {
		return crucibleId;
	}

	public void setCrucible (String crucibleId) {
		this.crucibleId = crucibleId;
	}

	public JiraIssue(String jiraId, String url, String description, String state, String crucibleId) {
    	this.jiraId = jiraId;
        this.jiraURL = url;
        this.jiraDescription = description;
        this.jiraState = state;
        this.crucibleId=crucibleId;
    }

    public JiraIssue() {
    }

    public String getUrl() {
        return jiraURL;
    }

    public void setUrl(String url) {
        this.jiraURL = url;
    }

    public String getJiraId() {
        return jiraId;
    }

    public void setJiraId(String id) {
        this.jiraId = id;
    }

    public String getJiraDescription() {
        return jiraDescription;
    }

    public void setJiraDescription(String description) {
        this.jiraDescription = description;
    }
    
    public String getJiraState() {
        return jiraState;
    }

    public void setJiraState(String state) {
        this.jiraState = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JiraIssue that = (JiraIssue) o;

        return getJiraId().equals(that.getJiraId());
    }

    @Override
    public int hashCode() {
        int result = jiraURL.hashCode();
        result = 31 * result + jiraURL.hashCode();
        return result;
    }
}
