package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CodeQualityMetric;
import com.capitalone.dashboard.model.CodeQualityMetricStatus;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.CoverityProject;
import com.capitalone.dashboard.util.Supplier;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component("DefaultSonarClient")
public class DefaultCoverityClient implements CoverityClient {
    private static final Log LOG = LogFactory.getLog(DefaultCoverityClient.class);

    protected static final String URL_LIST_RESOURCES = "/api/views/v1";
    protected static final String URL_LIST_PROJECT = "/api/viewContents/projects/v1/";
    protected static final String QUERY_PROJECTID = "?projectId=";
    protected static final String QUERY_ALLROWS = "&rowCount=-1";
    protected static final String ISSUES_BY_PROJECT = "/api/viewContents/issuesByProject/v1/All In Project";
    protected static final String FILES_URL = "/api/viewContents/files/v1/";
    
    protected static final String BLOCKER_VIOLATIONS = "blocker_violations";            
    protected static final String MAJOR_VIOLATIONS = "major_violations";
    protected static final String LINES_OF_CODE = "ncloc";
    protected static final String TOTAL_VIOLATIONS = "violations";
    protected static final String LINE_COVERAGE = "line_coverage";
    protected static final String TECHNICAL_DEBT = "sqale_index";
    protected static final String CRITICAL_VIOLATIONS = "critical_violations";
    
    protected static final String URL_QUALITY_PROFILES = "/api/qualityprofiles/search";
    protected static final String URL_QUALITY_PROFILE_PROJECT_DETAILS = "/api/qualityprofiles/projects?key=";
    protected static final String URL_QUALITY_PROFILE_CHANGES = "/api/qualityprofiles/changelog?profileKey=";

    protected static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    protected static final String ID = "id";
    protected static final String NAME = "name";
    protected static final String KEY = "key";
    protected static final String VERSION = "version";
    protected static final String MSR = "msr";
    protected static final String ALERT = "alert";
    protected static final String ALERT_TEXT = "alert_text";
    protected static final String VALUE = "val";
    protected static final String FORMATTED_VALUE = "frmt_val";
    protected static final String STATUS_WARN = "WARN";
    protected static final String STATUS_ALERT = "ALERT";
    
    
    protected static final String DATE = "date";
    protected static final String PROJECTS = "projects";
    protected static final String LATEST_IN_SNAPSHOTS = "In Latest Snapshot";
    protected static final String FILES = "files";
    
    private String projectViewId = "";
    private String filesViewId = ""; //for counting lines of code coverage
    private String projectListURL = "";
    private JSONArray viewsContentArray;
    private String instanceURL = "";

    protected final RestOperations rest;
    protected final HttpEntity<String> httpHeaders;

    @Autowired
    public DefaultCoverityClient(Supplier<RestOperations> restOperationsSupplier, CoveritySettings settings) {
        this.httpHeaders = new HttpEntity<>(
                this.createHeaders(settings.getUsername(), settings.getPassword())
            );
        this.rest = restOperationsSupplier.get();
    }

