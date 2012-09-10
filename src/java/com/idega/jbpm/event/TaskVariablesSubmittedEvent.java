package com.idega.jbpm.event;

import org.jbpm.graph.exe.Token;
import org.springframework.context.ApplicationEvent;

public class TaskVariablesSubmittedEvent extends ApplicationEvent {

	private static final long serialVersionUID = 647299810522341493L;

	private Token token;

	private Long taskInstanceId;

	public TaskVariablesSubmittedEvent(Object source, Token token, Long taskInstanceId) {
		super(source);

		this.token = token;
		this.taskInstanceId = taskInstanceId;
	}

	public Token getToken() {
		return token;
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

}