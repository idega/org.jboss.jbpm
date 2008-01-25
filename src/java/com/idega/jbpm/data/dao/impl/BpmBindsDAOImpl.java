package com.idega.jbpm.data.dao.impl;

import java.util.List;

import org.jbpm.taskmgmt.def.Task;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BpmBindsDAO;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/01/25 15:24:26 $ by $Author: civilis $
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
}