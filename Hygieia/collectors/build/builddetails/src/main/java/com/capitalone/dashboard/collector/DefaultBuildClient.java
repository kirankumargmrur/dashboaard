package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.BuildStatus;
import com.capitalone.dashboard.model.CIArtifactData;
import com.capitalone.dashboard.model.CIData;
import com.capitalone.dashboard.model.CommitData;
import com.capitalone.dashboard.model.JiraIssue;
import com.capitalone.dashboard.model.BuildJob;
import com.capitalone.dashboard.util.Supplier;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


/**
 * BambooClient implementation that uses RestTemplate and JSONSimple to
 * fetch information from Bamboo instances.
 */
@Component
public class DefaultBuildClient implements BuildClient {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultBuildClient.class);

	private final RestOperations rest;
	private final BuildSettings settings;

	private static final String JOBS_RESULT_SUFFIX= "rest/api/latest/result/";
	private static final String BUILD_DETAILS_URL_SUFFIX = "?expand=changes.change.files&expand=jiraIssues";
	private static final String CRUCIBLE_DETAILS_URL_SUFFIX ="rest-service/search-v1/reviewsForIssue?jiraKey=";

	@Autowired
	public DefaultBuildClient(Supplier<RestOperations> restOperationsSupplier, BuildSettings settings) {
		this.rest = restOperationsSupplier.get();
		this.settings = settings;
	}
	@SuppressWarnings("PMD.ExcessiveMethodLength")
	@Override
	public String getInstanceJobs(String instanceUrl, String planName, String buildNumber) {
		String buildURL="";
		try {
			//final String planName=this.settings.getPlanKey();
			final String planURL = joinURL(instanceUrl, planName);
			LOG.info("Plan:" + planName);
			LOG.info("PlanURL: " + planURL);
			// In terms of Bamboo this is the plan not job
			BuildJob buildJob = new BuildJob();
			buildJob.setInstanceUrl(instanceUrl);
			buildJob.setJobName(planName);
			buildJob.setJobUrl(planURL);

			// Finding out the results of the top-level plan 
			String resultUrl = joinURL(instanceUrl,JOBS_RESULT_SUFFIX);
			resultUrl = joinURL(resultUrl,planName);
			LOG.info("Job:" + planName);
			LOG.info("Result URL:"+ resultUrl);
			//int buildID=this.settings.getBuildNumber();
			//String buildNumber=Integer.toString(buildID);
			buildURL = resultUrl+"-"+buildNumber;
			CIData buildDetails= new CIData();
			buildDetails.setBuildId(buildNumber);
			buildDetails.setBuildURL(buildURL);
		} catch (MalformedURLException mfe) {
			LOG.error("malformed url for loading jobs", mfe);
		}

		return buildURL;
	}

	@Override
	public CIData getBuildDetails(String buildUrl, String instanceUrl, String jobName , String jenkinsBuildId) {
		try {
			LOG.info("Getting build details for "+buildUrl);
			String newUrl = rebuildJobUrl(buildUrl, instanceUrl);
			String url = joinURL(newUrl, BUILD_DETAILS_URL_SUFFIX);

			//            LOG.info("Build Details URL:"+ url);
			ResponseEntity<String> result = makeRestCall(url);
			String resultJSON = result.getBody();
			//            LOG.info("Build Details :"+ resultJSON);
			if (StringUtils.isEmpty(resultJSON)) {
				LOG.error("Error getting build details for. URL=" + url);
				return null;
			}
			JSONParser parser = new JSONParser();
			try {
				JSONObject buildJson = (JSONObject) parser.parse(resultJSON);
				Boolean finished = (Boolean) buildJson.get("finished");
				// Ignore jobs that are building
				if(finished) {
					CIData buildDetails=new CIData();
					buildDetails.setTimestamp(System.currentTimeMillis());
					buildDetails.setBuildId(jenkinsBuildId);
					buildDetails.setJobName(jobName);
					buildDetails.setBuildStatus(getBuildStatus(buildJson));
					String buildReason=buildJson.get("buildReason").toString();
					buildDetails.setBuildAuthor(buildReason.replaceAll("<a?.*?>", ""));
					addJiraIssue(buildDetails, buildJson);
					addCommitDetails(buildDetails, buildJson);
					addArtifactDetails(buildDetails, buildJson);
					return buildDetails;

				}
			} catch (ParseException e) {
				LOG.error("Parsing build: " + buildUrl, e);
			}
		} catch (RestClientException rce) {
			LOG.error("Client exception loading build details: " + rce.getMessage() + ". URL =" + buildUrl );
		} catch (MalformedURLException mfe) {
			LOG.error("Malformed url for loading build details" + mfe.getMessage() + ". URL =" + buildUrl );
		} catch (URISyntaxException use) {
			LOG.error("Uri syntax exception for loading build details"+ use.getMessage() + ". URL =" + buildUrl );
		} catch (RuntimeException re) {
			LOG.error("Unknown error in getting build details. URL="+ buildUrl, re);
		} catch (UnsupportedEncodingException unse) {
			LOG.error("Unsupported Encoding Exception in getting build details. URL=" + buildUrl, unse);
		}
		return null;
	}


	//This method will rebuild the API endpoint 
	public static String rebuildJobUrl(String build, String server) throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {
		URL instanceUrl = new URL(server);
		String userInfo = instanceUrl.getUserInfo();
		String instanceProtocol = instanceUrl.getProtocol();

		//decode to handle spaces in the job name.
		URL buildUrl = new URL(URLDecoder.decode(build, "UTF-8"));
		String buildPath = buildUrl.getPath();

		String host = buildUrl.getHost();
		int port = buildUrl.getPort();
		URI newUri = new URI(instanceProtocol, userInfo, host, port, buildPath, null, null);
		return newUri.toString();
	}


	/**
	 * Grabs jira information for the given build.
	 *
	 * @param build     a Build
	 * @param buildJson the build JSON object
	 */
	private void addJiraIssue(CIData buildDetails, JSONObject buildJson ) {
		String crucibleId="";
		try {
			LOG.info("Adding Jira details and respective Crucible Id");
			JSONObject jiraDetails = (JSONObject) buildJson.get("jiraIssues");
			for (Object item : getJsonArray(jiraDetails, "issue")) {
				JSONObject jsonItem = (JSONObject) item;
				JiraIssue jiras = new JiraIssue();
				jiras.setJiraId(getString(jsonItem, "key"));
				JSONObject jiraUrl=(JSONObject)jsonItem.get("url");
				jiras.setUrl(getString(jiraUrl, "href"));
				jiras.setJiraDescription(getString(jsonItem, "summary"));
				jiras.setJiraState(getString(jsonItem, "status"));
				String jiraKey=jsonItem.get("key").toString();
				JSONParser parser = new JSONParser();
				String crucibleServer = joinURL(this.settings.getCrucibleUrl(), CRUCIBLE_DETAILS_URL_SUFFIX+jiraKey);
				ResponseEntity<String> crucibleDetails = makeRestCall(crucibleServer);
				String crucibleResultJSON = crucibleDetails.getBody();
				try{
					JSONObject crucibleJson = (JSONObject) parser.parse(crucibleResultJSON);
					JSONArray arr=(JSONArray)crucibleJson.get("reviewData");

					if(arr.size()!=0) {
						JSONObject jsonBuilding = (JSONObject) arr.get(0);
						String crucibleKey= jsonBuilding.get("permaId").toString();
						JSONObject crucibleIdJson = (JSONObject) parser.parse(crucibleKey);
						crucibleId=crucibleIdJson.get("id").toString();
						jiras.setCrucible(crucibleId);
					}
					else {
						LOG.info("Jira "+jiraKey+" does not have any reviews");
					}
					buildDetails.getJiras().add(jiras);
				} catch (ParseException e) {
					LOG.info("Unable to parse the Jira json object");
				}
			}
		} catch (MalformedURLException e) {
			LOG.info("Inavlid url :  "+this.settings.getCrucibleUrl());
		}
	}

	/**
	 * Grabs commit information for the given build.
	 *
	 * @param buildDetails     a CIData
	 * @param buildJson the build JSON object
	 */
	private void addCommitDetails(CIData buildDetails, JSONObject buildJson) {
		LOG.info("Adding commit details");
		JSONObject jiraDetails = (JSONObject) buildJson.get("changes");
		for (Object item : getJsonArray(jiraDetails, "change")) {
			JSONObject jsonItem = (JSONObject) item;
			CommitData changes = new CommitData();
			changes.setCommitURL(getString(jsonItem, "commitUrl"));
			changes.setCommitId(getString(jsonItem, "changesetId"));
			changes.setCommitDescription(getString(jsonItem, "comment"));
			changes.setCommitAuthor(getString(jsonItem, "fullName"));
			changes.setCommitDate(getString(jsonItem, "date"));
			buildDetails.getCommits().add(changes);
		}
	}

	/**
	 * Grabs commit information for the given build.
	 *
	 * @param buildDetails     a CIData
	 * @param buildJson the build JSON object
	 */
	private void addArtifactDetails(CIData buildDetails, JSONObject buildJson) {
		LOG.info("Adding artifacts details");
		JSONObject artifactDetails= (JSONObject) buildJson.get("artifacts");
		for (Object item : getJsonArray(artifactDetails, "artifact")) {
			JSONObject jsonItem = (JSONObject) item;
			CIArtifactData artifacts = new CIArtifactData();
			artifacts.setArtifactDescription(getString(jsonItem, "producerJobKey"));
			artifacts.setArtifactName(getString(jsonItem, "name"));
			JSONObject artifactUrl=(JSONObject)jsonItem.get("link");
			artifacts.setArtifactURL(getString(artifactUrl, "href"));
			buildDetails.getArtifacts().add(artifacts);
		}
	}

	////// Helpers

	private String getString(JSONObject json, String key) {
		return (String) json.get(key);
	}

	private JSONArray getJsonArray(JSONObject json, String key) {
		Object array = json.get(key);
		return array == null ? new JSONArray() : (JSONArray) array;
	}

	private BuildStatus getBuildStatus(JSONObject buildJson) {
		String status = buildJson.get("buildState").toString();
		switch (status) {
		case "Successful":
			return BuildStatus.Success;
		case "UNSTABLE":
			return BuildStatus.Unstable;
		case "Failed":
			return BuildStatus.Failure;
		case "ABORTED":
			return BuildStatus.Aborted;
		default:
			return BuildStatus.Unknown;
		}
	}

	protected ResponseEntity<String> makeRestCall(String sUrl) throws MalformedURLException {
		URI thisuri = URI.create(sUrl);
		String userInfo = thisuri.getUserInfo();

		//get userinfo from URI or settings (in spring properties)
		if (StringUtils.isEmpty(userInfo) && (this.settings.getUsername() != null) && (this.settings.getApiKey() != null)) {
			userInfo = this.settings.getUsername() + ":" + this.settings.getApiKey();
		}
		// Basic Auth only.
		if (StringUtils.isNotEmpty(userInfo)) {
			return rest.exchange(thisuri, HttpMethod.GET,
					new HttpEntity<>(createHeaders(userInfo)),
					String.class);
		} else {
			return rest.exchange(thisuri, HttpMethod.GET, null,
					String.class);
		}

	}

	protected HttpHeaders createHeaders(final String userInfo) {
		byte[] encodedAuth = Base64.encodeBase64(
				userInfo.getBytes(StandardCharsets.US_ASCII));
		String authHeader = "Basic " + new String(encodedAuth);

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, authHeader);
		headers.set(HttpHeaders.ACCEPT,"application/json");
		return headers;
	}

	protected String getLog(String buildUrl) {
		try {
			return makeRestCall(joinURL(buildUrl, "consoleText")).getBody();
		} catch (MalformedURLException mfe) {
			LOG.error("malformed url for build log", mfe);
		}

		return "";
	}

	// join a base url to another path or paths - this will handle trailing or non-trailing /'s
	public static String joinURL(String base, String... paths) throws MalformedURLException {
		StringBuilder result = new StringBuilder(base);
		for (String path : paths) {
			String p = path.replaceFirst("^(\\/)+", "");
			if (result.lastIndexOf("/") != result.length() - 1) {
				result.append('/');
			}
			result.append(p);
		}
		return result.toString();
	}
}
