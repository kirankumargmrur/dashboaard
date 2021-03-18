package com.capitalone.dashboard.collector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.VersionCollector;
import com.capitalone.dashboard.model.VersionData;
import com.capitalone.dashboard.model.VersionDetailsJob;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.VersionCollectorRepository;
import com.capitalone.dashboard.repository.VersionDataRepository;
import com.capitalone.dashboard.repository.VersionDetailsJobRepository;

@Component
public class VersionCollectorTask  extends CollectorTask<VersionCollector> {


	private static final Log LOG = LogFactory.getLog(VersionCollectorTask.class);

	private final VersionCollectorRepository versionCollectorRepository;
	private final VersionDetailsJobRepository versionDetailsJobRepository;
	private final VersionDataRepository versionDataRepository;
	private final VersionSettings versionSettings;
	private final ComponentRepository dbComponentRepository;
	private final VersionClient versionClient;
	@Autowired
	public VersionCollectorTask(TaskScheduler taskScheduler, 
			VersionCollectorRepository versionCollectorRepository,
			VersionDetailsJobRepository versionDetailsJobRepository,
			VersionDataRepository versionDataRepository,
			VersionSettings versionSettings,
			VersionClient versionClient,
			ComponentRepository dbComponentRepository) {
		super(taskScheduler, "ComponentVersions");
		this.versionSettings = versionSettings;
		this.versionClient = versionClient;
		this.versionDataRepository = versionDataRepository;
		this.versionDetailsJobRepository = versionDetailsJobRepository;
		this.versionCollectorRepository = versionCollectorRepository;
		this.dbComponentRepository = dbComponentRepository;
	}

	@Override
	public VersionCollector getCollector() {
		return VersionCollector.prototype(versionSettings.getFilePath());
	}

	@Override
	public BaseCollectorRepository<VersionCollector> getCollectorRepository() {
		return versionCollectorRepository;
	}

	@Override
	public String getCron() {
		return versionSettings.getCron();
	}

	@Override
	public void collect(VersionCollector collector) {
		long start = System.currentTimeMillis();
		Set<ObjectId> udId = new HashSet<>();
		udId.add(collector.getId());
		log("collect: collectorId: " + collector.getId());
		clean(collector);
		LOG.info("File Path: " + versionSettings.getFilePath());
		List<File> filesList = readFilesInDirectory(Paths.get(versionSettings.getFilePath()));      	
		Map<VersionDetailsJob, Set<Build>> buildsByJob = 
				versionClient.getInstanceJobs(filesList);
		addNewJobs(buildsByJob.keySet(), collector);
		List<VersionDetailsJob> enabledJobs = enabledJobs(collector);
		if (!enabledJobs.isEmpty()) {
			addJobDetails(enabledJobs, buildsByJob);  
		}
		log("Finished", start);
	}

	private void clean(VersionCollector collector) {

		// First delete jobs that will be no longer collected because servers have moved etc.
		deleteUnwantedJobs(collector);

		Set<ObjectId> uniqueIDs = new HashSet<>();
		for (com.capitalone.dashboard.model.Component comp : dbComponentRepository
				.findAll()) {
			if (comp.getCollectorItems() == null
					|| comp.getCollectorItems().isEmpty()) continue;
			List<CollectorItem> itemList = comp.getCollectorItems().get(
					CollectorType.Versions);
			if (itemList == null) continue;
			for (CollectorItem ci : itemList) {
				if (ci != null && ci.getCollectorId().equals(collector.getId())) {
					uniqueIDs.add(ci.getId());
				}
			}
		}

		List<VersionDetailsJob> jobList = new ArrayList<>();
		Set<ObjectId> udId = new HashSet<>();
		udId.add(collector.getId());
		for (VersionDetailsJob job : versionDetailsJobRepository
				.findByCollectorIdIn(udId)) {
			if (job != null) {
				job.setEnabled(uniqueIDs.contains(job.getId()));
				jobList.add(job);
			}
		}
		versionDetailsJobRepository.save(jobList);
	}

	private void deleteUnwantedJobs(VersionCollector collector) {

		List<VersionDetailsJob> deleteJobList = new ArrayList<>();
		Set<ObjectId> udId = new HashSet<>();
		udId.add(collector.getId());
		for (VersionDetailsJob job : versionDetailsJobRepository.findByCollectorIdIn(udId)) {
			if (!job.getCollectorId().equals(collector.getId())) {
				deleteJobList.add(job);
			}
		}

		versionDetailsJobRepository.delete(deleteJobList);

	}

	private List<File> readFilesInDirectory(Path path)
	{
		try (Stream<Path> paths = Files.walk(path))
		{
			return paths
					.filter(Files::isRegularFile)
					.map(Path::toFile)
					.collect(Collectors.toList());
		}
		catch (IOException e)
		{
			log("readFilesInDirectory exception: " + e);
		}
		return Collections.emptyList();
	}

	private void addNewJobs(Set<VersionDetailsJob> buildsByJob, VersionCollector collector) {
		long start = System.currentTimeMillis();
		int count = 0;
		for (VersionDetailsJob job : buildsByJob) {
			if (isNewJob(collector, job)) {
				log("adding new job");
				job.setCollectorId(collector.getId());
				job.setEnabled(false); //By default it is disabled it will turn on when added to UI
				job.setDescription(job.getJobName());
				versionDetailsJobRepository.save(job);
				count++;
			}
			else {
				log("Ignore adding Job as it already exists");
			}
		}
		log("New jobs", start, count);
	}

	private void addJobDetails(List<VersionDetailsJob> enabledJobs, Map<VersionDetailsJob, Set<Build>> buildsByJob) {
		for (VersionDetailsJob job : enabledJobs) {
			Set<Build> builds = buildsByJob.get(job);

			if(builds == null) {
				log("builds.isEmpty ");
				continue;
			}

			for(Build build: builds) {
				LOG.info("Getting Versions from : "+ build.getBuildUrl());
				VersionData versions = versionClient.getVersionDetails(job.getJobName(), 
						build.getNumber(),
						versionSettings.getFilePath(),
						build.getBuildUrl());

				if(versions!=null) {
					if(isNewBuild(versions)) {
						versions.setCollectorItemId(job.getId());
						versionDataRepository.save(versions);
						LOG.info("Saved to DB");
					}else {
						LOG.info("The build "+versions.getJobName()+" with build number "+versions.getBuildId()+" already exists");
					}
					try{
						LOG.info("Deleting file : "+versionSettings.getFilePath()+"/"+build.getBuildUrl());
						File file = new File(versionSettings.getFilePath()+"/"+build.getBuildUrl());
						if(file.delete()){
							LOG.info(file.getName() + " is deleted!");
						}else{
							LOG.info("Delete operation is failed.");
						}
					}catch(Exception e){
						LOG.info("Unable to Perform delete operation");
					}


				}else {
					LOG.info("Unable to fetch build details");
				}
			}
		}
	}

	private List<VersionDetailsJob> enabledJobs(VersionCollector collector) {
		return versionDetailsJobRepository.findEnabledVersionDetailsJobs(collector.getId());
	}

	private boolean isNewJob(VersionCollector collector,
			VersionDetailsJob job) {
		return versionDetailsJobRepository.findByIdAndJobName(collector.getId(), job.getJobName()) == null;
	}

	private boolean isNewBuild(VersionData build) {
		return versionDataRepository.findByJobNameAndBuildId(build.getJobName(),
				build.getBuildId()) == null;
	}
}

