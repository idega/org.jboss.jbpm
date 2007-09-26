package com.idega.jbpm.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;

@Entity
@Table(name="ACTOR_TASK_BINDINGS")
public class ActorTaskBind implements Serializable {
	
	private static final long serialVersionUID = 7744283649458519318L;
	
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
	
	private static final String getByTaskIdAndActorType = "from " + ActorTaskBind.class.getName() + " as ATB where ATB.taskId = ? and ATB.actorType = ?";
	private static final String getByTaskId = "from " + ActorTaskBind.class.getName() + " ATB where ATB.taskId = :type";
	
	public static ActorTaskBind getBinding(Session session, long taskId, String actorType) {
		return (ActorTaskBind) session.createQuery(getByTaskIdAndActorType)
		.setLong(0, taskId)
		.setString(1, actorType)
		.uniqueResult();
	}
	
	public static ActorTaskBind getBinding(Session session, long taskId) {
		return (ActorTaskBind) session.createQuery(getByTaskId)
		.setLong("type", taskId)
		.uniqueResult();
	}
	
}
