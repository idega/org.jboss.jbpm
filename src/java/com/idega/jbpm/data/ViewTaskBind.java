package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/17 13:33:39 $ by $Author: civilis $
 */
@Entity
@Table(name="VIEW_TASK_BINDINGS")
public class ViewTaskBind implements Serializable {
	
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
}