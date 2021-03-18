package com.capitalone.dashboard.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoverityCollector extends Collector {
    private List<String> coverityServers = new ArrayList<>();

    public List<String> getCoverityServers() {
        return coverityServers;
    }

    public static CoverityCollector prototype(List<String> servers) {
        CoverityCollector protoType = new CoverityCollector();
        protoType.setName("Coverity");
        protoType.setCollectorType(CollectorType.CodeQuality);
        protoType.setOnline(true);
        protoType.setEnabled(true);
        if(servers!=null) {
            protoType.getCoverityServers().addAll(servers);
        }
        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put(CoverityProject.INSTANCE_URL,"");
        allOptions.put(CoverityProject.PROJECT_NAME,"");
        allOptions.put(CoverityProject.PROJECT_ID, "");
        protoType.setAllFields(allOptions);

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put(CoverityProject.INSTANCE_URL,"");
        uniqueOptions.put(CoverityProject.PROJECT_NAME,"");
        protoType.setUniqueFields(uniqueOptions);
        return protoType;
    }
}