    @Override
    public List<CoverityProject> getProjects(String instanceUrl) {
        List<CoverityProject> projects = new ArrayList<>();
        instanceURL = instanceUrl;
        String url = instanceURL + URL_LIST_RESOURCES;

        try {

            Boolean pTypeFound = false;

            JSONArray viewArray = parseAsArray(url, "views");
            
            if(viewArray.isEmpty()) {
            	LOG.error("NO views found in REST reponse");
            	return null;
            }
            
            for (Object view : viewArray) {
            	String viewType = str((JSONObject)view, "type");
            	String snapshots = str((JSONObject)view, "name");
            	//LOG.info("viewType: " + viewType);
            	if(viewType.contains(PROJECTS)) {
            		projectViewId = str((JSONObject)view, "id");
            		projectListURL = instanceUrl + URL_LIST_PROJECT + projectViewId + QUERY_PROJECTID + projectViewId + QUERY_ALLROWS;
            		//LOG.info("Complete URL_LIST_PROJECT: " + projectListURL);
            		pTypeFound = true;
            	}
            	
            	if(viewType.contains(FILES) && snapshots.contains(LATEST_IN_SNAPSHOTS)){
            		filesViewId = str((JSONObject)view, "id");
            		//LOG.info("Found filesViewId: " + filesViewId);
            	}
            }
            
            if(!pTypeFound) {
            	LOG.info("No projects found");
            	return projects;
            }
            
            JSONObject viewContents = parseAsObject(projectListURL, "viewContentsV1");
            viewsContentArray = (JSONArray)viewContents.get("rows");
            
            for (Object view : viewsContentArray) {
            	String projectName = str((JSONObject)view, "project");
            	LOG.info("Project : " + projectName);
                CoverityProject project = new CoverityProject();
                project.setInstanceUrl(instanceURL);
                project.setProjectId(projectName);
                project.setProjectName(projectName);
                projects.add(project);        	
            }
            return projects;

        } catch (ParseException e) {
            LOG.error("Could not parse response from: " + url, e);
        } catch (RestClientException rce) {
            LOG.error(rce);
        }
        return null;
    }


    @Override
    public CodeQuality currentCodeQuality(CoverityProject project) {
    	//LOG.info("currentCodeQuality for project: " + project.getProjectId());
    	String url = project.getInstanceUrl() + ISSUES_BY_PROJECT + QUERY_PROJECTID + project.getProjectId() + QUERY_ALLROWS;

        try {
            JSONObject jsonObject = parseAsObject(url, "viewContentsV1");
            
            //LOG.info("Total issues reported: " + str(jsonObject, "totalRows"));
            JSONArray jsonArray = (JSONArray)jsonObject.get("rows");
            CodeQuality codeQuality = new CodeQuality();
            codeQuality.setName(project.getProjectName());
            codeQuality.setUrl(url);
            codeQuality.setType(CodeQualityType.StaticAnalysis);
            for (Object view : viewsContentArray) {
            	String projectName = str((JSONObject)view, "project");
            	if(projectName.matches(project.getProjectName())) {
            		//LOG.info("PROJECT timestamp: " + timestamp((JSONObject)view, "lastSnapshotDate"));
            		codeQuality.setTimestamp(timestamp((JSONObject)view, "lastSnapshotDate"));
            	}
            }
            codeQuality.setVersion(project.getProjectName());
            codeQuality = updateMetricsQuality(codeQuality, jsonArray, project.getProjectName());        

            return codeQuality;
            
        } catch (ParseException e) {
            LOG.error("Could not parse response from: " + url, e);
        } catch (RestClientException rce) {
            LOG.error(rce);
        }

        return null;
    }
    
    public JSONArray getQualityProfiles(String instanceUrl) throws ParseException {
    	String url = instanceUrl + URL_QUALITY_PROFILES;
    	try {
    		JSONArray qualityProfileData = parseAsArray(url,"profiles");
    		return qualityProfileData;
    	} catch (ParseException e) {
    		LOG.error("Could not parse response from: " + url, e);
    		throw e;
    	} catch (RestClientException rce) {
    		LOG.error(rce);
    		throw rce;
    	}
    }
    
    public List<String> retrieveProfileAndProjectAssociation(String instanceUrl,String qualityProfile) throws ParseException{
    	List<String> projects = new ArrayList<>();
    	String url = instanceUrl + URL_QUALITY_PROFILE_PROJECT_DETAILS + qualityProfile;
    	try {
    		JSONArray associatedProjects = this.parseAsArray(url, "results");
    		if (!CollectionUtils.isEmpty(associatedProjects)) {
    			for (Object project : associatedProjects) {
    				JSONObject projectJson = (JSONObject) project;
    				String projectName = (String) projectJson.get("name");
    				projects.add(projectName);
    			}
    			return projects;
    		}
    		return null;
    	} catch (ParseException e) {
    		LOG.error("Could not parse response from: " + url, e);
    		throw e;
    	} catch (RestClientException rce) {
    		LOG.error(rce);
    		throw rce;
    	}
    }
    
