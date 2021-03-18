package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.CoverityProject;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

public interface CoverityClient {

    List<CoverityProject> getProjects(String instanceUrl);
    JSONArray getQualityProfiles(String instanceUrl) throws ParseException;
    List<String> retrieveProfileAndProjectAssociation(String instanceUrl,String qualityProfile) throws ParseException;
    JSONArray getQualityProfileConfigurationChanges(String instanceUrl,String qualityProfile) throws ParseException;
	CodeQuality currentCodeQuality(CoverityProject project); 

}
