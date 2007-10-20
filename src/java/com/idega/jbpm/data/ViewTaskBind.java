package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2007/10/20 20:18:27 $ by $Author: civilis $
 */
@Entity
@Table(name="VIEW_TASK_BINDINGS")
@NamedQuery(name="viewTaskBind.getUniqueByTaskIdAndViewType", query="from ViewTaskBind VTB where VTB.taskId = :taskId and viewType = :viewType")
public class ViewTaskBind implements Serializable {
	
	public static final String GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME = "viewTaskBind.getUniqueByTaskIdAndViewType";
	public static final String taskIdParam = "taskId";
	public static final String viewTypeParam = "viewType";
	
//	TODO: make bind id and view type as the composite PK. remove bind id.
//	supposedly add versioning (hibernate versioning)
// 	add constraints - no column should be null
	
	private static final long serialVersionUID = -1604232647212632303L;

	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="bind_id")
    private Long bindId;
	
	@Column(name="task_id")
	private Long taskId;
	
	@Column(name="view_identifier")
	private String viewIdentifier;
	
	@Column(name="view_type")
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
}