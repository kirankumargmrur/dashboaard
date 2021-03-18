
package com.capitalone.dashboard.repository;

import com.capitalone.dashboard.model.TestReportJob;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TestReportJobRepository extends BaseCollectorItemRepository<TestReportJob> {

    @Query(value="{ 'collectorId' : ?0, options.job_name : ?1}")
    TestReportJob findTestReportJob(ObjectId collectorId, String jobName);

    @Query(value="{ 'collectorId' : ?0, enabled: true}")
    List<TestReportJob> findEnabledTestReportJobs(ObjectId collectorId);
}
