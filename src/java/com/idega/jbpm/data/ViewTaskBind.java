package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/09/18 09:45:39 $ by $Author: civilis $
 */
@Entity
@Table(name="VIEW_TASK_BINDINGS")
public class ViewTaskBind implements Serializable {
	
//	TODO: make bind id and view type as the composite PK. remove bind id.
//	supposedly add versioning (hibernate versioning)
// 	add constraints - no column should be null
	
	
	private static final long serialVersionUID = 7744283644611519318L;
	
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
	
	private static final String getViewTaskBindQuery = "from "+ViewTaskBind.class.getName()+" as VTB where VTB.taskId = ? and viewType = ?";
	
	public static ViewTaskBind getViewTaskBind(Session session, long taskId, String viewType) {
	
//		TODO: retrieve ViewTaskBind by using taskId and viewType as the composite PK
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = session.createQuery(getViewTaskBindQuery)
		.setLong(0, taskId)
		.setString(1, viewType)
		.list();
		
		return binds.isEmpty() ? null : binds.iterator().next();
	}
}