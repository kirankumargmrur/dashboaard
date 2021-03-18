package com.capitalone.dashboard.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capitalone.dashboard.editors.CaseInsensitiveTestSuiteTypeEditor;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.CodeQualityType;
import com.capitalone.dashboard.model.DataResponse;
import com.capitalone.dashboard.model.VersionData;
import com.capitalone.dashboard.request.VersionDataCreateRequest;
import com.capitalone.dashboard.request.VersionsRequest;
import com.capitalone.dashboard.service.ComponentVersionService;

@RestController
public class VersionDataController {
	private ComponentVersionService componentVersionService;
	
	 @Autowired
	    public VersionDataController(ComponentVersionService componentVersionService) {
	        this.componentVersionService = componentVersionService;
	    }

	    @InitBinder
	    public void initBinder(WebDataBinder binder) {
	        binder.registerCustomEditor(CodeQualityType.class, new CaseInsensitiveTestSuiteTypeEditor());
	    }

	    @RequestMapping(value = "/quality/versions", method = GET, produces = APPLICATION_JSON_VALUE)
	    public DataResponse<Iterable<VersionData>> qualityData(@Valid VersionsRequest request) {
	        return componentVersionService.search(request);
	    }


	    @RequestMapping(value = "/quality/versions", method = POST,
	                consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	        public ResponseEntity<String> createTest(@Valid @RequestBody VersionDataCreateRequest request) throws HygieiaException {
	            String response = componentVersionService.create(request);
	            return ResponseEntity
	                    .status(HttpStatus.CREATED)
	                    .body(response);
	    }

}
