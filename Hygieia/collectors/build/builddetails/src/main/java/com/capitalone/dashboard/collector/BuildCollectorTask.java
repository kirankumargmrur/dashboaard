package com.capitalone.dashboard.collector;


import com.capitalone.dashboard.model.BuildCollector;
import com.capitalone.dashboard.model.CIData;
import com.capitalone.dashboard.repository.BuildCollectorRepository;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CIDataRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * CollectorTask that fetches Build information from Bamboo and Crucible
 */
@Component
public class BuildCollectorTask extends CollectorTask<BuildCollector> {
	@SuppressWarnings("PMD.UnusedPrivateField")
	private static final Logger LOG = LoggerFactory.getLogger(BuildCollectorTask.class);
	private final BuildCollectorRepository buildCollectorRepository;
	private final CIDataRepository buildRepository;
	private final BuildClient buildClient;
	private final BuildSettings buildSettings;
	@Autowired
	public BuildCollectorTask(TaskScheduler taskScheduler,
			BuildCollectorRepository buildCollectorRepository, 
			BuildClient buildClient,
			BuildSettings buildSettings,
			CIDataRepository buildRepository) {
		super(taskScheduler, "BuildDetails");
		this.buildCollectorRepository = buildCollectorRepository;
		this.buildRepository=buildRepository;
		this.buildClient = buildClient;
		this.buildSettings = buildSettings;
	}

	@Override
	public BuildCollector getCollector() {
		return BuildCollector.prototype(buildSettings.getServers(), buildSettings.getNiceNames());
	}

	@Override
	public BaseCollectorRepository<BuildCollector> getCollectorRepository() {
		return buildCollectorRepository;
	}

	@Override
	public String getCron() {
		return buildSettings.getCron();
	}

	@SuppressWarnings("unused")
	@Override
	public void collect(BuildCollector collector) {
		String planName="";
		String buildNumber = "";
		String jobName = "";
		String jenkinsBuildId = "";
		long start = System.currentTimeMillis();
		Set<ObjectId> udId = new HashSet<>();
		udId.add(collector.getId());
		Properties prop=new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(buildSettings.getFilePath());
			LOG.info("Reading configs from:  "+buildSettings.getFilePath());
			if (input!=null) {
				prop.load(input);
				if(prop!=null) {
					planName=prop.getProperty("planKey");
					buildNumber=prop.getProperty("buildNumber");
					jobName=prop.getProperty("jobName");
					jenkinsBuildId=prop.getProperty("jenkinsBuildId");
				}
				else {
					LOG.info("Configuration file not found");
				}
			}else
			{
				LOG.info("Unable to find the Config File");
			}
		} catch (IOException ex) {
			LOG.info("Unable to load the configuration file");
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					LOG.info("Unable to close the file");
				}
			}
		}
		for (String instanceUrl : collector.getBuildServers()) {
			logBanner(instanceUrl);
			try {
				log("Fetched jobs", start);
				LOG.info("Fetching build details");

				CIData build = buildClient.getBuildDetails(buildClient.getInstanceJobs(instanceUrl, planName , buildNumber), instanceUrl, jobName, jenkinsBuildId);
				if (build != null) {
					if(isNewBuild(build)) {
						buildRepository.save(build);
						LOG.info("Saved to DB");
						try{
							File file = new File(buildSettings.getFilePath());
							if(file.delete()){
								LOG.info(file.getName() + " is deleted!");
							}else{
								LOG.info("Delete operation is failed.");
							}
						}catch(Exception e){
							LOG.info("Unable to Perform delete operation");
						}
					}
					else {
						LOG.info("The build "+build.getJobName()+" with build number "+build.getBuildId()+" already exists");
					}
				}
				else {
					LOG.info("Unable to fetch build details");
				}
				log("Finished", start);
			} catch (RestClientException rce) { // since it was a rest exception, we will not delete this job  and wait for
				// rest exceptions to clear up at a later run.
				log("Error getting jobs for: " + instanceUrl, start);
			}
		}
	}

	private boolean isNewBuild(CIData build) {
		return buildRepository.findByJobNameAndBuildId(build.getJobName(),
				build.getBuildId()) == null;
	}
}
