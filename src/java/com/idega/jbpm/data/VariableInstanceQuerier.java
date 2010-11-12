package com.idega.jbpm.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.bean.VariableInstanceInfo;

public interface VariableInstanceQuerier extends GenericDao {

	public Collection<VariableInstanceInfo> getVariablesByProcessDefinitionNaiveWay(String processDefinitionName);
	public Collection<VariableInstanceInfo> getVariablesByProcessDefinition(String processDefinitionName);
	public Collection<VariableInstanceInfo> getFullVariablesByProcessDefinition(String processDefinitionName);
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds);
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds, List<Long> existingVars);
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId);
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId);
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names);
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names, boolean checkTaskInstance);
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(List<String> names, Collection<Long> procIds, boolean checkTaskInstance,
			boolean addEmptyVars);
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(List<String> names, Collection<Long> procIds, boolean checkTaskInstance,
			boolean addEmptyVars, boolean mirrowData);
	
	public Collection<VariableInstanceInfo> getVariablesByNames(List<String> names);
	
	public Collection<VariableInstanceInfo> getVariablesByNameAndValue(String name, Serializable value);
	public boolean isVariableStored(String name, Serializable value);
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValue(String name, Serializable value);
	
	public void loadVariable(String variableName);
}