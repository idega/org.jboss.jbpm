package com.idega.jbpm.data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableQuerierData;

public interface VariableInstanceQuerier extends GenericDao {

	public EntityManager getEntityManager();

	/**
	 * Use a method to get variables by process instance id: getVariablesByProcessInstanceId or getVariablesByProcessDefinition
	 */
	@Deprecated
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessDefinitionNaiveWay(String processDefinitionName);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessDefinition(String processDefinitionName);

	/**
	 * Use a method to get variables by process instance id: getVariablesByProcessInstanceId
	 */
	@Deprecated
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getFullVariablesByProcessDefinition(String processDefinitionName);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds, List<Long> existingVars);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessInstanceId(Long processInstanceId);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getFullVariablesByProcessInstanceId(Long processInstanceId);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getFullVariablesByProcessInstanceId(Long processInstanceId, boolean mirrow);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getFullVariablesByTaskInstanceId(Long taskInstanceId);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getFullVariablesByTaskInstanceIdsAndVariablesNames(List<Long> taskInstanceIds, List<String> names);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariableByProcessInstanceIdAndVariableName(Long procId, String name);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessDefsAndVariablesNames(List<String> procDefNames, List<String> names);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessInstanceIdAndVariablesNames(
			Collection<Long> procIds,
			List<String> names,
			boolean checkTaskInstance
	);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessInstanceIdAndVariablesNames(
			List<String> names,
			Collection<Long> procIds,
			boolean checkTaskInstance,
			boolean addEmptyVars
	);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessInstanceIdAndVariablesNames(
			List<String> names,
			Collection<Long> procIds,
			boolean checkTaskInstance,
			boolean addEmptyVars,
			boolean mirrowData
	);

	public <V extends com.idega.bpm.model.VariableInstance> Map<Long, Map<String, V>> getGroupedData(Collection<V> variables);
	public <V extends com.idega.bpm.model.VariableInstance> Map<Long, Map<String, V>> getGroupedData(Collection<V> variables, boolean byTaskInstance);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessDefAndVariableName(String procDefName, String name);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessDefAndVariableName(String procDefName, String name, boolean checkBind);
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessDefAndVariablesNames(String procDefName, List<String> names, boolean checkBind);

	/**
	 *
	 * <p>Queries jBPM data source for variables.</p>
	 * @param procDefNames is {@link List} of {@link ProcessDefinition#getName()},
	 * not <code>null</code>;
	 * @param variableNames is name of JBPM variable:
	 * {@link VariableInstance#getName()}, not <code>null</code>
	 * @param checkTaskInstance
	 * @return variables with values, found in data source,
	 * or {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariables(
			List<String> procDefNames,
			List<String> variableNames,
			boolean checkTaskInstance
	);

	/**
	 *
	 * <p>Selects {@link VariableInstance}s from jBPM data source.</p>
	 * @param variableNames is {@link Collection} of {@link VariableInstance#getName()}
	 * to search values for, not <code>null</code>;
	 * @param processInstances to get variables for, not <code>null</code>;
	 * @param checkTaskInstance - tells if {@link TaskInstance}s should be
	 * selected too;
	 * @param addEmptyVars tells if empty {@link com.idega.bpm.model.VariableInstance}s
	 * should be created for variable names, which are not found in database.
	 * They won't have {@link com.idega.bpm.model.VariableInstance#getVariableValue()};
	 * @param mirrowData tells if {@link BPMVariableData} data table should be used;
	 * @return {@link VariableInstance}s from jBPM data tables or
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public <V extends com.idega.bpm.model.VariableInstance> List<V> getVariables(
			Collection<String> variableNames,
			Collection<ProcessInstance> processInstances,
			boolean checkTaskInstance,
			boolean addEmptyVars,
			boolean mirrowData
	);

	public <V extends com.idega.bpm.model.VariableInstance> List<V> getVariables(
			Collection<String> variableNames,
			Set<Long> processInstancesIds,
			boolean checkTaskInstance,
			boolean addEmptyVars,
			boolean mirrowData
	);

	/**
	 *
	 * <p>Selects {@link VariableInstance}s from jBPM data source.</p>
	 * @param variableNames is {@link Collection} of {@link VariableInstance#getName()}
	 * to search values for, not <code>null</code>;
	 * @param processInstances to get variables for, not <code>null</code>;
	 * @param checkTaskInstance - tells if {@link TaskInstance}s should be
	 * selected too;
	 * @param addEmptyVars tells if empty {@link com.idega.bpm.model.VariableInstance}s
	 * should be created for variable names, which are not found in database.
	 * They won't have {@link com.idega.bpm.model.VariableInstance#getVariableValue()};
	 * @param mirrowData tells if {@link BPMVariableData} data table should be used;
	 * @return {@link VariableInstance}s from jBPM data tables or
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public <V extends com.idega.bpm.model.VariableInstance> Map<String, V> getSortedVariables(
			Collection<String> variableNames,
			Collection<ProcessInstance> processInstances,
			boolean checkTaskInstance,
			boolean addEmptyVars,
			boolean mirrowData
	);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByNames(List<String> names);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByNameAndValue(String name, Serializable value);
	public boolean isVariableStored(String name, Serializable value);
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValue(String name, Serializable value);
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValueAndProcInstIds(String name, Serializable value, List<Long> procInstIds);

	/**
	 * <p>Gets values of jBPM variable instances from database.</p>
	 * @param name {@link BPMProcessVariable#getName()}.
	 * @param values {@link List} of {@link BPMProcessVariable#getValue()}.
	 * Could be {@link List} of {@link String}, {@link Number}, {@link Timestamp}.
	 * @param procDefNames {@link List} of {@link ProcessDefinition#getName()}.
	 * @return jBPM variable instances from database.
	 * {@link Collections#EMPTY_LIST} on failure.
	 */
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getProcessVariablesByNameAndValue(
			String name,
			List<Serializable> values,
			List<String> procDefNames
	);

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
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getProcessVariablesByNameAndValue(
			String name,
			List<Serializable> values,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean searchExpression,
			boolean mirrow
	);

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
	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getProcessVariablesByNameAndValue(
			String name,
			VariableQuerierData values,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean mirrow
	);

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
	 * ({@link BPMProcessVariable#getName()}, {@link com.idega.bpm.model.VariableInstance})).
	 * <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public <V extends com.idega.bpm.model.VariableInstance> Map<Long, Map<String, V>> getVariablesByNamesAndValuesByProcesses(
			Map<String, List<Serializable>> variablesWithValues,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables
	);

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
	 * ({@link BPMProcessVariable#getName()}, {@link com.idega.bpm.model.VariableInstance})).
	 * <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public <V extends com.idega.bpm.model.VariableInstance> Map<Long, Map<String, V>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> variablesWithValues,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables);

	public List<String> getValuesByVariable(String name);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getVariablesByProcessInstanceIds(Collection<Long> procInstIds);

	public <V extends com.idega.bpm.model.VariableInstance> List<V> getVariablesByNameAndTaskInstance(Collection<String> names, Long tiId);

	public <V extends com.idega.bpm.model.VariableInstance> Map<Long, List<V>> getGroupedVariables(Collection<V> variables);

	public <V extends com.idega.bpm.model.VariableInstance> Map<Long, Map<String, V>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables
	);
	public <V extends com.idega.bpm.model.VariableInstance> Map<Long, Map<String, V>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> originalProcInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables,
			boolean selectOnlyProcIntsIds
	);

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getConverted(List<Serializable[]> data);
	public <V extends com.idega.bpm.model.VariableInstance> Map<Long, Map<String, V>> getConvertedVariables(List<Variable> variables);
	public <V extends com.idega.bpm.model.VariableInstance> List<V> getConverted(Map<Long, Map<String, V>> vars);

	public void doBindProcessVariables();
	public void doIndexVariables(Long piId);

	/**
	 *
	 * <p>Searches for {@link VariableInstance} from jbpm_variableinstance
	 * table by:</p>
	 * @param requiredVariableName is {@link VariableInstance#getName()} which
	 * instances should be found, not <code>null</code>;
	 * @param argumentVariableName is {@link VariableInstance#getName()} of
	 * {@link VariableInstance} by which required variable should be found.
	 * When <code>null</code>, argumentValue will be ignored;
	 * @param argumentValue is {@link VariableInstance#getValue()} of
	 * {@link VariableInstance} by name defined in argumentVariableName;
	 * @param processInstanceId is {@link ProcessInstance#getId()}, which
	 * contains required {@link VariableInstance};
	 * @return {@link Collection} of {@link com.idega.bpm.model.VariableInstance}, which
	 * matches criteria, or {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public <V extends com.idega.bpm.model.VariableInstance> List<V> findJBPMVariable(
			String requiredVariableName,
			String argumentVariableName,
			String argumentValue,
			Long processInstanceId
	);

	/**
	 *
	 * <p>Searches for {@link VariableInstance} from jbpm_variableinstance
	 * table by:</p>
	 * @param requiredVariableName is {@link VariableInstance#getName()} which
	 * instances should be found, not <code>null</code>;
	 * @param argumentVariableName is {@link VariableInstance#getName()} of
	 * {@link VariableInstance} by which required variable should be found.
	 * When <code>null</code>, argumentValue will be ignored;
	 * @param argumentValue is {@link VariableInstance#getValue()} of
	 * {@link VariableInstance} by name defined in argumentVariableName;
	 * @param processInstanceId is {@link ProcessInstance#getId()}, which
	 * contains required {@link VariableInstance};
	 * @return {@link Collection} of {@link com.idega.bpm.model.VariableInstance#getVariableValue()},
	 * which matches criteria, or {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public List<Serializable> findJBPMVariableValues(String requiredVariableName,
			String argumentVariableName, String argumentValue,
			Long processInstance);

}