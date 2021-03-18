package com.capitalone.dashboard.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import com.capitalone.dashboard.model.VersionDetailsJob;

public interface VersionDetailsJobRepository extends BaseCollectorItemRepository<VersionDetailsJob> {

	@Query(value="{ 'collectorId' : ?0, options.job_name : ?1}")
	VersionDetailsJob findByIdAndJobName(ObjectId collectorId, String jobName);

	@Query(value="{ 'collectorId' : ?0 }")
	List<VersionDetailsJob> findEnabledVersionDetailsJobs(ObjectId collectorId);
}
