package com.idega.jbpm.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.idega.jbpm.bean.VariableInstanceInfo;

public interface VariableInstanceQuerier {

	public Collection<VariableInstanceInfo> getVariablesByProcessDefinition(String processDefinitionName);
	public Collection<VariableInstanceInfo> getFullVariablesByProcessDefinition(String processDefinitionName);
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId);
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId);
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names);
	
	public Collection<VariableInstanceInfo> getVariablesByNames(List<String> names);
	
	//	TODO: enable this method when CLOB problem is fixed for Oracle
//	public Collection<VariableInstanceInfo> getVariablesByNameAndValue(String name, Serializable value);
	public boolean isVariableStored(String name, Serializable value);
}