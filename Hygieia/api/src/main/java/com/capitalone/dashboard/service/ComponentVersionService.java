package com.capitalone.dashboard.service;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.VersionData;
import com.capitalone.dashboard.request.VersionsRequest;
import com.capitalone.dashboard.request.VersionDataCreateRequest;

public interface ComponentVersionService {
	
	DataResponse<Iterable<VersionData>> search(VersionsRequest request);

    String create(VersionDataCreateRequest request) throws HygieiaException;

}
