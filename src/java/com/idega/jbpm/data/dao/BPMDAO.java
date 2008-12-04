package com.idega.jbpm.data.dao;

import java.util.Collection;
import java.util.List;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.Role;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 *
 * Last modified: $Date: 2008/12/04 10:06:17 $ by $Author: civilis $
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
}