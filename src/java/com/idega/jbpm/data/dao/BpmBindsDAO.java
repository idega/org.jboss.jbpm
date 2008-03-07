package com.idega.jbpm.data.dao;

import java.util.Collection;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRoleNativeIdentityBind;
import com.idega.jbpm.data.ViewTaskBind;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/03/07 17:00:44 $ by $Author: civilis $
 */
public interface BpmBindsDAO extends GenericDao {

	public abstract ActorTaskBind getActorTaskBind(long taskId, String actorType);

	public abstract ActorTaskBind getActorTaskBind(long taskId);

	public abstract ViewTaskBind getViewTaskBind(long taskId, String viewType);

	public abstract List<ViewTaskBind> getViewTaskBindsByTaskId(long taskId);

	public abstract ViewTaskBind getViewTaskBindByView(String viewId, String viewType);

	public abstract List<ViewTaskBind> getViewTaskBindsByTasksIds(Collection<Long> taskIds);

	public abstract Task getTaskFromViewTaskBind(ViewTaskBind viewTaskBind);
	
	public abstract ManagersTypeProcessDefinitionBind getManagersTypeProcDefBind(long processDefinitionId);
	
	public abstract List<ProcessDefinition> getAllManagersTypeProcDefs();
	
	public abstract List<ProcessRoleNativeIdentityBind> getAllProcessRoleNativeIdentityBinds();
	
	public abstract void addGrpsToRole(Long roleActorId, Collection<String> selectedGroupsIds);
	
	public abstract List<NativeIdentityBind> getNativeIdentities(long processRoleIdentityId);
	
	public abstract List<ProcessRoleNativeIdentityBind> getAllProcessRoleNativeIdentityBinds(Collection<String> rolesNames);
	
	public abstract List<ProcessRoleNativeIdentityBind> getAllProcessRoleNativeIdentityBindsByActors(Collection<Long> actorIds);
}