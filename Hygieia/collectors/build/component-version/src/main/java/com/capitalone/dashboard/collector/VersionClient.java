package com.capitalone.dashboard.collector;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.VersionData;
import com.capitalone.dashboard.model.VersionDetailsJob;

public interface VersionClient {

	Map<VersionDetailsJob, Set<Build>> getInstanceJobs(List<File> fileList);
	VersionData getVersionDetails(String jobName, String buildNumber, String filePath, String fileName);

}
