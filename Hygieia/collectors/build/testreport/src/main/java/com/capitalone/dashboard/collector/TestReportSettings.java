package com.capitalone.dashboard.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bean to hold settings specific to the test collector.
 */
@Component
@ConfigurationProperties(prefix="testreport")
public class TestReportSettings {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestReportSettings.class);
    private String cron;
    private String path;

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
    	LOGGER.info("TestReportSettings: setCron: "+ cron);
        this.cron = cron;
    }

    public String getTestReportPaths() {
    	LOGGER.info("TestReportSettings: getTestReportPaths: "+ path);
        return path;
    }

    public void setTestReportPaths(List<String> paths) {
    	LOGGER.info("TestReportSettings: setTestReportPaths: "+ path);
        this.path = path;
    }
}
