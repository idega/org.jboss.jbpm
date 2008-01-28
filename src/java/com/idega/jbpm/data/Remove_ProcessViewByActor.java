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
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/01/28 07:43:40 $ by $Author: civilis $
 */

//TODO: remove
/*
@Entity
@Table(name="BPM_PROCESS_VIEW_BIND")
@NamedQueries({
		@NamedQuery(name="processViewByActor.getByViewerType", query="from ProcessViewByActor PVA where PVA.viewerType = :viewerType and viewType = :viewType and processDefinitionId = :processDefinitionId"),
		@NamedQuery(name="processViewByActor.getByViewType", query="from ProcessViewByActor PVA where PVA.viewType = :viewType and processDefinitionId = :processDefinitionId")
})
*/
public class Remove_ProcessViewByActor implements Serializable {
	
	private static final long serialVersionUID = -4151166970366065468L;
	
	public static final String GET_BY_VIEWER_TYPE_QUERY_NAME = "processViewByActor.getByViewerType";
	public static final String GET_BY_VIEW_TYPE_QUERY_NAME = "processViewByActor.getByViewType";
	public static final String viewerTypeParam = "viewerType";
	public static final String viewTypeParam = "viewType";
	public static final String processDefinitionIdParam = "processDefinitionId";
	
	public static final String VIEWER_TYPE_OWNER = "OWNER";
	public static final String VIEWER_TYPE_CASE_HANDLERS = "HANDLERS";
	public static final String VIEWER_TYPE_OTHERS = "OTHERS";
	
//	@Id @GeneratedValue(strategy = GenerationType.AUTO)
//	@Column(name="bind_id", nullable=false)
    private Long bindId;
	
	//@Column(name="process_definition_id", nullable=false)
	private Long processDefinitionId;
	
	//@Column(name="view_identifier", nullable=false)
	private String viewIdentifier;
	
	//@Column(name="view_type", nullable=false)
	private String viewType;
	
	//@Column(name="actor_id")
	private String actorId;
	
	//@Column(name="actor_type")
	private String actorType;
	
	//@Column(name="viewer_type", nullable=false)
	private String viewerType;
	
	public Remove_ProcessViewByActor() { }

	public Long getBindId() {
		return bindId;
	}

	@SuppressWarnings("unused")
	private void setBindId(Long bindId) {
		this.bindId = bindId;
	}

	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	public void setProcessDefinitionId(Long processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
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

	public String getActorId() {
		return actorId;
	}

	public void setActorId(String actorId) {
		this.actorId = actorId;
	}

	public String getActorType() {
		return actorType;
	}

	public void setActorType(String actorType) {
		this.actorType = actorType;
	}

	public String getViewerType() {
		return viewerType;
	}

	public void setViewerType(String viewerType) {
		this.viewerType = viewerType;
	}
}