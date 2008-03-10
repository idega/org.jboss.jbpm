package com.idega.jbpm.data.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.TaskInstanceAccess;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BpmBindsDAO;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.permission.Access;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/03/10 19:32:48 $ by $Author: civilis $
 */
public class BpmBindsDAOImpl extends GenericDaoImpl implements BpmBindsDAO {

	public ActorTaskBind getActorTaskBind(long taskId, String actorType) {
		
		return (ActorTaskBind) getEntityManager().createNamedQuery(ActorTaskBind.GET_UNIQUE_BY_TASK_ID_AND_ACTOR_TYPE_QUERY_NAME)
		.setParameter(ActorTaskBind.taskIdParam, taskId)
		.setParameter(ActorTaskBind.actorTypeParam, actorType)
		.getSingleResult();
	}
	
	public ActorTaskBind getActorTaskBind(long taskId) {
		
		return (ActorTaskBind) getEntityManager().createNamedQuery(ActorTaskBind.GET_UNIQUE_BY_TASK_ID_QUERY_NAME)
		.setParameter(ActorTaskBind.taskIdParam, taskId)
		.getSingleResult();
	}
	
	public ViewTaskBind getViewTaskBind(long taskId, String viewType) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(ViewTaskBind.GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME)
		.setParameter(ViewTaskBind.taskIdParam, taskId)
		.setParameter(ViewTaskBind.viewTypeParam, viewType)
		.getResultList();

