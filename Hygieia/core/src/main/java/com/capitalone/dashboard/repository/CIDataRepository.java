package com.capitalone.dashboard.repository;

import com.capitalone.dashboard.model.CIData;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Repository for {@link ci_build_data} data.
 */
public interface CIDataRepository extends CrudRepository<CIData, ObjectId>, QueryDslPredicateExecutor<CIData> {

	@Query(value = "{ 'jobName' : ?0 }")
	List<CIData> findCIDatas(String jobName);

	@Query(value = "{ 'jobName' : ?0 ,'buildId' : ?1 }")
	CIData findByJobNameAndBuildId(String jobName, String buildId);


}
