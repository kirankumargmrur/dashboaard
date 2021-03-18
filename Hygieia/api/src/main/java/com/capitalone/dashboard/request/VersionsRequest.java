package com.capitalone.dashboard.request;

import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;

public class VersionsRequest {
	@NotNull
	private ObjectId componentId;
    private Integer numberOfDays;
    private Long dateBegins;
    private Long dateEnds;
    private Integer max;
    
    public ObjectId getComponentId() {
        return componentId;
    }

    public void setComponentId(ObjectId componentId) {
        this.componentId = componentId;
    }

	public Integer getNumberOfDays() {
		return numberOfDays;
	}

	public void setNumberOfDays(Integer numberOfDays) {
		this.numberOfDays = numberOfDays;
	}

	public Long getDateBegins() {
		return dateBegins;
	}

	public void setDateBegins(Long dateBegins) {
		this.dateBegins = dateBegins;
	}

	public Long getDateEnds() {
		return dateEnds;
	}

	public void setDateEnds(Long dateEnds) {
		this.dateEnds = dateEnds;
	}

	public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }
    
    public boolean validDateRange() {
        return dateBegins != null || dateEnds != null;
    }

}
