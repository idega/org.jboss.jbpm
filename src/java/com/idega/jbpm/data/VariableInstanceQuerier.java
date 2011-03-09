package com.idega.jbpm.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.bean.VariableInstanceInfo;

public interface VariableInstanceQuerier extends GenericDao {

	/**
	 * Use a method to get variables by process instance id: getVariablesByProcessInstanceId or getVariablesByProcessDefinition
	 */
	@Deprecated
	public Collection<VariableInstanceInfo> getVariablesByProcessDefinitionNaiveWay(String processDefinitionName);
	
	public Collection<VariableInstanceInfo> getVariablesByProcessDefinition(String processDefinitionName);
	
	/**
	 * Use a method to get variables by process instance id: getVariablesByProcessInstanceId
	 */
	@Deprecated
	public Collection<VariableInstanceInfo> getFullVariablesByProcessDefinition(String processDefinitionName);
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds);
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds, List<Long> existingVars);
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId);
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId);
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId, boolean mirrow);
	
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
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValueAndProcInstIds(String name, Serializable value, List<Long> procInstIds);
	
	public void loadVariables(List<String> variablesNames);
	
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(String name, List<Serializable> values, List<String> procDefNames);
	
	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesByProcesses(Map<String, List<Serializable>> variablesWithValues, List<String> variables,
			List<String> procDefNames, List<Long> procInstIds, Map<String, Boolean> flexibleVariables);
}