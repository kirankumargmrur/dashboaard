package com.capitalone.dashboard.model;

public class TestReportJob extends CollectorItem {
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

        TestReportJob that = (TestReportJob) o;
        return getJobName().equals(that.getJobName());
    }

    @Override
    public int hashCode() {
        int result = getJobName().hashCode();
        return result;
    }
}