		return binds.isEmpty() ? null : binds.iterator().next();
	}
	
	public List<ViewTaskBind> getViewTaskBindsByTaskId(long taskId) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(ViewTaskBind.getViewTaskBindsByTaskId)
		.setParameter(ViewTaskBind.taskIdParam, taskId)
		.getResultList();

		return binds;
	}
	
	public ViewTaskBind getViewTaskBindByView(String viewId, String viewType) {
		
		return (ViewTaskBind)getEntityManager().createNamedQuery(ViewTaskBind.GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME)
		.setParameter(ViewTaskBind.viewIdParam, viewId)
		.setParameter(ViewTaskBind.viewTypeParam, viewType)
		.getSingleResult();
	}
	
	public List<ViewTaskBind> getViewTaskBindsByTasksIds(Collection<Long> taskIds) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> viewTaskBinds = getEntityManager().createNamedQuery(ViewTaskBind.GET_VIEW_TASK_BINDS_BY_TASKS_IDS)
		.setParameter(ViewTaskBind.tasksIdsParam, taskIds)
		.getResultList();

		return viewTaskBinds;
	}

	public Task getTaskFromViewTaskBind(ViewTaskBind viewTaskBind) {
		
		return (Task)getEntityManager().createNamedQuery(ViewTaskBind.GET_VIEW_TASK)
		.setParameter(ViewTaskBind.viewTypeParam, viewTaskBind.getViewType())
		.setParameter(ViewTaskBind.taskIdParam, viewTaskBind.getTaskId())
		.getSingleResult();
	}
	
	public ManagersTypeProcessDefinitionBind getManagersTypeProcDefBind(long processDefinitionId) {
		
		return 
		(ManagersTypeProcessDefinitionBind)
		getEntityManager().createNamedQuery(ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getByProcessDefinitionId)
		.setParameter(ManagersTypeProcessDefinitionBind.processDefinitionIdParam, processDefinitionId)
		.getSingleResult();
	}
	
	public List<ProcessDefinition> getAllManagersTypeProcDefs() {
	
		@SuppressWarnings("unchecked")
		List<ProcessDefinition> all = getEntityManager().createNamedQuery(ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getAllProcDefs)
		.getResultList();
		
		return all;
	}
	
	public List<ProcessRole> getAllProcessRoleNativeIdentityBinds() {
		
		@SuppressWarnings("unchecked")
		List<ProcessRole> all = getEntityManager().createNamedQuery(ProcessRole.getAll)
		.getResultList();
		
		return all;
	}
	
	public List<ProcessRole> getAllProcessRoleNativeIdentityBinds(Collection<String> rolesNames) {
		
		if(rolesNames == null || rolesNames.isEmpty())
			throw new IllegalArgumentException("Roles names should contain values");
		
		@SuppressWarnings("unchecked")
		List<ProcessRole> all = getEntityManager().createNamedQuery(ProcessRole.getAllByRoleNames)
		.setParameter(ProcessRole.processRoleNameProperty, rolesNames)
		.getResultList();
		
		return all;
	}
	
	public List<ProcessRole> getProcessRoles(Collection<Long> actorIds) {
		
		if(actorIds == null || actorIds.isEmpty())
			throw new IllegalArgumentException("ActorIds should contain values");
		
		@SuppressWarnings("unchecked")
		List<ProcessRole> all = getEntityManager().createNamedQuery(ProcessRole.getAllByActorIds)
		.setParameter(ProcessRole.actorIdProperty, actorIds)
		.getResultList();
		
		return all;
	}
	
	public List<ProcessRole> getProcessRoles(Collection<Long> actorIds, long taskInstanceId) {
		
		if(actorIds == null || actorIds.isEmpty())
			throw new IllegalArgumentException("ActorIds should contain values");
		
		@SuppressWarnings("unchecked")
		List<ProcessRole> all = getEntityManager().createNamedQuery(ProcessRole.getAssignedToTaskInstances)
		.setParameter(ProcessRole.actorIdProperty, actorIds)
		.setParameter(TaskInstanceAccess.taskInstanceIdProperty, taskInstanceId)
		.getResultList();
		
		return all;
	}
	
	
	
	public void addGrpsToRole(Long roleActorId, Collection<String> selectedGroupsIds) {
		
		ProcessRole roleIdentity = find(ProcessRole.class, roleActorId);
		
		List<NativeIdentityBind> nativeIdentities = new ArrayList<NativeIdentityBind>(selectedGroupsIds.size());
		
		for (String groupId : selectedGroupsIds) {
		
			NativeIdentityBind nativeIdentity = new NativeIdentityBind();
			nativeIdentity.setIdentityId(groupId);
			nativeIdentity.setIdentityType(IdentityType.GROUP);
			nativeIdentity.setProcessRole(roleIdentity);
			nativeIdentities.add(nativeIdentity);
		}
		
		List<NativeIdentityBind> existingNativeIdentities = roleIdentity.getNativeIdentities();
		List<Long> nativeIdentitiesToRemove = new ArrayList<Long>();
		
		if(existingNativeIdentities != null) {
		
			for (NativeIdentityBind existing : existingNativeIdentities) {
				
				if(nativeIdentities.contains(existing)) {
					
					nativeIdentities.remove(existing);
					nativeIdentities.add(existing);
				} else {
					
					nativeIdentitiesToRemove.add(existing.getId());
				}
			}
		} else {
			existingNativeIdentities = new ArrayList<NativeIdentityBind>();
		}
		
		roleIdentity.setNativeIdentities(nativeIdentities);
		merge(roleIdentity);
		
		if(!nativeIdentitiesToRemove.isEmpty())
			getEntityManager().createNamedQuery(NativeIdentityBind.deleteByIds)
				.setParameter(NativeIdentityBind.idsParam, nativeIdentitiesToRemove)
				.executeUpdate();
	}
	
	public List<NativeIdentityBind> getNativeIdentities(long processRoleIdentityId) {
		
		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(NativeIdentityBind.getByProcIdentity)
		.setParameter(NativeIdentityBind.procIdentityParam, processRoleIdentityId)
		.getResultList();
		
		return binds;
	}
	
	public List<NativeIdentityBind> getNativeIdentities(List<Long> actorsIds, IdentityType identityType) {

		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(NativeIdentityBind.getByTypesAndProceIdentities)
		.setParameter(NativeIdentityBind.identityTypeProperty, identityType)
		.setParameter(ProcessRole.actorIdProperty, actorsIds)
		.getResultList();
		
		return binds;
	}
	
	public Collection<String> assignTaskAccesses(long taskInstanceId, Map<Role, ProcessRole> proles) {
		
		HashSet<String> actorIds = new HashSet<String>(proles.size());
		
		for (Entry<Role, ProcessRole> entry : proles.entrySet()) {
			
			TaskInstanceAccess tiAccess = new TaskInstanceAccess();
			tiAccess.setProcessRole((ProcessRole)merge(entry.getValue()));
			tiAccess.setTaskInstanceId(taskInstanceId);
			
			for (Access access : entry.getKey().getAccesses())
				tiAccess.addAccess(access);
			
			persist(tiAccess);
			
			actorIds.add(entry.getValue().getActorId().toString());
		}
		
		return actorIds;
	}
	
	public Collection<String> createPRolesAndAssignTaskAccesses(TaskInstance taskInstance, List<Role> proles) {

		try {
			
			ProcessInstance pi = taskInstance.getProcessInstance();
			HashMap<Role, ProcessRole> roleToAssign = new HashMap<Role, ProcessRole>(1);
			List<String> actorIds = new ArrayList<String>(proles.size());
			
			for (Role role : proles) {
				
				ProcessRole prole = new ProcessRole();
				prole.setProcessRoleName(role.getRoleName());
				prole.setProcessInstanceId(pi.getId());
				
				persist(prole);
				
				roleToAssign.clear();
				roleToAssign.put(role, prole);
				assignTaskAccesses(taskInstance.getId(), roleToAssign);
				
				actorIds.add(prole.getActorId().toString());
			}
			
			return actorIds;
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
	}
}