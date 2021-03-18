package com.capitalone.dashboard.model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated custom result pushed from a continuous integration build execution. 
 *
 */
@Document(collection="ci_build_data")
public class CIData extends BaseModel {
    private String jobName;
    private String buildId;
    private String repoBranchName;
    private List<JiraIssue> jiras = new ArrayList<>();
    private List<CommitData> commits = new ArrayList<>();
    private String buildAuthor;
    private BuildStatus buildStatus;
    private List<CIArtifactData> artifacts = new ArrayList<>();
    private long timestamp;
    private String buildURL;
    
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long l) {
		this.timestamp = l;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public String getBuildId() {
		return buildId;
	}
	public void setBuildId(String buildId) {
		this.buildId = buildId;
	}
	public String getRepoBranchName() {
		return repoBranchName;
	}
	public void setRepoBranchName(String repoBranchName) {
		this.repoBranchName = repoBranchName;
	}
	public List<JiraIssue> getJiras() {
		return jiras;
	}
	public void setJiras(List<JiraIssue> jiras) {
		this.jiras = jiras;
	}
	public List<CommitData> getCommits() {
		return commits;
	}
	public void setCommits(List<CommitData> commits) {
		this.commits = commits;
	}
	public String getBuildAuthor() {
		return buildAuthor;
	}
	public void setBuildAuthor(String buildAuthor) {
		this.buildAuthor = buildAuthor;
	}
	public BuildStatus getBuildStatus() {
		return buildStatus;
	}
	public void setBuildStatus(BuildStatus buildStatus) {
		this.buildStatus = buildStatus;
	}
	public List<CIArtifactData> getArtifacts() {
		return artifacts;
	}
	public void setArtifacts(List<CIArtifactData> artifacts) {
		this.artifacts = artifacts;
	}
	
	public String getBuildURL() {
		
		return buildURL;
	}
	public void setBuildURL(String buildURL) {
		this.buildURL = buildURL;
		
	}
    
    
}

