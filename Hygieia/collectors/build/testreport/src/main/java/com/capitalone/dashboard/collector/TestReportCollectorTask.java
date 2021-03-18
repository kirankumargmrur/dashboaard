package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.Build;
//import com.capitalone.dashboard.model.Build;
//import com.capitalone.dashboard.model.TestResult;
//import com.capitalone.dashboard.repository.TestResultRepository;
//import com.capitalone.dashboard.model.CollectorItemConfigHistory;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.TestCapability;
import com.capitalone.dashboard.model.TestCase;
import com.capitalone.dashboard.model.TestCaseStatus;
import com.capitalone.dashboard.model.TestCaseStep;
//import com.capitalone.dashboard.model.JenkinsJob;
//import com.capitalone.dashboard.model.ConfigHistOperationType;
import com.capitalone.dashboard.model.TestReportCollector;
import com.capitalone.dashboard.model.TestReportJob;
import com.capitalone.dashboard.model.TestResult;
import com.capitalone.dashboard.model.TestSuite;
import com.capitalone.dashboard.model.TestSuiteType;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.TestReportCollectorRepository;
import com.capitalone.dashboard.repository.TestReportJobRepository;
import com.capitalone.dashboard.repository.TestResultRepository;

//import org.apache.commons.io.comparator.NameFileComparator;
//import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
//import org.json.JSONObject;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.JSONObject;
import org.json.XML;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
//import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.dataformat.xml.XmlMapper;
//import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
/*import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;*/
//import javax.xml.bind.*; // will be using JAXBContext,UnMarshaller and JAXBException classes from this package
//import org.springframework.web.client.RestClientException;

import java.io.*;
/*import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;*/

