package com.capitalone.dashboard.model;

import java.util.HashMap;
import java.util.Map;

public class VersionCollector extends Collector {
	
	private String versionsFilePath;

    public String getVersionsFilePath() {
		return versionsFilePath;
	}

	public void setVersionsFilePath(String versionsFilePath) {
		this.versionsFilePath = versionsFilePath;
	}
	
	public static VersionCollector prototype(String filePath) {
		VersionCollector protoType = new VersionCollector();
		protoType.setName("ComponentVersions");
		protoType.setCollectorType(CollectorType.Versions);
		protoType.setOnline(true);
		protoType.setEnabled(true);
		protoType.setVersionsFilePath(filePath);
		
		Map<String, Object> allOptions = new HashMap<>();
        allOptions.put(VersionDetailsJob.JOB_NAME,"");
        protoType.setAllFields(allOptions);

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put(VersionDetailsJob.JOB_NAME,"");
        protoType.setUniqueFields(uniqueOptions);
        return protoType;
	}

}
