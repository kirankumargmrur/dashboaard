package com.capitalone.dashboard.model;

//import java.util.ArrayList;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;

public class TestReportCollector extends Collector {
    private String testReportPaths = "//opt//HygieiaReports//";

    public String getTestReportPaths() {
        return testReportPaths;
    }

    public static TestReportCollector prototype(String paths) {
        TestReportCollector protoType = new TestReportCollector();
        protoType.setName("TestReport");
        protoType.setCollectorType(CollectorType.Test);
        protoType.setOnline(true);
        protoType.setEnabled(true);

        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put(TestReportJob.JOB_NAME,"");
        protoType.setAllFields(allOptions);

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put(TestReportJob.JOB_NAME,"");
        protoType.setUniqueFields(uniqueOptions);
        return protoType;
    }
}
