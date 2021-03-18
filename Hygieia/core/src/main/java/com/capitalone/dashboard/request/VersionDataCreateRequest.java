package com.capitalone.dashboard.request;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.capitalone.dashboard.model.ComponentVersions;

public class VersionDataCreateRequest {
	
	private String hygieiaId;
    @NotNull
    private long timestamp;
    @NotNull
    private String jobName;
    @NotNull
    private String buildId;
    
	private List<ComponentVersions> metrics = new ArrayList<>();

	public String getHygieiaId() {
		return hygieiaId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getJobName() {
		return jobName;
	}

	public String getBuildId() {
		return buildId;
	}

	public List<ComponentVersions> getMetrics() {
		return metrics;
	}

	public void setHygieiaId(String hygieiaId) {
		this.hygieiaId = hygieiaId;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public void setBuildId(String buildId) {
		this.buildId = buildId;
	}

	public void setMetrics(List<ComponentVersions> metrics) {
		this.metrics = metrics;
	}

	public String getNiceName() {
		
		return null;
	}

}
