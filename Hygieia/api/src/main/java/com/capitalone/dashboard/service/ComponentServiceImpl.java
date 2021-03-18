package com.capitalone.dashboard.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.ComponentVersions;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.QTestResult;
import com.capitalone.dashboard.model.QVersionData;
import com.capitalone.dashboard.model.TestResult;
import com.capitalone.dashboard.model.VersionData;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.VersionDataRepository;
import com.capitalone.dashboard.request.CollectorRequest;
import com.capitalone.dashboard.request.VersionsRequest;
import com.capitalone.dashboard.request.TestResultRequest;
import com.capitalone.dashboard.request.VersionDataCreateRequest;
import com.google.common.collect.Iterables;
import com.mysema.query.BooleanBuilder;

@Service
public class ComponentServiceImpl implements ComponentVersionService{

	private final VersionDataRepository versionDataRepository;
	private final ComponentRepository componentRepository;
	private final CollectorRepository collectorRepository;
	private final CollectorService collectorService;

	@Autowired
	public ComponentServiceImpl(VersionDataRepository versionDataRepository,
			ComponentRepository componentRepository,
			CollectorRepository collectorRepository,
			CollectorService collectorService) {
		this.versionDataRepository = versionDataRepository;
		this.componentRepository = componentRepository;
		this.collectorRepository = collectorRepository;
		this.collectorService = collectorService;
	}

	@Override
	public DataResponse<Iterable<VersionData>> search(VersionsRequest request) {
		 if (request == null) {
	            return emptyResponse();
	        }

	        if (request.getComponentId() == null) { // return whole model
	            // TODO: but the dataresponse needs changing.. the timestamp breaks this ability.
//	            Iterable<VersionData> concatinatedResult = ImmutableList.of();
//	            for (CodeQualityType type : CodeQualityType.values()) {
//	                request.setType(type);
//	                DataResponse<Iterable<CodeQuality>> result = searchType(request);
//	                Iterables.concat(concatinatedResult, result.getResult());
//	            }
	            return emptyResponse();
	        }

	        return searchType(request);
		
	}
	
	protected DataResponse<Iterable<VersionData>> emptyResponse() {
        return new DataResponse<>(null, System.currentTimeMillis());
    }

	
	public DataResponse<Iterable<VersionData>> searchType(VersionsRequest request) {
        CollectorItem item = getCollectorItem(request);
        if (item == null) {
            return emptyResponse();
        }

        QVersionData versions = new QVersionData("versions");
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(versions.collectorItemId.eq(item.getId()));

        if (request.getNumberOfDays() != null) {
            long endTimeTarget =
                    new LocalDate().minusDays(request.getNumberOfDays()).toDate().getTime();
            builder.and(versions.timestamp.goe(endTimeTarget));
        } else if (request.validDateRange()) {
            builder.and(versions.timestamp.between(request.getDateBegins(), request.getDateEnds()));
        }
        Iterable<VersionData> result;
        if (request.getMax() == null) {
            result = versionDataRepository.findAll(builder.getValue(), versions.timestamp.desc());
        } else {
            PageRequest pageRequest =
                    new PageRequest(0, request.getMax(), Sort.Direction.DESC, "timestamp");
            result = versionDataRepository.findAll(builder.getValue(), pageRequest).getContent();
        }
        String jobName = (String) item.getOptions().get("jobName");
        Collector collector = collectorRepository.findOne(item.getCollectorId());
        long lastExecuted = (collector == null) ? 0 : collector.getLastExecuted();
        return new DataResponse<>(result, lastExecuted);
    }
	protected CollectorItem getCollectorItem(VersionsRequest request) {
        CollectorItem item = null;
        Component component = componentRepository.findOne(request.getComponentId());
        List<CollectorItem> items = component.getCollectorItems().get(CollectorType.Versions);
        if (items != null) {
            item = Iterables.getFirst(items, null);
        }
        return item;
    }
	
	@Override
	public String create(VersionDataCreateRequest request) throws HygieiaException {
		 /**
         * Step 1: create Collector if not there
         * Step 2: create Collector item if not there
         * Step 3: Insert Quality data if new. If existing, update it.
         */
        Collector collector = createCollector();

        if (collector == null) {
            throw new HygieiaException("Failed creating code quality collector.", HygieiaException.COLLECTOR_CREATE_ERROR);
        }

        CollectorItem collectorItem = createCollectorItem(collector, request);

        if (collectorItem == null) {
            throw new HygieiaException("Failed creating code quality collector item.", HygieiaException.COLLECTOR_ITEM_CREATE_ERROR);
        }

        VersionData quality = createVersionData(collectorItem, request);

        if (quality == null) {
            throw new HygieiaException("Failed inserting/updating Quality information.", HygieiaException.ERROR_INSERTING_DATA);
        }

        return quality.getId().toString();
	}

	 private Collector createCollector() {
	        CollectorRequest collectorReq = new CollectorRequest();
	        collectorReq.setName("ComponentVersions");  //for now hardcode it.
	        collectorReq.setCollectorType(CollectorType.Versions);
	        Collector col = collectorReq.toCollector();
	        col.setEnabled(true);
	        col.setOnline(true);
	        col.setLastExecuted(System.currentTimeMillis());
	        return collectorService.createCollector(col);
	    }

	    private CollectorItem createCollectorItem(Collector collector, VersionDataCreateRequest request) throws HygieiaException {
	        CollectorItem tempCi = new CollectorItem();
	        tempCi.setCollectorId(collector.getId());
	        tempCi.setDescription(request.getJobName());
	        tempCi.setPushed(true);
	        tempCi.setLastUpdated(System.currentTimeMillis());
	        Map<String, Object> option = new HashMap<>();
	        option.put("projectName", request.getJobName());
	        tempCi.getOptions().putAll(option);
	        tempCi.setNiceName(request.getNiceName());

	        if (StringUtils.isEmpty(tempCi.getNiceName())) {
	            return collectorService.createCollectorItem(tempCi);
	        }
	        return collectorService.createCollectorItemByNiceNameAndJobName(tempCi, request.getJobName());
	    }

	    private VersionData createVersionData(CollectorItem collectorItem, VersionDataCreateRequest request) {
	    	VersionData quality = versionDataRepository.findByIdAndJobName(collectorItem.getId(), request.getJobName());
	        if (quality == null) {
	            quality = new VersionData();
	        }
	        quality.setCollectorItemId(collectorItem.getId());
	        quality.setBuildId(request.getBuildId());
	        quality.setJobName(request.getJobName());
	        quality.setTimestamp(System.currentTimeMillis());
	        for (ComponentVersions cm : request.getMetrics()) {
	            quality.getComponentVersions().add(cm);
	        }
	        return versionDataRepository.save(quality); // Save = Update (if ID present) or Insert (if ID not there)
	    }

}
