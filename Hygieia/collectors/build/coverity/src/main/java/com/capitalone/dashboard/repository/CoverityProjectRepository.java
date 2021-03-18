package com.capitalone.dashboard.repository;

import com.capitalone.dashboard.model.CoverityProject;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CoverityProjectRepository extends BaseCollectorItemRepository<CoverityProject> {

    @Query(value="{ 'collectorId' : ?0, options.instanceUrl : ?1, options.projectId : ?2}")
    CoverityProject findCoverityProject(ObjectId collectorId, String instanceUrl, String projectId);

    @Query(value="{ 'collectorId' : ?0, options.instanceUrl : ?1, enabled: true}")
    List<CoverityProject> findEnabledProjects(ObjectId collectorId, String instanceUrl);
}
