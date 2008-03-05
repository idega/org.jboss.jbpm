package com.idega.jbpm.data.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRoleNativeIdentityBind;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BpmBindsDAO;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/03/05 21:11:51 $ by $Author: civilis $
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
	
	public List<ViewTaskBind> getViewTaskBindsByTasksIds(List<Long> taskIds) {
		
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
	
	public List<ProcessRoleNativeIdentityBind> getAllProcessRoleNativeIdentityBinds() {
		
		@SuppressWarnings("unchecked")
		List<ProcessRoleNativeIdentityBind> all = getEntityManager().createNamedQuery(ProcessRoleNativeIdentityBind.getAll)
		.getResultList();
		
		return all;
	}
	
	public List<ProcessRoleNativeIdentityBind> getAllProcessRoleNativeIdentityBinds(List<String> rolesNames) {
		
		if(rolesNames == null || rolesNames.isEmpty())
			throw new IllegalArgumentException("Roles names should contain values");
		
		@SuppressWarnings("unchecked")
		List<ProcessRoleNativeIdentityBind> all = getEntityManager().createNamedQuery(ProcessRoleNativeIdentityBind.getAllByRoleNames)
		.setParameter(ProcessRoleNativeIdentityBind.processRoleNameProperty, rolesNames)
		.getResultList();
		
		return all;
	}
	
	public void addGrpsToRole(Long roleActorId, List<String> selectedGroupsIds) {
		
		ProcessRoleNativeIdentityBind roleIdentity = find(ProcessRoleNativeIdentityBind.class, roleActorId);
		
		List<NativeIdentityBind> nativeIdentities = new ArrayList<NativeIdentityBind>(selectedGroupsIds.size());
		
		for (String groupId : selectedGroupsIds) {
		
			NativeIdentityBind nativeIdentity = new NativeIdentityBind();
			nativeIdentity.setIdentityId(groupId);
			nativeIdentity.setIdentityType(IdentityType.GROUP);
			nativeIdentity.setProcessRoleNativeIdentity(roleIdentity);
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
}