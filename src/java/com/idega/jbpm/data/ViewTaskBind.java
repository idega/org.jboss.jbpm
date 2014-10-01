package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.idega.jbpm.exe.ProcessConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.18 $
 *
 * Last modified: $Date: 2009/01/13 10:47:56 $ by $Author: anton $
 */
@Entity
@Table(name="BPM_VIEWS_TASKS")
@NamedQueries(
		{
			@NamedQuery(name=ViewTaskBind.GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME, query="from ViewTaskBind VTB where VTB."+ViewTaskBind.taskIdProp+" = :"+ViewTaskBind.taskIdParam+" and viewType = :"+ViewTaskBind.viewTypeParam),
			@NamedQuery(name=ViewTaskBind.GET_UNIQUE_BY_TASK_INSTANCE_ID_AND_VIEW_TYPE_QUERY_NAME, query="from ViewTaskBind VTB where VTB."+ViewTaskBind.taskInstanceIdProp+" = :"+ViewTaskBind.taskInstanceIdProp+" and viewType = :"+ViewTaskBind.viewTypeParam),
			@NamedQuery(name=ViewTaskBind.getViewTaskBindsByTaskId, query="from ViewTaskBind VTB where VTB."+ViewTaskBind.taskIdProp+" = :"+ViewTaskBind.taskIdParam),
			@NamedQuery(name=ViewTaskBind.getViewTaskBindsByTaskInstanceId, query="from ViewTaskBind VTB where VTB."+ViewTaskBind.taskInstanceIdProp+" = :"+ViewTaskBind.taskInstanceIdProp),
			@NamedQuery(name=ViewTaskBind.GET_VIEW_TASK_BINDS_BY_TASKS_IDS, query="from ViewTaskBind VTB where VTB."+ViewTaskBind.taskIdProp+" in (:"+ViewTaskBind.tasksIdsParam+")"),
			@NamedQuery(name=ViewTaskBind.GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME, query="from ViewTaskBind VTB where VTB.viewIdentifier = :"+ViewTaskBind.viewIdParam+" and viewType = :"+ViewTaskBind.viewTypeParam),
			@NamedQuery(name=ViewTaskBind.GET_VIEW_TASK, query="select TASK from org.jbpm.taskmgmt.def.Task TASK, ViewTaskBind VTB where VTB."+ViewTaskBind.taskIdProp+" = :"+ViewTaskBind.taskIdParam+" and VTB.viewType = :"+ViewTaskBind.viewTypeParam+" and TASK.id = VTB."+ViewTaskBind.taskIdParam),
			@NamedQuery(name=ViewTaskBind.GET_PROCESS_TASK_VIEW_INFO, query="select task, vtb."+ViewTaskBind.viewIdentifierProp+" from org.jbpm.graph.def.ProcessDefinition pd, org.jbpm.taskmgmt.def.Task task, ViewTaskBind vtb where pd.id in (:"+ViewTaskBind.processDefIdsParam+") and task.processDefinition = pd.id and vtb."+ViewTaskBind.taskIdProp+" = task.id and vtb."+ViewTaskBind.viewTypeProp+" = :"+ViewTaskBind.viewTypeProp)
		}
)
@Cacheable
public class ViewTaskBind implements Serializable {

	public static final String GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME = "viewTaskBind.getUniqueByTaskIdAndViewType";
	public static final String GET_UNIQUE_BY_TASK_INSTANCE_ID_AND_VIEW_TYPE_QUERY_NAME = "viewTaskBind.getUniqueByTaskInstanceIdAndViewType";
	public static final String getViewTaskBindsByTaskId = "viewTaskBind.getViewTaskBindsByTaskId";
	public static final String getViewTaskBindsByTaskInstanceId = "viewTaskBind.getViewTaskBindsByTaskInstanceId";
	public static final String GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME = "viewTaskBind.getViewTaskBindByView";
	public static final String GET_VIEW_TASK_BINDS_BY_TASKS_IDS = "viewTaskBind.getViewTaskBindsByTasksIds";
	public static final String GET_VIEW_TASK = "viewTaskBind.getViewTask";
	public static final String GET_PROCESS_TASK_VIEW_INFO = "viewTaskBind.GET_PROCESS_TASK_VIEW_INFO";

	public static final String processDefIdsParam = "pdids";
	public static final String taskIdParam = "taskId";
	public static final String tasksIdsParam = "tasksIds";
	public static final String viewTypeParam = "viewType";
	public static final String viewIdParam = "viewIdentifier";

	private static final long serialVersionUID = -1604232647212632303L;

	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="bind_id")
    private Long bindId;

	public static final String taskIdProp = "taskId";
	@Column(name="task_id", nullable=true)
	private Long taskId;

	public static final String taskInstanceIdProp = ProcessConstants.TASK_INSTANCE_ID;
	@Column(name="task_instance_id", nullable=true)
	private Long taskInstanceId;

	public static final String viewIdentifierProp = "viewIdentifier";
	@Column(name="view_identifier", nullable=false)
	private String viewIdentifier;

	public static final String viewTypeProp = "viewType";
	@Column(name="view_type", nullable=false)
	private String viewType;

	public static final String viewOrderProp = "viewOrder";
	@Column(name="viewOrder", nullable=true)
	private Integer viewOrder;

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

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public Integer getViewOrder() {
		return viewOrder;
	}

	public void setViewOrder(Integer viewOrder) {
		this.viewOrder = viewOrder;
	}

}