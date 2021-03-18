package com.capitalone.dashboard.model;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="component_versions")
public class VersionData extends BaseModel{
	private ObjectId collectorItemId;
	private String jobName;
	private String buildId;
	private long timestamp;

	private List<ComponentVersions> componentVersions = new ArrayList<>();

	public ObjectId getCollectorItemId() {
		return collectorItemId;
	}

	public void setCollectorItemId(ObjectId collectorItemId) {
		this.collectorItemId = collectorItemId;
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

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public List<ComponentVersions> getComponentVersions() {
		return componentVersions;
	}

	public void setComponentVersions(List<ComponentVersions> componentVersions) {
		this.componentVersions = componentVersions;
	}
}
