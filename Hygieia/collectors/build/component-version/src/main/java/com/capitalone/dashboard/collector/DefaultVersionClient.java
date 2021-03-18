package com.capitalone.dashboard.collector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.ComponentVersions;
import com.capitalone.dashboard.model.IpofficeClients;
import com.capitalone.dashboard.model.VersionDetailsJob;
import com.capitalone.dashboard.model.VersionData;

@Component
public class DefaultVersionClient implements VersionClient {
	private static final String DELIMITER = "__";
	private static final Log LOG = LogFactory.getLog(DefaultVersionClient.class);


	public Map<VersionDetailsJob, Set<Build>> getInstanceJobs(List<File> filesList) {
		Map<VersionDetailsJob, Set<Build>> result = new LinkedHashMap<>();
		ArrayList<String> jobsReportedSoFar = new ArrayList<String>();
		for(File forJobs: filesList) {
			LOG.info("getInstanceJobs: filename: "+forJobs.getName());
			String[] job = forJobs.getName().split(DELIMITER);
			if(job.length > 0) {
				if(!jobsReportedSoFar.contains(job[0])) {
					LOG.info("getInstanceJobs: New Job: "+job[0]);
					VersionDetailsJob reportJob = new VersionDetailsJob();
					reportJob.setJobName(job[0]);
					jobsReportedSoFar.add(job[0]);

					Set<Build> builds = new LinkedHashSet<>();
					result.put(reportJob, builds);
					LOG.info("getInstanceJobs: filesList size: " + filesList.size());
					for(File forBuilds: filesList) {
						// A basic Build object. This will be fleshed out later if this is a new Build.
						if(forBuilds.getName().contains(job[0])) {
							String[] buildInfo = forBuilds.getName().split(DELIMITER);
							LOG.info("getInstanceJobs: fileName: " +forBuilds.getName());
							LOG.info("getInstanceJobs: Build number: "+buildInfo[1]);
							if( buildInfo.length > 1) {
								Build hudsonBuild = new Build();
								hudsonBuild.setTimestamp(System.currentTimeMillis());
								hudsonBuild.setNumber(buildInfo[1]);
								hudsonBuild.setBuildUrl(forBuilds.getName());
								builds.add(hudsonBuild);
							}
							else {
								LOG.info("No Build number available");
								Build hudsonBuild = new Build();
								hudsonBuild.setTimestamp(System.currentTimeMillis());
								hudsonBuild.setNumber("0");
								hudsonBuild.setBuildUrl(forJobs.getName());
								builds.add(hudsonBuild);
							}

						}
					}
				}
			}
			else {
				LOG.info("getInstanceJobs: New Job: "+forJobs.getName());
				if(!jobsReportedSoFar.contains(forJobs.getName())) {
					VersionDetailsJob reportJob = new VersionDetailsJob();
					reportJob.setJobName(forJobs.getName());
					jobsReportedSoFar.add(forJobs.getName());

					Set<Build> builds = new LinkedHashSet<>();
					result.put(reportJob, builds);

					for(File forBuilds: filesList) {
						// A basic Build object. This will be fleshed out later if this is a new Build.
						if(forBuilds.getName().contains(job[0])) {
							String[] buildInfo = forJobs.getName().split(DELIMITER);
							if( buildInfo.length > 1) {
								Build hudsonBuild = new Build();
								LOG.info("getInstanceJobs: Build number: "+buildInfo[2]);
								hudsonBuild.setNumber(buildInfo[1]);
								hudsonBuild.setTimestamp(System.currentTimeMillis());
								hudsonBuild.setBuildUrl(forJobs.getName());
								builds.add(hudsonBuild);
							}
							else {
								LOG.info("No Build number available");
								Build hudsonBuild = new Build();
								hudsonBuild.setNumber("0");
								hudsonBuild.setTimestamp(System.currentTimeMillis());
								hudsonBuild.setBuildUrl(forJobs.getName());
								builds.add(hudsonBuild);
							}
						}
					}	

				}
			}

		}
		jobsReportedSoFar.clear();
		return result;
	}

