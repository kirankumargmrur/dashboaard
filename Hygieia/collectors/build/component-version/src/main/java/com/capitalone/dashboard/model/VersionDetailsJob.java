package com.capitalone.dashboard.model;

public class VersionDetailsJob extends CollectorItem {
	protected static final String JOB_NAME = "job_name";

	public String getJobName() {
		return (String) getOptions().get(JOB_NAME);
	}

	public void setJobName(String jobName) {
		getOptions().put(JOB_NAME, jobName);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		VersionDetailsJob version = (VersionDetailsJob) o;
		return getJobName().equals(version.getJobName());
	}

	@Override
	public int hashCode() {
		int result = getJobName().hashCode();
		return result;
	}
}
