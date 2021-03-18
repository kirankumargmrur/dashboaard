package com.capitalone.dashboard.model;

/**
 * CollectorItem extension to store the instance, build job and build url.
 */
public class BuildJob extends JobCollectorItem {
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		BuildJob buildJob = (BuildJob) o;

		return getInstanceUrl().equals(buildJob.getInstanceUrl()) && getJobName().equals(buildJob.getJobName());
	}

	@Override
	public int hashCode() {
		int result = getInstanceUrl().hashCode();
		result = 31 * result + getJobName().hashCode();
		return result;
	}
}
