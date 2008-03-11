package com.idega.jbpm.data.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.Role;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/11 12:16:59 $ by $Author: civilis $
 */
public interface BPMDAO extends GenericDao {

	public abstract ActorTaskBind getActorTaskBind(long taskId, String actorType);

	public abstract ActorTaskBind getActorTaskBind(long taskId);

	public abstract ViewTaskBind getViewTaskBind(long taskId, String viewType);

	public abstract List<ViewTaskBind> getViewTaskBindsByTaskId(long taskId);

	public abstract ViewTaskBind getViewTaskBindByView(String viewId, String viewType);

	public abstract List<ViewTaskBind> getViewTaskBindsByTasksIds(Collection<Long> taskIds);

	public abstract Task getTaskFromViewTaskBind(ViewTaskBind viewTaskBind);
	
	public abstract ManagersTypeProcessDefinitionBind getManagersTypeProcDefBind(long processDefinitionId);
	
	public abstract List<ProcessDefinition> getAllManagersTypeProcDefs();
	
	public abstract List<ProcessRole> getAllProcessRoleNativeIdentityBinds();
	
	public abstract void updateAddGrpsToRole(Long roleActorId, Collection<String> selectedGroupsIds);
	
	public abstract List<NativeIdentityBind> getNativeIdentities(long processRoleIdentityId);
	
	public abstract List<ProcessRole> getAllProcessRoleNativeIdentityBinds(Collection<String> rolesNames);
	
	public abstract List<ProcessRole> getProcessRoles(Collection<Long> actorIds);
	
	public abstract List<ProcessRole> getProcessRoles(Collection<Long> actorIds, long taskInstanceId);
	
	public abstract List<NativeIdentityBind> getNativeIdentities(List<Long> actorsIds, IdentityType identityType);
	
	public abstract Collection<String> updateAssignTaskAccesses(long taskInstanceId, Map<Role, ProcessRole> proles);
	
	public abstract Collection<String> updateCreatePRolesAndAssignTaskAccesses(TaskInstance taskInstance, List<Role> proles);
}