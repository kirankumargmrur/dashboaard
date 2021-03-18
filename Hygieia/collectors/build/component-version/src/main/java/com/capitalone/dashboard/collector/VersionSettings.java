package com.capitalone.dashboard.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "version")
public class VersionSettings {
	private String cron;
	private boolean saveLog = false;
	private String filePath;

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public boolean isSaveLog() {
		return saveLog;
	}

	public void setSaveLog(boolean saveLog) {
		this.saveLog = saveLog;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
}
