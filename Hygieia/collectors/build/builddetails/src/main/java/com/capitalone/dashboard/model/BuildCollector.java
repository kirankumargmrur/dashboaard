package com.capitalone.dashboard.model;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extension of Collector that stores current build server configuration.
 */
public class BuildCollector extends Collector {
	private List<String> buildServers = new ArrayList<>();
	private List<String> niceNames = new ArrayList<>();
	private String crucibleURL;

	public String getCrucibleURL() {
		return crucibleURL;
	}
	public void setCrucibleURL(String crucibleURL) {
		this.crucibleURL=crucibleURL;
	}
	public List<String> getBuildServers() {
		return buildServers;
	}

	public List<String> getNiceNames() {
		return niceNames;
	}

	public void setNiceNames(List<String> niceNames) {
		this.niceNames = niceNames;
	}

	public void setBuildServers(List<String> buildServers) {
		this.buildServers = buildServers;
	}

	public static BuildCollector prototype(List<String> buildServers, List<String> niceNames) {
		BuildCollector protoType = new BuildCollector();
		protoType.setName("BuildDetails");
		protoType.setCollectorType(CollectorType.Build);
		protoType.setOnline(true);
		protoType.setEnabled(true);
		protoType.getBuildServers().addAll(buildServers);
		if (!CollectionUtils.isEmpty(niceNames)) {
			protoType.getNiceNames().addAll(niceNames);
		}
		Map<String, Object> options = new HashMap<>();
		options.put(BuildJob.INSTANCE_URL,"");
		options.put(BuildJob.JOB_URL,"");
		options.put(BuildJob.JOB_NAME,"");
		protoType.setAllFields(options);
		protoType.setUniqueFields(options);
		return protoType;
	}


}
