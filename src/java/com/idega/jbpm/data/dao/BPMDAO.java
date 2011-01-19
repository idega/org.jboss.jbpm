package com.idega.jbpm.data.dao;

import java.sql.Date;
import java.util.Collection;
import java.util.List;
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
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.identity.Role;

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
	
	public abstract void bindProcessVariables();
	
	public abstract void importVariablesData();
	
	public List<Long> getProcessInstanceIdsByProcessDefinitionNames(List<String> processDefinitionNames);
	
	public List<Long> getProcessInstanceIdsByDateRangeAndProcessDefinitionNamesOrProcInstIds(Date from, Date to, List<String> processDefinitionNames, List<Long> procInsIds);
}