	@SuppressWarnings("unused")
	@Override
	public VersionData getVersionDetails(String jobName, String buildNumber, String filePath, String fileName) {
		Properties prop=new Properties();
		VersionData versionData = new VersionData();
		InputStream input = null;
		versionData.setTimestamp(System.currentTimeMillis());
		versionData.setJobName(jobName);
		versionData.setBuildId(buildNumber);

		try {
			LOG.info("Reading configs from:  "+filePath+"/"+fileName);
			input = new FileInputStream(filePath+"/"+fileName);
			if (input!=null) {
				prop.load(input);
				if(prop!=null) {
					addIpofficeComponents(prop, versionData);
					addIxWorkplaceComponents(prop, versionData);
					addComComponents(prop, versionData);
					addJ100Components(prop, versionData);
					addCasComponents(prop, versionData);
				}else {
					LOG.info("Properties is empty"+prop);
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

		return versionData;
	}

	private void addCasComponents(Properties prop, VersionData versionData) {

		if(!prop.isEmpty() && prop!=null) {
			ComponentVersions ipoComponents = new ComponentVersions();
			if(prop.getProperty("cas")!=null && !prop.getProperty("cas").isEmpty()) {
				ipoComponents.setName("Certificate Agent");
				ipoComponents.setVersion(prop.getProperty("cas"));
				versionData.getComponentVersions().add(ipoComponents);
			}
		}

	}

	private void addJ100Components(Properties prop, VersionData versionData) {

		Map<String, String> components = new HashMap<String, String>();
		if(!prop.isEmpty() && prop!=null) {
			ComponentVersions ipoComponents = new ComponentVersions();
			if(prop.getProperty("j1xx")!=null && !prop.getProperty("j1xx").isEmpty()) {
				ipoComponents.setName("J1XX");
				ipoComponents.setVersion(prop.getProperty("j1xx"));
				components.put("J129", prop.getProperty("j129"));
				components.put("J139", prop.getProperty("j139"));
				components.put("J159", prop.getProperty("j159"));
				components.put("J169", prop.getProperty("j169"));
				components.put("J179", prop.getProperty("j179"));
				components.put("J189", prop.getProperty("j189"));
				for (String key: components.keySet()) {
					if(components.get(key)!=null) {
						IpofficeClients ipoClients = new IpofficeClients();
						ipoClients.setName(key);
						ipoClients.setVersion(components.get(key));
						ipoComponents.getComponents().add(ipoClients);
					}
				}			
				versionData.getComponentVersions().add(ipoComponents);
			}
		}
	}

	private void addComComponents(Properties prop, VersionData versionData) {

		Map<String, String> components = new HashMap<String, String>();
		if(!prop.isEmpty() && prop!=null) {
			ComponentVersions ipoComponents = new ComponentVersions();
			if(prop.getProperty("com")!=null && !prop.getProperty("com").isEmpty()) {
				ipoComponents.setName("COM");
				ipoComponents.setVersion(prop.getProperty("com"));
				components.put("com-app", prop.getProperty("com_app"));
				components.put("com-proxy", prop.getProperty("com_proxy"));
				components.put("com-datastore", prop.getProperty("com_datastore"));
				for (String key: components.keySet()) {
					if(components.get(key)!=null) {
						IpofficeClients ipoClients = new IpofficeClients();
						ipoClients.setName(key);
						ipoClients.setVersion(components.get(key));
						ipoComponents.getComponents().add(ipoClients);
					}
				}
				versionData.getComponentVersions().add(ipoComponents);
			}	
		}
	}

	private void addIxWorkplaceComponents(Properties prop, VersionData versionData) {


		Map<String, String> components = new HashMap<String, String>();
		if(!prop.isEmpty() && prop!=null) {
			ComponentVersions ipoComponents = new ComponentVersions();
			if(prop.getProperty("equinox")!=null && !prop.getProperty("equinox").isEmpty()) {
				ipoComponents.setName("Avaya IX Workplace");
				ipoComponents.setVersion(prop.getProperty("equinox"));
				components.put("Android", prop.getProperty("equinox_android"));
				components.put("Windows", prop.getProperty("equinox_windows"));
				components.put("Mac OS", prop.getProperty("equinox_mac"));
				components.put("iOS", prop.getProperty("equinox_ios"));
				for (String key: components.keySet()) {
					if(components.get(key)!=null) {
						IpofficeClients ipoClients = new IpofficeClients();
						ipoClients.setName(key);
						ipoClients.setVersion(components.get(key));
						ipoComponents.getComponents().add(ipoClients);
					}
				}

				versionData.getComponentVersions().add(ipoComponents);
			}	
		}
	}

	private void addIpofficeComponents(Properties prop, VersionData versionData) {

		Map<String, String> components = new HashMap<String, String>();
		if(!prop.isEmpty() && prop!=null) {
			ComponentVersions ipoComponents = new ComponentVersions();
			if(prop.getProperty("ipoffice")!=null && !prop.getProperty("ipoffice").isEmpty()) {
				ipoComponents.setName("IP Office");
				ipoComponents.setVersion(prop.getProperty("ipoffice"));
				components.put("IP Office Core", prop.getProperty("core"));
				components.put("Jade", prop.getProperty("jade"));
				components.put("Voicemail", prop.getProperty("evml"));
				components.put("Voicemail", prop.getProperty("vmpro"));
				components.put("Web Management", prop.getProperty("webmanager"));
				components.put("one-X Portal", prop.getProperty("onexportal"));
				components.put("Media Manager", prop.getProperty("mediamanager"));
				components.put("Admin CD", prop.getProperty("admincd"));
				components.put("Web Client", prop.getProperty("webclient"));
				components.put("WebRTC Gateway", prop.getProperty("webrtc"));
				components.put("Soft Console", prop.getProperty("softconsole"));

				for (String key: components.keySet()) {
					if(components.get(key)!=null) {
						IpofficeClients ipoClients = new IpofficeClients();
						ipoClients.setName(key);
						ipoClients.setVersion(components.get(key));
						ipoComponents.getComponents().add(ipoClients);
					}
				}

				versionData.getComponentVersions().add(ipoComponents);
			}
		}
	}

}
