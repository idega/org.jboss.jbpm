package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 *
 * Last modified: $Date: 2008/01/06 17:02:59 $ by $Author: civilis $
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
}