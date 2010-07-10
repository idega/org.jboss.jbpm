package com.idega.jbpm.events;

import org.springframework.context.ApplicationEvent;

public class VariableCreatedEvent extends ApplicationEvent {

	private static final long serialVersionUID = -5977065467292706593L;
	
	private String processDefinitionName;
	
	public VariableCreatedEvent(Object source, String processDefinitionName) {
		super(source);
		
		this.processDefinitionName = processDefinitionName;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

}