import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collections;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
//import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class TestReportCollectorTask extends CollectorTask<TestReportCollector> {
    @SuppressWarnings({ "PMD.UnusedPrivateField", "unused" })
    private static final Log LOG = LogFactory.getLog(TestReportCollectorTask.class);

    private final TestReportCollectorRepository testReportCollectorRepository;
    private final TestReportJobRepository testReportjobRepository;
    private final TestResultRepository testResultRepository;
    private final TestReportSettings testReportSettings;
    private final ComponentRepository dbComponentRepository;
    private static final String DELIMITER = "__";
    private static final String JENKINSURL = "http://jenkinsmaster2.nonprod.avaya.com:8080";
    private static int totalTestCaseCount, skippedTestCaseCount, failedTestCaseCount, successTestCaseCount, unknownTestCaseCount;

    @Autowired
    public TestReportCollectorTask(TaskScheduler taskScheduler,
                              TestReportCollectorRepository testReportCollectorRepository,
                              TestReportJobRepository testReportjobRepository,
                              TestResultRepository testResultRepository,
                              TestReportSettings testReportSettings,
                              //TestReportClient testReportClient,
                              ComponentRepository dbComponentRepository) {
        super(taskScheduler, "TestReport");
        this.testReportCollectorRepository = testReportCollectorRepository;
        this.testReportjobRepository = testReportjobRepository;
        this.testResultRepository = testResultRepository;
        this.testReportSettings = testReportSettings;
        //this.testReportClient = testReportClient;
        this.dbComponentRepository = dbComponentRepository;
    }

    @Override
    public TestReportCollector getCollector() {
        return TestReportCollector.prototype(testReportSettings.getTestReportPaths());
    }

    @Override
    public BaseCollectorRepository<TestReportCollector> getCollectorRepository() {
        return testReportCollectorRepository;
    }

    @Override
    public String getCron() {
        return testReportSettings.getCron();
    }

    @Override
    public void collect(TestReportCollector collector) {
        long start = System.currentTimeMillis();

        log("collect: collectorId: " + collector.getId());
        clean(collector);
        List<File> filesList = readFilesInDirectory(Paths.get(collector.getTestReportPaths()));      	
    	Map<TestReportJob, Set<Build>> buildsByJob = 
                getInstanceJobs(filesList);
        addNewJobs(buildsByJob.keySet(), collector);
                        
        List<TestReportJob> enabledJobs = enabledJobs(collector);
        if (!enabledJobs.isEmpty())
        {
            addNewTestSuites(enabledJobs, buildsByJob, filesList); 
        }
        else
        {
        	log("WARNING: No Enabled Jobs found");
        }
        log("Finished", start);
    }

    /**
     * Clean up unused hudson/jenkins collector items
     *
     * @param collector the collector
     */

    private void clean(TestReportCollector collector) {

        // First delete jobs that will be no longer collected because servers have moved etc.
        deleteUnwantedJobs(collector);

        Set<ObjectId> uniqueIDs = new HashSet<>();
        for (com.capitalone.dashboard.model.Component comp : dbComponentRepository
                .findAll()) {
            if (comp.getCollectorItems() == null
                    || comp.getCollectorItems().isEmpty()) continue;
            List<CollectorItem> itemList = comp.getCollectorItems().get(
                    CollectorType.Test);
            if (itemList == null) continue;
            for (CollectorItem ci : itemList) {
                if (ci != null && ci.getCollectorId().equals(collector.getId())) {
                    uniqueIDs.add(ci.getId());
                }
            }
        }

        List<TestReportJob> jobList = new ArrayList<>();
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        for (TestReportJob job : testReportjobRepository
                .findByCollectorIdIn(udId)) {
            if (job != null) {
                job.setEnabled(uniqueIDs.contains(job.getId()));
                jobList.add(job);
            }
        }
        testReportjobRepository.save(jobList);
    }

    private void deleteUnwantedJobs(TestReportCollector collector) {

        List<TestReportJob> deleteJobList = new ArrayList<>();
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        for (TestReportJob job : testReportjobRepository.findByCollectorIdIn(udId)) {
            if (!job.getCollectorId().equals(collector.getId())) {
                deleteJobList.add(job);
            }
        }

        testReportjobRepository.delete(deleteJobList);

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
    
    /**
     * Adds new {@link TestReportJob}s to the database as disabled jobs.
     *
     * @param jobs      list of {@link TestReportJob}s
     * @param collector the {@link TestReportCollector}
     */
    private void addNewJobs(Set<TestReportJob> jobs,
                            TestReportCollector collector) {
        long start = System.currentTimeMillis();
        int count = 0;

        for (TestReportJob job : jobs) {
            if (isNewJob(collector, job)) {
            	log("adding new job");
                job.setCollectorId(collector.getId());
                job.setEnabled(false); // Do not enable for collection. Will be
                // enabled when added to dashboard
                job.setDescription(job.getJobName());
                testReportjobRepository.save(job);
                count++;
            }
            else {
            	log("Ignore adding Job as it already exists");
            }
        }
        log("New jobs", start, count);
    }
    
    private boolean isNewJob(TestReportCollector collector,
    		TestReportJob job) {
	return testReportjobRepository.findTestReportJob(collector.getId(), job.getJobName()) == null;
	}

    private List<TestReportJob> enabledJobs(TestReportCollector collector) {
        return testReportjobRepository.findEnabledTestReportJobs(collector.getId());
    }
    
    
    private void addNewTestSuites(List<TestReportJob> enabledJobs, Map<TestReportJob, Set<Build>> buildsByJob, List<File> filesList) {
    	log("addNewTestSuites");
        long start = System.currentTimeMillis();
        int count = 0;
        for (TestReportJob job : enabledJobs) {
            //Build buildSummary = testReportClient.getLastSuccessfulBuild();
            // Obtain the Test Result
        	
        	log("job.getJobName() : " + job.getJobName());
        	Set<Build> builds = buildsByJob.get(job);
        	//log("builds size: " + builds.size());
        	if(builds == null) {
        		log("builds.isEmpty ");
        		continue;
        	}
        	for(Build build: builds) {
        		log("Iterate builds by job");
        		try {
	        		for(File filePath: filesList)
	            	{
	        			String[] jobParts = filePath.getName().split(DELIMITER);
	        			if(jobParts.length < 2) {
	        				log("Ignore adding test report" + filePath.getName() + " as TEST TYPE UNKNOWN");
	        				continue;
	        			}
	        				
	    	        	if(isNewTestReportResult(job, build, jobParts[2])) {
	    	        		log("Adding new test result");
	    	        		String enabledJobfileName = job.getJobName() + DELIMITER + build.getNumber() + DELIMITER + jobParts[2] + DELIMITER + jobParts[3] + DELIMITER + ".xml";	
		        			if(filePath.getName().equalsIgnoreCase(enabledJobfileName)) {
		        				TestSuiteType testType = getTestType(jobParts[2]);
		        				String description = job.getJobName();
			            		log("filePath: " + filePath.getName(), start);
			    	        	String line="",str="";
			    	            BufferedReader br = new BufferedReader(new FileReader(filePath));
			    	            while ((line = br.readLine()) != null) 
			    	            {   
			    	                str+=line;  
			    	            }
			    	            String jsondata = XML.toJSONObject(str).toString();
			    	            log("------------------------------------------------------------------------------------");
			    	            log("Test report complete json data: " + jsondata);
			    	            log("------------------------------------------------------------------------------------");
			    	            //JSONParser parser = new JSONParser();
			    	            //for (Object featureObj : (JSONArray) parser.parse(jsondata)) {
			    	            JSONObject buildJson = (JSONObject) new JSONParser().parse(jsondata);
			    	            //Object featureObj = parser.parse(jsondata);
		    	            	//JSONObject feature = (JSONObject) featureObj;
		    	            	log("Test report JSONArray object data: " + buildJson); 
		    	            	JSONObject testsuiteObject = new JSONObject();
			    	            log("------------------------------------------------------------------------------------");
			    	            Object artifactObj  = buildJson.get("testsuites");
			    	            if((artifactObj == null) && (buildJson.get("testsuite")!= null)) {
			    	            	log("No testsuites, but testsuite is present");
			    	            	testsuiteObject = buildJson;
			    	            }
			    	            else {
			    	            	log("Test report testsuites object data: " + artifactObj);	
			    	            	testsuiteObject = (JSONObject) artifactObj;
			    	            }
	
			    	            log("------------------------------------------------------------------------------------");
			    	            TestResult result = getTestReportTestResult(testsuiteObject, description, build.getNumber(), jobParts[3], testType);
			    	            if (result != null) {
			    	            	log("Save test result");
			    	    	        result.setCollectorItemId(job.getId());
			    	    	        result.setTimestamp(Long.valueOf(jobParts[3]));
			    	    	        result.setEndTime(Long.valueOf(jobParts[3]));
			    	    	        testResultRepository.save(result);
			    	    	        boolean deleted = Files.deleteIfExists(filePath.toPath());
			    	    	        if(deleted) {
			    	    	        	log("Removed test report file");
			    	    	        }
			    	    	        count++;
			    	    	    }
			    	           // }
		        			}
	    	        	}
	    	        	else {
	    	        		log("Ignore adding test result as it already exists in DB");
	    	        	}
	            	}
        		}
        		catch (Exception e) {
                	log("Test report exception: " + e.getMessage());
                }
        	}
        }
        log("New test suites", start, count);
    }
    
    private TestSuiteType getTestType(String testType) {
    	TestSuiteType result;
    	switch(testType){
	        case "unit":
	        	result = TestSuiteType.Unit;
	            break;
	        case "functional":
	        	result = TestSuiteType.Functional;
	            break;
	        case "sanity":
	        	result = TestSuiteType.Regression;
	            break;
	        case "integration":
	        	result = TestSuiteType.Integration;
	            break;
	        case "performance":
	        	result = TestSuiteType.Performance;
	            break;
	        case "security":
	        	result = TestSuiteType.Security;
	            break;
	        default:
	        	result = TestSuiteType.Unit;
	            break;
	    }
    	return result; 	
    }
    
    
    private boolean isNewTestReportResult(TestReportJob job, Build build, String testType) {
    	log(" isNewTestReportResult : jobId:" + job.getId() + " build number: " + build.getNumber());
    	return testResultRepository.findByCollectorItemIdAndExecutionIdAndTestCapabilitiesType(
    				job.getId(), build.getNumber(), testType) == null;
    }
    
    private TestResult getTestReportTestResult(JSONObject jsondata, String description, String buildNumber, String timeStamp, TestSuiteType testType) {
        List<TestCapability> capabilities = getCapabilities(jsondata, description, buildNumber, timeStamp, testType);
        return buildTestResultObject(jsondata, buildNumber, timeStamp, capabilities);
    }
    
    private List<TestCapability> getCapabilities(JSONObject buildJson, String description, String buildNumber, String timeStamp, TestSuiteType testType) {
    	log("getCapabilities : start");
        List<TestCapability> capabilities = new ArrayList<>();
        TestCapability cap = new TestCapability();
        cap.setType(testType);
        List<TestSuite> testSuites = transformToTestSuite(buildJson, testType);
        cap.setDescription(description);
        cap.getTestSuites().addAll(testSuites); //add test suites
        long duration = 0;
        int testSuiteSkippedCount = 0, testSuiteSuccessCount = 0, testSuiteFailCount = 0, testSuiteUnknownCount = 0, count = 1;
        for (TestSuite t : testSuites) {
        	log("getCapabilities: testsuite = " + count);
            duration += t.getDuration();
            log("Testsuite duration: " + duration);
            switch (t.getStatus()) {
                case Success:
                    testSuiteSuccessCount++;
                    break;
                case Failure:
                    testSuiteFailCount++;
                    break;
                case Skipped:
                    testSuiteSkippedCount++;
                    break;
                default:
                    testSuiteUnknownCount++;
                    break;
            }
            count++;
        }
        if (testSuiteFailCount > 0) {
            cap.setStatus(TestCaseStatus.Failure);
        } else if (testSuiteSkippedCount > 0) {
            cap.setStatus(TestCaseStatus.Skipped);
        } else if (testSuiteSuccessCount > 0) {
            cap.setStatus(TestCaseStatus.Success);
        } else {
            cap.setStatus(TestCaseStatus.Unknown);
        }
        cap.setFailedTestSuiteCount(testSuiteFailCount);
        cap.setSkippedTestSuiteCount(testSuiteSkippedCount);
        cap.setSuccessTestSuiteCount(testSuiteSuccessCount);
        cap.setUnknownStatusTestSuiteCount(testSuiteUnknownCount);
        cap.setTotalTestSuiteCount(testSuites.size());
        cap.setDuration(duration);
        cap.setExecutionId(buildNumber);
        cap.setTimestamp(Long.valueOf(timeStamp));
        cap.setStartTime(Long.valueOf(timeStamp));
        cap.setEndTime(Long.valueOf(timeStamp));
        capabilities.add(cap);
        return capabilities;
        
    }
    
    
    private List<TestSuite> transformToTestSuite(JSONObject buildJson, TestSuiteType testType) {
    	log("transformToTestSuite : JSON object: " +  buildJson);
    	List<TestSuite> suites = new ArrayList<>();
    	int id=1;
    	Object intervention = buildJson.get("testsuite");
        if (intervention instanceof JSONArray) {
        	log("transformToTestSuite : inputJson is of array type");
        	for (Object testsuite : getJsonArray(buildJson, "testsuite"))
        	{
        		suites.add(parseFeatureAsTestSuite((JSONObject)testsuite, id, testType));
        		id++;
        	}      	   	
        }
        else if(intervention instanceof JSONObject) {
        	log("transformToTestSuite: inputJson is of object type");
        	suites.add(parseFeatureAsTestSuite((JSONObject)intervention, id, testType));
        }
        else {
        	log("Unknown Json type");
        }

    	return suites;
    }
    
    private TestSuite parseFeatureAsTestSuite(JSONObject testsuite, int id, TestSuiteType testType) {
    	log("parseFeatureAsTestSuite : Id: " + id + " Json: " + testsuite);
        TestSuite suite = new TestSuite();
        suite.setId(String.valueOf(id));
        suite.setType(testType);
        suite.setDescription(getString(testsuite, "name"));

        long duration = 0;

        int testCaseTotalCount = 0, testCaseSkippedCount = 0, testCaseSuccessCount = 0, testCaseFailCount = 0, testCaseUnknownCount = 0, idx = 1;

        Object intervention = testsuite.get("testcase");
        if (intervention instanceof JSONArray) {
        	log("testcase intervention is of Json array type");
            testCaseTotalCount = getJsonArray(testsuite, "testcase").size();
            for (Object testcase : getJsonArray(testsuite, "testcase")) {
                TestCase testCase = parseScenarioAsTestCase((JSONObject) testcase, idx);
                duration += testCase.getDuration();
                switch(testCase.getStatus()) {
                    case Success:
                    {
                        testCaseSuccessCount++; successTestCaseCount++;
                        break;
                    }
                    case Failure:
                    {
                        testCaseFailCount++; failedTestCaseCount++;
                        break;
                    }
                    case Skipped:
                    {
                        testCaseSkippedCount++; skippedTestCaseCount++;
                        break;
                    }
                    default:
                    {
                        testCaseUnknownCount++; unknownTestCaseCount++;
                        break;
                    }
                }
                totalTestCaseCount++;
                suite.getTestCases().add(testCase);
                idx++;
            }
        }
        else if (intervention instanceof JSONObject) {
        	log("testcase intervention is of Json object type");
            testCaseTotalCount = 1;
            Object testCaseObj = testsuite.get("testcase");
            TestCase testCase = parseScenarioAsTestCase((JSONObject)testCaseObj, idx);
            duration += testCase.getDuration();
            switch(testCase.getStatus()) {
                case Success:
                {
                    testCaseSuccessCount++; successTestCaseCount++;
                    break;
                }
                case Failure:
                {
                    testCaseFailCount++; failedTestCaseCount++;
                    break;
                }
                case Skipped:
                {
                    testCaseSkippedCount++; skippedTestCaseCount++;
                    break;
                }
                default:
                {
                    testCaseUnknownCount++; unknownTestCaseCount++;
                    break;
                }
            }
            totalTestCaseCount++;
            suite.getTestCases().add(testCase);
        }
        else {
        	log("Unknown Json type");
        }
       
        log("Testcase success count: " + testCaseSuccessCount + " fail count: " + testCaseFailCount + " skipped: " + testCaseSkippedCount + " Total: " + testCaseTotalCount + "Duration: " + duration);
        suite.setSuccessTestCaseCount(testCaseSuccessCount);
        suite.setFailedTestCaseCount(testCaseFailCount);
        suite.setSkippedTestCaseCount(testCaseSkippedCount);
        suite.setTotalTestCaseCount(testCaseTotalCount);
        suite.setUnknownStatusCount(testCaseUnknownCount);
        suite.setDuration(duration);
        if(testCaseFailCount > 0) {
            suite.setStatus(TestCaseStatus.Failure);
        } else if (testCaseSuccessCount > 0){
            suite.setStatus(TestCaseStatus.Success);
        } else if(testCaseSkippedCount > 0) {
            suite.setStatus(TestCaseStatus.Skipped);
        }else {
            suite.setStatus(TestCaseStatus.Unknown);
        }
        return suite;
    }

    private TestCase parseScenarioAsTestCase(JSONObject testcase, int id) {
    	log("parseScenarioAsTestCase : Id = "+id);
        TestCase testCase  = new TestCase();
        testCase.setId(String.valueOf(id));
        testCase.setDescription(getString(testcase, "name"));
        // Parse each step as a TestCase
        int testStepSuccessCount = 0, testStepFailCount = 0, testStepSkippedCount = 0, testStepUnknownCount = 0;
        long testDuration = 0;

        TestCaseStep testCaseStep = parseStepAsTestCaseStep(testcase, id);
        testDuration += testCaseStep.getDuration();
        // Count Statuses
        switch(testCaseStep.getStatus()) {
            case Success:
                testStepSuccessCount++;
                break;
            case Failure:
                testStepFailCount++;
                break;
            case Skipped:
                testStepSkippedCount++;
                break;
            default:
                testStepUnknownCount++;
                break;

        }
        testCase.getTestSteps().add(testCaseStep);

        testCase.setDuration(testDuration);
        testCase.setSuccessTestStepCount(testStepSuccessCount);
        testCase.setSkippedTestStepCount(testStepSkippedCount);
        testCase.setFailedTestStepCount(testStepFailCount);
        testCase.setUnknownStatusCount(testStepUnknownCount);
        testCase.setTotalTestStepCount(testCase.getTestSteps().size());
        // Set Status
        if(testStepFailCount > 0) {
            testCase.setStatus(TestCaseStatus.Failure);
        } else if(testStepSkippedCount > 0) {
            testCase.setStatus(TestCaseStatus.Skipped);
        } else if (testStepSuccessCount > 0){
            testCase.setStatus(TestCaseStatus.Success);
        } else {
            testCase.setStatus(TestCaseStatus.Unknown);
        }
        return testCase;
    }

    private TestCaseStep parseStepAsTestCaseStep(JSONObject stepObject, int id) {
    	log("parseStepAsTestCaseStep : Number = " + id);
        TestCaseStep step  = new TestCaseStep();
        step.setDescription(getString(stepObject, "name"));
        step.setId(String.valueOf(id));
        TestCaseStatus stepStatus = TestCaseStatus.Unknown;
        step.setDuration(getLong(stepObject, "time"));
        log("Testcase: duration = "+ getLong(stepObject, "time"));
        stepStatus = parseStatus(stepObject);
        step.setStatus(stepStatus);
        return step;
    }


    private TestCaseStatus parseStatus(JSONObject result) {
        String status = getString(result, "status");
        log("Testcase status: " + status);
        //Handle test reports from junit format
        
        if(status == null) {
	        if((result.get("error") != null) || (result.get("failure") != null)) {
	        	status = "failed";
	        }
	        else if((result.get("ignored") != null) || (result.get("skipped") != null)) {
	        	status = "skipped";
	        }
	        else {
	        	status = "run";
	        }
        }
        
     
        switch (status) {
            case "Success":
            case "run" :
                return TestCaseStatus.Success;
            case "failed" :
            case "Failure":
                return TestCaseStatus.Failure;
            case "skipped" :
                return TestCaseStatus.Skipped;
            default:
                return TestCaseStatus.Unknown;
        }
    }

    
    private JSONArray getJsonArray(JSONObject json, String key) {
        Object array = json.get(key);
        return array == null ? new JSONArray() : (JSONArray) array;
    }

    private String getString(JSONObject json, String key) {
        return (String) json.get(key);
    }

    private long getLong(JSONObject json, String key) {
        Object obj = json.get(key);
        log("getLong for duration : " + obj);
        return (obj instanceof Number ? ((Number)obj).longValue() : 0);
    }
    
	private Map<TestReportJob, Set<Build>> getInstanceJobs(List<File> filesList) {
        Map<TestReportJob, Set<Build>> result = new LinkedHashMap<>();
        ArrayList<String> jobsReportedSoFar = new ArrayList<String>();
        for(File forJobs: filesList) {
        	LOG.info("getInstanceJobs: filename: "+forJobs.getName());
        	String[] job = forJobs.getName().split(DELIMITER);
           	if(job.length > 0) {
        		if(!jobsReportedSoFar.contains(job[0])) {
        			LOG.info("getInstanceJobs: New Job: "+job[0]);
		        	TestReportJob reportJob = new TestReportJob();
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
					            hudsonBuild.setBuildUrl(forJobs.getName());
					            builds.add(hudsonBuild);
		            		}
		            		else {
		            			log("No Build number available");
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
		        	TestReportJob reportJob = new TestReportJob();
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
		            			log("No Build number available");
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
	
	
    private TestResult buildTestResultObject(JSONObject buildJson, String buildNumber, String timeStamp, List<TestCapability> capabilities) {
    	log("buildTestResultObject");
        if (!capabilities.isEmpty()) {
            // There are test suites so let's construct a TestResult to encapsulate these results
            TestResult testResult = new TestResult();
            log("Testsuites name: " + getString(buildJson, "name"));
            testResult.setDescription(getString(buildJson, "name"));
            testResult.setExecutionId(buildNumber);
            testResult.setUrl(JENKINSURL);

            testResult.setTimestamp(Long.valueOf(timeStamp));
            for(TestCapability cap : capabilities) {
            	testResult.setDuration(cap.getDuration());
                testResult.setStartTime(Long.valueOf(timeStamp) - cap.getDuration());
            }

            testResult.setEndTime(Long.valueOf(timeStamp));
            testResult.getTestCapabilities().addAll(capabilities);  //add all capabilities
            testResult.setTotalCount(totalTestCaseCount);
            testResult.setSuccessCount(successTestCaseCount);
            testResult.setFailureCount(failedTestCaseCount);
            testResult.setSkippedCount(skippedTestCaseCount);
            testResult.setUnknownStatusCount(unknownTestCaseCount);
            totalTestCaseCount = successTestCaseCount = failedTestCaseCount = skippedTestCaseCount = unknownTestCaseCount = 0;
            return testResult;
        }
        return null;
    }
}
