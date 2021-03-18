package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CollectorItemConfigHistory;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.ConfigHistOperationType;
import com.capitalone.dashboard.model.CoverityCollector;
import com.capitalone.dashboard.model.CoverityProject;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.CoverityCollectorRepository;
import com.capitalone.dashboard.repository.CoverityProfileRepository;
import com.capitalone.dashboard.repository.CoverityProjectRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CoverityCollectorTask extends CollectorTask<CoverityCollector> {
    @SuppressWarnings({ "PMD.UnusedPrivateField", "unused" })
    private static final Log LOG = LogFactory.getLog(CoverityCollectorTask.class);

    private final CoverityCollectorRepository coverityCollectorRepository;
    private final CoverityProjectRepository coverityProjectRepository;
    private final CodeQualityRepository codeQualityRepository;
    private final CoverityProfileRepository coverityProfileRepository;
    private final CoveritySettings coveritySettings;
    private final DefaultCoverityClient coverityClient;
    private final ComponentRepository dbComponentRepository;

    @Autowired
    public CoverityCollectorTask(TaskScheduler taskScheduler,
                              CoverityCollectorRepository coverityCollectorRepository,
                              CoverityProjectRepository coverityProjectRepository,
                              CodeQualityRepository codeQualityRepository,
                              CoverityProfileRepository coverityProfileRepository,
                              CoveritySettings coveritySettings,
                              DefaultCoverityClient coverityClient,
                              ComponentRepository dbComponentRepository) {
        super(taskScheduler, "Coverity");
        this.coverityCollectorRepository = coverityCollectorRepository;
        this.coverityProjectRepository = coverityProjectRepository;
        this.codeQualityRepository = codeQualityRepository;
        this.coverityProfileRepository = coverityProfileRepository;
        this.coveritySettings = coveritySettings;
        this.coverityClient = coverityClient;
        this.dbComponentRepository = dbComponentRepository;
    }

    @Override
    public CoverityCollector getCollector() {
        return CoverityCollector.prototype(coveritySettings.getServers());
    }

    @Override
    public BaseCollectorRepository<CoverityCollector> getCollectorRepository() {
        return coverityCollectorRepository;
    }

    @Override
    public String getCron() {
        return coveritySettings.getCron();
    }

    @Override
    public void collect(CoverityCollector collector) {
        long start = System.currentTimeMillis();
        LOG.debug("coverity collector Id: " + collector.getId());
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        List<CoverityProject> existingProjects = coverityProjectRepository.findByCollectorIdIn(udId);
        List<CoverityProject> latestProjects = new ArrayList<>();
        clean(collector, existingProjects);

        if (!CollectionUtils.isEmpty(collector.getCoverityServers())) {
            
            for (int i = 0; i < collector.getCoverityServers().size(); i++) {

                String instanceUrl = collector.getCoverityServers().get(i);

                logBanner(instanceUrl);
                List<CoverityProject> projects = coverityClient.getProjects(instanceUrl);
                latestProjects.addAll(projects);

                int projSize = ((CollectionUtils.isEmpty(projects)) ? 0 : projects.size());
                log("Fetched projects   " + projSize, start);
                addNewProjects(projects, existingProjects, collector);
                refreshData(enabledProjects(collector, instanceUrl), coverityClient);
                log("Finished", start);
            }
        }
        deleteUnwantedJobs(latestProjects, existingProjects, collector);
    }

	/**
	 * Clean up unused coverity collector items
	 *
	 * @param collector
	 *            the {@link CoverityCollector}
	 */

    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts") // agreed PMD, fixme
    private void clean(CoverityCollector collector, List<CoverityProject> existingProjects) {
        Set<ObjectId> uniqueIDs = new HashSet<>();
        for (com.capitalone.dashboard.model.Component comp : dbComponentRepository
                .findAll()) {
            if (comp.getCollectorItems() != null && !comp.getCollectorItems().isEmpty()) {
                List<CollectorItem> itemList = comp.getCollectorItems().get(
                        CollectorType.CodeQuality);
                if (itemList != null) {
                    for (CollectorItem ci : itemList) {
                        if (ci != null && ci.getCollectorId().equals(collector.getId())) {
                            uniqueIDs.add(ci.getId());
                        }
                    }
                }
            }
        }
        List<CoverityProject> stateChangeJobList = new ArrayList<>();
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        for (CoverityProject job : existingProjects) {
            // collect the jobs that need to change state : enabled vs disabled.
            if ((job.isEnabled() && !uniqueIDs.contains(job.getId())) ||  // if it was enabled but not on a dashboard
                    (!job.isEnabled() && uniqueIDs.contains(job.getId()))) { // OR it was disabled and now on a dashboard
                job.setEnabled(uniqueIDs.contains(job.getId()));
                stateChangeJobList.add(job);
            }
        }
        if (!CollectionUtils.isEmpty(stateChangeJobList)) {
            coverityProjectRepository.save(stateChangeJobList);
        }
    }


    private void deleteUnwantedJobs(List<CoverityProject> latestProjects, List<CoverityProject> existingProjects, CoverityCollector collector) {
        List<CoverityProject> deleteJobList = new ArrayList<>();

        // First delete collector items that are not supposed to be collected anymore because the servers have moved(?)
        for (CoverityProject job : existingProjects) {
            if (job.isPushed()) continue; // do not delete jobs that are being pushed via API
            if (!collector.getCoverityServers().contains(job.getInstanceUrl()) ||
                    (!job.getCollectorId().equals(collector.getId())) ||
                    (!latestProjects.contains(job))) {
                deleteJobList.add(job);
            }
        }
        if (!CollectionUtils.isEmpty(deleteJobList)) {
            coverityProjectRepository.delete(deleteJobList);
        }
    }

   private void refreshData(List<CoverityProject> coverityProjects, DefaultCoverityClient coverityClient) {
       long start = System.currentTimeMillis();
       int count = 0;

       LOG.info("refreshData ");
       for (CoverityProject project : coverityProjects) {
           LOG.info("refreshData for project: "+ project.getProjectName());
           CodeQuality codeQuality = coverityClient.currentCodeQuality(project);
           if (codeQuality != null && isNewQualityData(project, codeQuality)) {
               codeQuality.setCollectorItemId(project.getId());
               codeQualityRepository.save(codeQuality);
               count++;
           }
       }
       log("Updated", start, count);
    }
    
    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    private void fetchQualityProfileConfigChanges(CoverityCollector collector,String instanceUrl,DefaultCoverityClient coverityClient) throws org.json.simple.parser.ParseException{
    	JSONArray qualityProfiles = coverityClient.getQualityProfiles(instanceUrl);   
    	JSONArray sonarProfileConfigurationChanges = new JSONArray();
        
    	for (Object qualityProfile : qualityProfiles ) {      	
    		JSONObject qualityProfileJson = (JSONObject) qualityProfile;
    		String qualityProfileKey = (String)qualityProfileJson.get("key");

    		List<String> sonarProjects = coverityClient.retrieveProfileAndProjectAssociation(instanceUrl,qualityProfileKey);
    		if (sonarProjects != null){
    			sonarProfileConfigurationChanges = coverityClient.getQualityProfileConfigurationChanges(instanceUrl,qualityProfileKey);
    			addNewConfigurationChanges(collector,sonarProfileConfigurationChanges);
    		}
    	}
    }
    
    private void addNewConfigurationChanges(CoverityCollector collector,JSONArray coverityProfileConfigurationChanges){
    	ArrayList<CollectorItemConfigHistory> profileConfigChanges = new ArrayList();
    	
    	for (Object configChange : coverityProfileConfigurationChanges) {		
    		JSONObject configChangeJson = (JSONObject) configChange;
    		CollectorItemConfigHistory profileConfigChange = new CollectorItemConfigHistory();
    		Map<String,Object> changeMap = new HashMap<String,Object>();
    		
    		profileConfigChange.setCollectorItemId(collector.getId());
    		profileConfigChange.setUserName((String) configChangeJson.get("authorName"));
    		profileConfigChange.setUserID((String) configChangeJson.get("authorLogin") );
    		changeMap.put("event", configChangeJson);
   
    		profileConfigChange.setChangeMap(changeMap);
    		
    		ConfigHistOperationType operation = determineConfigChangeOperationType((String)configChangeJson.get("action"));
    		profileConfigChange.setOperation(operation);
    		
				
    		long timestamp = convertToTimestamp((String) configChangeJson.get("date"));
    		profileConfigChange.setTimestamp(timestamp);
    		
    		if (isNewConfig(collector.getId(),(String) configChangeJson.get("authorLogin"),operation,timestamp)) {
    			profileConfigChanges.add(profileConfigChange);
    		}
    	}
    	coverityProfileRepository.save(profileConfigChanges);
    }
    
    private Boolean isNewConfig(ObjectId collectorId,String authorLogin,ConfigHistOperationType operation,long timestamp) {
    	List<CollectorItemConfigHistory> storedConfigs = coverityProfileRepository.findProfileConfigChanges(collectorId, authorLogin,operation,timestamp);
    	return storedConfigs.isEmpty();
    }
    
    private List<CoverityProject> enabledProjects(CoverityCollector collector, String instanceUrl) {
        return coverityProjectRepository.findEnabledProjects(collector.getId(), instanceUrl);
    }

    private void addNewProjects(List<CoverityProject> projects, List<CoverityProject> existingProjects, CoverityCollector collector) {
        long start = System.currentTimeMillis();
        int count = 0;
        List<CoverityProject> newProjects = new ArrayList<>();
        for (CoverityProject project : projects) {
            if (!existingProjects.contains(project)) {
                project.setCollectorId(collector.getId());
                project.setEnabled(false);
                project.setDescription(project.getProjectName());
                newProjects.add(project);
                count++;
            }
        }
        //save all in one shot
        if (!CollectionUtils.isEmpty(newProjects)) {
            coverityProjectRepository.save(newProjects);
        }
        log("New projects", start, count);
    }

    @SuppressWarnings("unused")
	private boolean isNewProject(CoverityCollector collector, CoverityProject application) {
        return coverityProjectRepository.findCoverityProject(
                collector.getId(), application.getInstanceUrl(), application.getProjectId()) == null;
    }

    private boolean isNewQualityData(CoverityProject project, CodeQuality codeQuality) {
        return codeQualityRepository.findByCollectorItemIdAndTimestamp(
                project.getId(), codeQuality.getTimestamp()) == null;
    }
    
    private long convertToTimestamp(String date) {
    	
    	DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    	DateTime dt = formatter.parseDateTime(date);
    	long d = new DateTime(dt).getMillis();
    	
    	return d;	
    }
    
    private ConfigHistOperationType determineConfigChangeOperationType(String changeAction){
    	switch (changeAction) {
		
	    	case "DEACTIVATED":
	    		return ConfigHistOperationType.DELETED;
	    		
	    	case "ACTIVATED":
	    		return ConfigHistOperationType.CREATED;
	    	default:
	    		return ConfigHistOperationType.CHANGED;
    	}	
    }

}
