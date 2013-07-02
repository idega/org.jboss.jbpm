package com.idega.jbpm.data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableQuerierData;

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

	public Collection<VariableInstanceInfo> getVariableByProcessInstanceIdAndVariableName(Long procId, String name);
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, boolean checkCache,
			List<String> names);
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names);
	public Collection<VariableInstanceInfo> getVariablesByProcessDefsAndVariablesNames(List<String> procDefNames, List<String> names);
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

//	public void loadVariables(List<String> variablesNames);

	/**
	 * <p>Gets values of jBPM variable instances from database.</p>
	 * @param name {@link BPMProcessVariable#getName()}.
	 * @param values {@link List} of {@link BPMProcessVariable#getValue()}.
	 * Could be {@link List} of {@link String}, {@link Number}, {@link Timestamp}.
	 * @param procDefNames {@link List} of {@link ProcessDefinition#getName()}.
	 * @return jBPM variable instances from database.
	 * {@link Collections#EMPTY_LIST} on failure.
	 */
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(
			String name,
			List<Serializable> values,
			List<String> procDefNames);

	/**
	 * <p>Gets values of jBPM variable instances from database.</p>
	 * @param name {@link BPMProcessVariable#getName()}.
	 * @param values {@link List} of {@link BPMProcessVariable#getValue()}.
	 * Could be {@link List} of {@link String}, {@link Number}, {@link Timestamp}.
	 * @param procDefNames {@link List} of {@link ProcessDefinition#getName()}.
	 * @param procInstIds {@link List} of {@link ProcessInstance#getId()}.
	 * @param selectProcessInstanceId
	 * @param searchExpression does not make sense anymore.
	 * @param mirrow There is one {@link String} long enough to be converted to
	 * CLOB type in ORACLE database. Property jbpm_variables_mirrowed, could be
	 * found at
	 * http://localhost:8080/workspace/developer/applicationproperties/ , set it
	 * to <code>true</code>, if it is needed to use alternative table in search,
	 * <code>false</code> otherwise.
	 * @return jBPM variable instances from database.
	 * {@link Collections#EMPTY_LIST} on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(
			String name,
			List<Serializable> values,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean searchExpression,
			boolean mirrow);

	/**
	 * <p>Gets values of jBPM variable instances from database.</p>
	 * @param name {@link BPMProcessVariable#getName()}.
	 * @param values {@link VariableQuerierData} instance.
	 * @param procDefNames {@link List} of {@link ProcessDefinition#getName()}.
	 * @param procInstIds {@link List} of {@link ProcessInstance#getId()}.
	 * @param selectProcessInstanceId
	 * @param mirrow There is one {@link String} long enough to be converted to
	 * CLOB type in ORACLE database. Property jbpm_variables_mirrowed, could be
	 * found at
	 * http://localhost:8080/workspace/developer/applicationproperties/ , set it
	 * to <code>true</code>, if it is needed to use alternative table in search,
	 * <code>false</code> otherwise.
	 * @return jBPM variable instances from database.
	 * {@link Collections#EMPTY_LIST} on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(
			String name,
			VariableQuerierData values,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean mirrow);

	/**
	 *
	 * @param variablesWithValues {@link Map} of (
	 * {@link BPMProcessVariable#getName()},
	 * {@link BPMProcessVariable#getRealValue()}).
	 * @param variables {@link List} of {@link BPMProcessVariable#getName()}.
	 * @param procDefNames {@link List} of {@link ProcessDefinition#getName()}.
	 * @param procInstIds {@link List} of {@link ProcessInstance#getId()}.
	 * @param flexibleVariables {@link Map} of (
	 * {@link BPMProcessVariable#getName()},
	 * {@link BPMProcessVariable#isFlexible()}).
	 * @return {@link Map} of ({@link ProcessInstance#getId()}, {@link Map} of
	 * ({@link BPMProcessVariable#getName()}, {@link VariableInstanceInfo})).
	 * <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesByProcesses(
			Map<String, List<Serializable>> variablesWithValues,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables);

	/**
	 *
	 * @param variablesWithValues {@link Map} of
	 * ({@link BPMProcessVariable#getName()}, {@link VariableQuerierData}).
	 * @param variables {@link List} of {@link BPMProcessVariable#getName()}.
	 * @param procDefNames {@link List} of {@link ProcessDefinition#getName()}.
	 * @param procInstIds {@link List} of {@link ProcessInstance#getId()}.
	 * @param flexibleVariables {@link Map} of (
	 * {@link BPMProcessVariable#getName()},
	 * {@link BPMProcessVariable#isFlexible()}).
	 * @return {@link Map} of ({@link ProcessInstance#getId()}, {@link Map} of
	 * ({@link BPMProcessVariable#getName()}, {@link VariableInstanceInfo})).
	 * <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> variablesWithValues,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables);

	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean useCachedVariables);

//	public List<String> getValuesByVariableFromMirrowedTable(String name);
	public List<String> getValuesByVariable(String name);

	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIds(Collection<Long> procInstIds);

	public List<VariableInstanceInfo> getVariablesByNameAndTaskInstance(Collection<String> names, Long tiId);

	public Map<Long, List<VariableInstanceInfo>> getGroupedVariables(Collection<VariableInstanceInfo> variables);

	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean useCachedVariables,
			boolean strictBinaryVariables
	);
	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> originalProcInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean useCachedVariables,
			boolean strictBinaryVariables,
			boolean selectOnlyProcIntsIds
	);

	public boolean isVariableCached(String name);
	public List<VariableInstanceInfo> getCachedVariables(String name, Serializable value, boolean approximate);
	public List<VariableInstanceInfo> getCachedVariables(String name);
	public Map<Long, VariableInstanceInfo> getCachedVariablesGroupedByProcess(String name);

	public Collection<VariableInstanceInfo> getConverted(List<Serializable[]> data, int numberOfColumns);
	public Map<Long, Map<String, VariableInstanceInfo>> getConvertedVariables(List<Variable> variables);
	public List<VariableInstanceInfo> getConverted(Map<Long, Map<String, VariableInstanceInfo>> vars);

	public void doBindProcessVariables();
	public void doIndexVariables(Long piId);

}