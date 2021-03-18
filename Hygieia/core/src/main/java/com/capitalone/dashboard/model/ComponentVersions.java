package com.capitalone.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class ComponentVersions {
	private String name;
	private String version;
	private List<IpofficeClients> components= new ArrayList<>();
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public List<IpofficeClients> getComponents() {
		return components;
	}
	
	public void setComponents(List<IpofficeClients> components) {
		this.components = components;
	}
	
}