   public JSONArray getQualityProfileConfigurationChanges(String instanceUrl,String qualityProfile) throws ParseException{
	   String url = instanceUrl + URL_QUALITY_PROFILE_CHANGES + qualityProfile;
	   try {
		   JSONArray qualityProfileConfigChanges = this.parseAsArray(url, "events");
		   return qualityProfileConfigChanges;
	   } catch (ParseException e) {
		   LOG.error("Could not parse response from: " + url, e);
		   throw e;
	   } catch (RestClientException rce) {
		   LOG.error(rce);
		   throw rce;
	   }
   }

   protected JSONObject parseAsObject(String url, String key) throws ParseException {
   	   //LOG.info("GET Query: " + url);
       ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, this.httpHeaders, String.class);
       JSONParser jsonParser = new JSONParser();
       JSONObject jsonObject = (JSONObject) jsonParser.parse(response.getBody());
       //LOG.info("HTTP GET Response : " + response.getBody());
       return (JSONObject) jsonObject.get(key);
   }
   
    protected JSONArray parseAsArray(String url) throws ParseException {
    	//LOG.info("GET Query: " + url);
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, this.httpHeaders, String.class);
        //LOG.info("HTTP GET Response : " + response.getBody());
        return (JSONArray) new JSONParser().parse(response.getBody());
    }

    protected JSONArray parseAsArray(String url, String key) throws ParseException {
    	//LOG.info("GET Query: " + url);
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, this.httpHeaders, String.class);
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(response.getBody());
        //LOG.info("HTTP GET Response : " + response.getBody());
        return (JSONArray) jsonObject.get(key);
    }

    protected long timestamp(JSONObject json, String key) {
        Object obj = json.get(key);
        if (obj != null) {
            try {
                return new SimpleDateFormat(DATE_FORMAT).parse(obj.toString()).getTime();
            } catch (java.text.ParseException e) {
                LOG.error(obj + " is not in expected format " + DATE_FORMAT, e);
            }
        }
        return 0;
    }

    protected String str(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : obj.toString();
    }
    @SuppressWarnings("unused")
    protected Integer integer(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : (Integer) obj;
    }

    @SuppressWarnings("unused")
    protected BigDecimal decimal(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : new BigDecimal(obj.toString());
    }

    @SuppressWarnings("unused")
    protected Boolean bool(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Boolean.valueOf(obj.toString());
    }

    protected CodeQualityMetricStatus metricStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return CodeQualityMetricStatus.Ok;
        }

        switch(status) {
            case STATUS_WARN:  return CodeQualityMetricStatus.Warning;
            case STATUS_ALERT: return CodeQualityMetricStatus.Alert;
            default:           return CodeQualityMetricStatus.Ok;
        }
    }

    private final HttpHeaders createHeaders(String username, String password){
        HttpHeaders headers = new HttpHeaders();
        if (username != null && !username.isEmpty() &&
            password != null && !password.isEmpty()) {
          String auth = username + ":" + password;
          byte[] encodedAuth = Base64.encodeBase64(
              auth.getBytes(Charset.forName("US-ASCII"))
          );
          String authHeader = "Basic " + new String(encodedAuth);
          headers.set("Authorization", authHeader);
        }
        return headers;
    }
    
    private CodeQuality updateMetricsQuality(CodeQuality codeQuality, JSONArray jsonArray, String projectName) {
    	long nCritical = 0 , nBlocker = 0, nMajor = 0, nLOC = 0, nTotalIssues = 0, sqaleIdx = 0;
        for (Object view : jsonArray) {
            String severity = str((JSONObject)view, "displayImpact");
            nTotalIssues++;
        	switch(severity){
    	        case "Medium":
    	        	nCritical++;
    	            break;
    	        case "High":
    	        	nBlocker++;
    	            break;
    	        case "Low":
    	        	nMajor++;
    	            break;
    	        default:
    	        	LOG.info("UNKnown issue type detected: " + severity);
    	        	break;
        	}
        }
        CodeQualityMetric blockerMetric = new CodeQualityMetric(BLOCKER_VIOLATIONS);
        blockerMetric.setValue(nBlocker);
        blockerMetric.setFormattedValue(String.valueOf(nBlocker));
        blockerMetric.setStatus(CodeQualityMetricStatus.Ok);
        blockerMetric.setStatusMessage("");
        codeQuality.getMetrics().add(blockerMetric);   
        
        CodeQualityMetric majorMetric = new CodeQualityMetric(MAJOR_VIOLATIONS);
        majorMetric.setValue(nMajor);
        majorMetric.setFormattedValue(String.valueOf(nMajor));
        majorMetric.setStatus(CodeQualityMetricStatus.Ok);
        majorMetric.setStatusMessage("");
        codeQuality.getMetrics().add(majorMetric);   
        
        String url = instanceURL + FILES_URL + filesViewId + QUERY_PROJECTID + projectName + QUERY_ALLROWS;
        try {
        	JSONObject codeQualityRows = parseAsObject(url, "viewContentsV1");
        	JSONArray jsonRows = (JSONArray)codeQualityRows.get("rows");
            for (Object view : jsonRows) {
            	nLOC = nLOC + Integer.parseInt(str((JSONObject)view, "codeLineCount"));     	
            }
        }
        catch (Exception e) {
    		LOG.error("Could not parse response from: " + url, e);
    		return null;
    	}
        
        CodeQualityMetric clocMetric = new CodeQualityMetric(LINES_OF_CODE);
        clocMetric.setValue(nLOC);
        clocMetric.setFormattedValue(String.valueOf(nLOC));
        clocMetric.setStatus(CodeQualityMetricStatus.Ok);
        clocMetric.setStatusMessage("");
        codeQuality.getMetrics().add(clocMetric);   
        
        CodeQualityMetric violationsMetric = new CodeQualityMetric(TOTAL_VIOLATIONS);
        violationsMetric.setValue(nTotalIssues);
        violationsMetric.setFormattedValue(String.valueOf(nTotalIssues));
        violationsMetric.setStatus(CodeQualityMetricStatus.Ok);
        violationsMetric.setStatusMessage("");
        codeQuality.getMetrics().add(violationsMetric);   
        
        CodeQualityMetric lineCovMetric = new CodeQualityMetric(LINE_COVERAGE);
        //lineCovMetric.setValue();
        lineCovMetric.setFormattedValue(String.valueOf(""));
        lineCovMetric.setStatus(CodeQualityMetricStatus.Ok);
        lineCovMetric.setStatusMessage("");
        codeQuality.getMetrics().add(lineCovMetric);   
        
        try {
        	Date now = new Date();
        	String date = (new SimpleDateFormat(DATE_FORMAT)).format(now);
	        long currentTime = new SimpleDateFormat(DATE_FORMAT).parse(date).getTime();
	        //LOG.info("Current time: "  + currentTime + "codeQuality time: "  + codeQuality.getTimestamp());
	        sqaleIdx = (currentTime - codeQuality.getTimestamp())/(1000 * 60 * 60 * 24);
        }
        catch (Exception e) {
    		LOG.error("Could not format date: " +  e);
        }
        
        CodeQualityMetric techDebt = new CodeQualityMetric(TECHNICAL_DEBT);
        techDebt.setValue(sqaleIdx);
        techDebt.setFormattedValue(String.valueOf(sqaleIdx));
        techDebt.setStatus(CodeQualityMetricStatus.Ok);
        techDebt.setStatusMessage("");
        codeQuality.getMetrics().add(techDebt);   
        
        //critical_violations
        CodeQualityMetric critcMetric = new CodeQualityMetric(CRITICAL_VIOLATIONS);
        critcMetric.setValue(nCritical);
        critcMetric.setFormattedValue(String.valueOf(nCritical));
        critcMetric.setStatus(CodeQualityMetricStatus.Ok);
        critcMetric.setStatusMessage("");
        codeQuality.getMetrics().add(critcMetric);  
        return codeQuality;
    }
}
