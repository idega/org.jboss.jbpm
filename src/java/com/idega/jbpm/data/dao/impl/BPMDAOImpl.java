package com.idega.jbpm.data.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.Role;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.20 $
 *
 * Last modified: $Date: 2008/12/04 10:06:17 $ by $Author: civilis $
 */
@Scope("singleton")
@Repository("bpmBindsDAO")
@Transactional(readOnly=true)
public class BPMDAOImpl extends GenericDaoImpl implements BPMDAO {
	
	public ViewTaskBind getViewTaskBind(long taskId, String viewType) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(ViewTaskBind.GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME)
		.setParameter(ViewTaskBind.taskIdParam, taskId)
		.setParameter(ViewTaskBind.viewTypeParam, viewType)
		.getResultList();

		return binds.isEmpty() ? null : binds.iterator().next();
	}
	
	public ViewTaskBind getViewTaskBindByTaskInstance(long taskInstanceId, String viewType) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(ViewTaskBind.GET_UNIQUE_BY_TASK_INSTANCE_ID_AND_VIEW_TYPE_QUERY_NAME)
		.setParameter(ViewTaskBind.taskInstanceIdProp, taskInstanceId)
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
	
	public List<ViewTaskBind> getViewTaskBindsByTaskInstanceId(long taskInstanceId) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(ViewTaskBind.getViewTaskBindsByTaskInstanceId)
		.setParameter(ViewTaskBind.taskInstanceIdProp, taskInstanceId)
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
	
	private ManagersTypeProcessDefinitionBind getManagersTypeProcDefBind(long processDefinitionId) {
		
		return getSingleResult(
				ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getByProcessDefinitionId, ManagersTypeProcessDefinitionBind.class, 
				new Param(ManagersTypeProcessDefinitionBind.processDefinitionIdParam, processDefinitionId)
		);
	}
	
	@Autowired private BPMContext bpmContext;
	public ProcessManagerBind getProcessManagerBind(String processName) {
		
		ProcessManagerBind pmb = getSingleResult(
				ProcessManagerBind.getByProcessName, ProcessManagerBind.class, 
				new Param(ProcessManagerBind.processNameProp, processName)
		);
		
		if(pmb == null) {
			
//			backward compat, remove at somewhat 12.15. Drop table bpm_managers_procdefs and the process definitions related in this table
//			and remove bpmContext  and getManagersTypeProcDefBind
			
			JbpmContext jctx = bpmContext.createJbpmContext();
			
			try {
				ProcessDefinition pd = jctx.getGraphSession().findLatestProcessDefinition(processName);
				
				if(pd != null) {
				
					ManagersTypeProcessDefinitionBind mtpdb = getManagersTypeProcDefBind(pd.getId());
					
					if(mtpdb != null) {
						
						pmb = new ProcessManagerBind();
						pmb.setManagersType(mtpdb.getManagersType());
						pmb.setProcessName(processName);
					}
				}
				
			} finally {
				bpmContext.closeAndCommit(jctx);
			}
		}
		
		return pmb;
	}
	
	public List<Actor> getAllGeneralProcessRoles() {
		
		@SuppressWarnings("unchecked")
		List<Actor> all = getEntityManager().createNamedQuery(Actor.getAllGeneral)
		.getResultList();
		
		return all;
	}
	
	public List<Actor> getProcessRoles(Collection<Long> actorIds) {
		
		if(actorIds == null || actorIds.isEmpty())
			throw new IllegalArgumentException("ActorIds should contain values");
		
		@SuppressWarnings("unchecked")
		List<Actor> all = getEntityManager().createNamedQuery(Actor.getAllByActorIds)
		.setParameter(Actor.actorIdProperty, actorIds)
		.getResultList();
		
		return all;
	}
	
	@Transactional(readOnly = false)
	public void updateAddGrpsToRole(Long roleActorId, Collection<String> selectedGroupsIds) {
		
		Actor roleIdentity = find(Actor.class, roleActorId);
		
		List<NativeIdentityBind> nativeIdentities = new ArrayList<NativeIdentityBind>(selectedGroupsIds.size());
		
		for (String groupId : selectedGroupsIds) {
		
			NativeIdentityBind nativeIdentity = new NativeIdentityBind();
			nativeIdentity.setIdentityId(groupId);
			nativeIdentity.setIdentityType(IdentityType.GROUP);
			nativeIdentity.setActor(roleIdentity);
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
		getEntityManager().merge(roleIdentity);
		
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
	
	public List<NativeIdentityBind> getNativeIdentities(Collection<Long> actorsIds, IdentityType identityType) {

		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(NativeIdentityBind.getByTypesAndProceIdentities)
		.setParameter(NativeIdentityBind.identityTypeProperty, identityType)
		.setParameter(Actor.actorIdProperty, actorsIds)
		.getResultList();
		
		return binds;
	}
	
	@Transactional(readOnly = false)
	public void updateCreateProcessRoles(Collection<Role> rolesNames, Long processInstanceId) {
		
		for (Role role : rolesNames) {
			
			Actor prole = new Actor();
			prole.setProcessRoleName(role.getRoleName());
			prole.setProcessInstanceId(processInstanceId);
			
			persist(prole);
		}
	}
	
	public List<Object[]> getProcessTasksViewsInfos(Collection<Long> processDefinitionsIds, String viewType) {
		
		if(processDefinitionsIds == null || processDefinitionsIds.isEmpty() || viewType == null)
			return new ArrayList<Object[]>(0);
		
		@SuppressWarnings("unchecked")
		List<Object[]> viewsInfos = getEntityManager().createNamedQuery(ViewTaskBind.GET_PROCESS_TASK_VIEW_INFO)
		.setParameter(ViewTaskBind.processDefIdsParam, processDefinitionsIds)
		.setParameter(ViewTaskBind.viewTypeProp, viewType)
		.getResultList();
		
		return viewsInfos;
	}
	
	public List<Actor> getProcessRoles(Collection<String> rolesNames, Long processInstanceId) {
		
		List<Actor> proles = 
			getResultList(
					Actor.getSetByRoleNamesAndPIId, Actor.class,
					new Param(Actor.processRoleNameProperty, rolesNames),
					new Param(Actor.processInstanceIdProperty, processInstanceId)
			);
		
		return proles;
	}
	
	public List<ProcessInstance> getSubprocessInstancesOneLevel(long parentProcessInstanceId) {
		
		List<ProcessInstance> subprocesses = 
			getResultList(
					ProcessManagerBind.getSubprocessesOneLevel, ProcessInstance.class,
					new Param(ProcessManagerBind.processInstanceIdParam, parentProcessInstanceId)
			);
		
		return subprocesses;
	}
}