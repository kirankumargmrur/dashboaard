package com.capitalone.dashboard.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import com.capitalone.dashboard.model.VersionData;

public interface VersionDataRepository extends CrudRepository<VersionData, ObjectId>, QueryDslPredicateExecutor<VersionData> {

	@Query(value = "{ 'jobName' : ?0 ,'buildId' : ?1 }")
	VersionData findByJobNameAndBuildId(String jobName, String buildId);

	VersionData findByIdAndJobName(ObjectId id, String jobName);
}
