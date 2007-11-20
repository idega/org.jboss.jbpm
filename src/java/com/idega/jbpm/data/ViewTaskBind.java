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

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2007/11/20 18:36:52 $ by $Author: civilis $
 */
@Entity
@Table(name="VIEW_TASK_BINDINGS")
@NamedQueries({@NamedQuery(name="viewTaskBind.getUniqueByTaskIdAndViewType", query="from ViewTaskBind VTB where VTB.taskId = :taskId and viewType = :viewType"),
@NamedQuery(name="viewTaskBind.getViewTaskBindByView", query="from ViewTaskBind VTB where VTB.viewIdentifier = :viewIdentifier and viewType = :viewType")})
public class ViewTaskBind implements Serializable {
	
	public static final String GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME = "viewTaskBind.getUniqueByTaskIdAndViewType";
	public static final String GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME = "viewTaskBind.getUniqueByTaskIdAndViewType";
	public static final String taskIdParam = "taskId";
	public static final String viewTypeParam = "viewType";
	public static final String viewIdParam = "viewIdentifier";
	
//	TODO: make bind id and view type as the composite PK. remove bind id.
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

}