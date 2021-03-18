package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.CIData;

/**
 * Client for fetching build information from Bamboo and Crucible
 */
public interface BuildClient {

	/**
	 * @param instanceUrl the URL for the Bamboo instance
	 * @return a summary of every build for each job on the instance
	 */
	String getInstanceJobs(String instanceUrl, String planName, String buildNumber);

	/**
	 * Fetch full populated build information for a build.
	 *
	 * @param buildUrl the url of the build
	 * @param instanceUrl
	 * @return a Build instance or null
	 */
	CIData getBuildDetails(String buildUrl, String instanceUrl , String jobName , String jenkinsBuildId);
}
