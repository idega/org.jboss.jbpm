package com.idega.jbpm.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.NamedQueries;

import javax.persistence.Table;

@Entity
@Table(name="BPM_ACTOR_TASK")
@NamedQueries(
		{
			@NamedQuery(name=ActorTaskBind.GET_UNIQUE_BY_TASK_ID_AND_ACTOR_TYPE_QUERY_NAME, query="from ActorTaskBind ATB where ATB.taskId = :"+ActorTaskBind.taskIdParam+" and ATB.actorType = :"+ActorTaskBind.actorTypeParam),
			@NamedQuery(name=ActorTaskBind.GET_UNIQUE_BY_TASK_ID_QUERY_NAME, query="from ActorTaskBind ATB where ATB.taskId = :"+ActorTaskBind.taskIdParam)
		}		
)
public class ActorTaskBind implements Serializable {
	
	private static final long serialVersionUID = 7744283649458519318L;
	
	public static final String GET_UNIQUE_BY_TASK_ID_AND_ACTOR_TYPE_QUERY_NAME = "actorTaskBind.getUniqueByTaskIdAndActorType";
	public static final String GET_UNIQUE_BY_TASK_ID_QUERY_NAME = "actorTaskBind.getUniqueByTaskId";
	public static final String taskIdParam = "taskId";
	public static final String actorTypeParam = "actorType";
	
	public static final String USER = "USER";
	public static final String GROUP = "GROUP";
	public static final String ROLE = "ROLE";
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID")
    private Long id;
	
	@Column(name="TASK_ID")
	private Long taskId;
	
	@Column(name="ACTOR_ID")
	private String actorId;
	
	@Column(name="ACTOR_TYPE")
	private String actorType;

	public Long getId() {
		return id;
	}

	@SuppressWarnings("unused")
	private void setId(Long id) {
		this.id = id;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
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
}