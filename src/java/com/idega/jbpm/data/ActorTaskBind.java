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

import org.hibernate.Session;
import org.hibernate.Transaction;

@Entity
@Table(name="ACTOR_TASK_BINDINGS")
@NamedQueries(
		{
			@NamedQuery(name="actorTaskBind.getUniqueByTaskIdAndActorType", query="from ActorTaskBind ATB where ATB.taskId = :taskId and ATB.actorType = :actorType"),
			@NamedQuery(name="actorTaskBind.getUniqueByTaskId", query="from ActorTaskBind ATB where ATB.taskId = :taskId")
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
	
//	FIXME: why is this needed if taskId describes record uniquely? (@see method getBinding(Session session, long taskId))
	public static ActorTaskBind getBinding(Session session, long taskId, String actorType) {
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			return (ActorTaskBind) session.getNamedQuery(GET_UNIQUE_BY_TASK_ID_AND_ACTOR_TYPE_QUERY_NAME)
			.setLong(taskIdParam, taskId)
			.setString(actorTypeParam, actorType)
			.uniqueResult();
			
		} finally {
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public static ActorTaskBind getBinding(Session session, long taskId) {
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			return (ActorTaskBind) session.getNamedQuery(GET_UNIQUE_BY_TASK_ID_QUERY_NAME)
			.setLong(taskIdParam, taskId)
			.uniqueResult();
			
		} finally {
			if(!transactionWasActive)
				transaction.commit();
		}
	}
}