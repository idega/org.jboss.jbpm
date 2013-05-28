package com.idega.jbpm.data.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.Variable;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.identity.Role;
import com.idega.util.IWTimestamp;

/**
 * serves as DAO, as well as PAO (process access object). In the latter case, this should be used
 * only in cases, when explicit access to process is required, but working with -wrapper- objects
 * (e.g. ProcessDefinitionW), ProcessManager and so on is recommended
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.20 $ Last modified: $Date: 2009/02/13 17:06:30 $ by $Author: donatas $
 */
public interface BPMDAO extends GenericDao {

	public abstract ViewTaskBind getViewTaskBind(long taskId, String viewType);

	public abstract ViewTaskBind getViewTaskBindByTaskInstance(long taskInstanceId, String viewType);

	public abstract List<ViewTaskBind> getViewTaskBindsByTaskId(long taskId);

	public abstract List<ViewTaskBind> getViewTaskBindsByTaskInstanceId(long taskInstanceId);

	public abstract ViewTaskBind getViewTaskBindByView(String viewId, String viewType);

	public abstract List<ViewTaskBind> getViewTaskBindsByTasksIds(Collection<Long> taskIds);

	public abstract Task getTaskFromViewTaskBind(ViewTaskBind viewTaskBind);

	public abstract ProcessManagerBind getProcessManagerBind(String processName);

	public abstract List<Actor> getAllGeneralProcessRoles();

	public abstract void updateAddGrpsToRole(Long roleActorId, Collection<String> selectedGroupsIds);

	public abstract List<NativeIdentityBind> getNativeIdentities(long processRoleIdentityId);

	public abstract List<Actor> getProcessRoles(Collection<Long> actorIds);

	public abstract List<NativeIdentityBind> getNativeIdentities(Collection<Long> actorsIds, IdentityType identityType);

	public abstract void updateCreateProcessRoles(Collection<Role> rolesNames, Long processInstanceId);

	public abstract List<Object[]> getProcessTasksViewsInfos(Collection<Long> processDefinitionsIds, String viewType);

	public abstract List<Actor> getProcessRoles(Collection<String> rolesNames, Long processInstanceId);

	public abstract List<ProcessInstance> getSubprocessInstancesOneLevel(long parentProcessInstanceId);

	public abstract ProcessDefinition findLatestProcessDefinition(final String processName);

	public abstract List<ActorPermissions> getPermissionsForUser(Integer userId, String processName, Long processInstanceId, Set<String> userNativeRoles,
			Set<String> userGroupsIds);

	public abstract int getTaskViewBindCount(String viewId, String viewType);

	public abstract void importVariablesData();

	public List<Long> getProcessInstanceIdsByProcessDefinitionNames(List<String> processDefinitionNames);

	/**
	 * <p>Queries database for {@link List} of {@link ProcessInstance#getId()
	 * } by:</p>
	 * @param from {@link ProcessInstance#getStart()} >= from,
	 * @param to {@link ProcessInstance#getStart()} <= to,
	 * @param processDefinitionNames
	 * {@link List} of {@link ProcessDefinition#getName()},
	 * @param procInsIds {@link List} of {@link ProcessInstance#getId()}.
	 * @return {@link List} of {@link ProcessInstance#getId()}.
	 * <code>null</code> on failure.
	 */
	public Map<Long, Map<String, java.util.Date>> getProcessInstanceIdsByDateRangeAndProcessDefinitionNamesOrProcInstIds(IWTimestamp from, IWTimestamp to,
			List<String> processDefinitionNames, List<Long> procInsIds);

	public List<Object[]> getProcessDateRanges(Collection<Long> processInstanceIds);

	public String getProcessDefinitionNameByProcessDefinitionId(Long processDefinitionId);

	/**
	 *
	 * @param processDefinitionName is {@link ProcessDefinition#getName()},
	 * not <code>null</code>;
	 * @return all {@link ProcessDefinition}s by given name or
	 * {@link Collections#emptyList()} on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessDefinition> getProcessDefinitions(String processDefinitionName);

	public List<Long> getProcessDefinitionIdsByName(String procDefName);

	public List<Variable> getVariablesByNameAndProcessInstance(String name, Long piId);
	public List<Variable> getVariablesByNameAndProcessInstance(List<String> names, Long piId);
	public List<Variable> getVariablesByNamesAndProcessInstanceIds(List<String> names, List<Long> piIds);
	public List<Variable> getVariablesByConditions(List<String> names, List<Long> piIds, List<Long> tiIds, List<Long> viIds);

	/**
	 *
	 * @param processDefinitionNames is {@link Collection} of
	 * {@link ProcessDefinition#getName()},
	 * not <code>null</code>;
	 * @return {@link List} of {@link ProcessInstance}s by
	 * {@link ProcessDefinition} names or {@link Collections#emptyList()}
	 * on failure;
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public List<ProcessInstance> getProcessInstances(List<String> processDefinitionNames);
}