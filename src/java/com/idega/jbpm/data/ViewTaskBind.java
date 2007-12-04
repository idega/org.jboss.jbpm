package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jbpm.taskmgmt.def.Task;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 *
 * Last modified: $Date: 2007/12/04 18:49:48 $ by $Author: civilis $
 */
@Entity
@Table(name="VIEW_TASK_BINDINGS")
@NamedQueries(
		{
			@NamedQuery(name="viewTaskBind.getUniqueByTaskIdAndViewType", query="from ViewTaskBind VTB where VTB.taskId = :taskId and viewType = :viewType"),
			@NamedQuery(name="viewTaskBind.getViewTaskBindsByTaskId", query="from ViewTaskBind VTB where VTB.taskId = :taskId"),
			@NamedQuery(name="viewTaskBind.getViewTaskBindsByTasksIds", query="from ViewTaskBind VTB where VTB.taskId in (:tasksIds)"),
			@NamedQuery(name="viewTaskBind.getViewTaskBindByView", query="from ViewTaskBind VTB where VTB.viewIdentifier = :viewIdentifier and viewType = :viewType"),
			@NamedQuery(name="viewTaskBind.getViewTask", query="select TASK from org.jbpm.taskmgmt.def.Task TASK, ViewTaskBind VTB where TASK.id = VTB.taskId and VTB.taskId = :taskId and VTB.viewType = :viewType")
		}
)
public class ViewTaskBind implements Serializable {
	
	public static final String GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME = "viewTaskBind.getUniqueByTaskIdAndViewType";
	public static final String getViewTaskBindsByTaskId = "viewTaskBind.getViewTaskBindsByTaskId";
	public static final String GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME = "viewTaskBind.getUniqueByTaskIdAndViewType";
	public static final String GET_VIEW_TASK_BINDS_BY_TASKS_IDS = "viewTaskBind.getViewTaskBindsByTasksIds";
	public static final String GET_VIEW_TASK = "viewTaskBind.getViewTask";
	
	public static final String taskIdParam = "taskId";
	public static final String tasksIdsParam = "tasksIds";
	public static final String viewTypeParam = "viewType";
	public static final String viewIdParam = "viewIdentifier";
	
//	supposedly add versioning (hibernate versioning)
	
	private static final long serialVersionUID = -1604232647212632303L;

	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="bind_id", nullable=false)
    private Long bindId;
	
	@Column(name="task_id", nullable=false)
	private Long taskId;
	
	@Column(name="view_identifier", nullable=false)
	private String viewIdentifier;
	
	@Column(name="view_type", nullable=false)
	private String viewType;

//	@ManyToOne
//	@JoinColumn(name="ID_")
//	private Task task;
	
	public ViewTaskBind() { }

	public Long getBindId() {
		return bindId;
	}
	
	@SuppressWarnings("unused")
	private void setBindId(Long bindId) {
		this.bindId = bindId;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public String getViewIdentifier() {
		return viewIdentifier;
	}

	public void setViewIdentifier(String viewIdentifier) {
		this.viewIdentifier = viewIdentifier;
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}
	
	public static ViewTaskBind getViewTaskBind(Session session, long taskId, String viewType) {
	
//		FIXME: retrieve ViewTaskBind by using taskId and viewType as the composite PK
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			@SuppressWarnings("unchecked")
			List<ViewTaskBind> binds = session.getNamedQuery(GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME)
			.setLong(taskIdParam, taskId)
			.setString(viewTypeParam, viewType)
			.list();
			
			return binds.isEmpty() ? null : binds.iterator().next();
			
		} finally {
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public static List<ViewTaskBind> getViewTaskBindsByTaskId(Session session, long taskId) {
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			@SuppressWarnings("unchecked")
			List<ViewTaskBind> binds = session.getNamedQuery(getViewTaskBindsByTaskId)
			.setLong(taskIdParam, taskId)
			.list();
			
			return binds;
			
		} finally {
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public static ViewTaskBind getViewTaskBindByView(Session session, String viewId, String viewType) {
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			
			return (ViewTaskBind) session.getNamedQuery(GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME)
			.setString(viewIdParam, viewId)
			.setString(viewTypeParam, viewType)
			.uniqueResult();
			
		} finally {
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public static List<ViewTaskBind> getViewTaskBindsByTasksIds(Session session, List<Long> taskIds) {
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			@SuppressWarnings("unchecked")
			List<ViewTaskBind> viewTaskBinds = session.getNamedQuery(GET_VIEW_TASK_BINDS_BY_TASKS_IDS)
			.setParameterList(tasksIdsParam, taskIds)
			.list();
			
			return viewTaskBinds;
			
		} finally {
			if(!transactionWasActive)
				transaction.commit();
		}
	}

	public Task getTask(Session session) {
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			return (Task)session.getNamedQuery(GET_VIEW_TASK)
			.setString(viewTypeParam, getViewType())
			.setLong(taskIdParam, getTaskId())
			.uniqueResult();
			
		} finally {
			if(!transactionWasActive)
				transaction.commit();
		}
	}
}