package com.idega.jbpm.events;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

public class VariableCreatedEvent extends ApplicationEvent {

	private static final long serialVersionUID = -5977065467292706593L;
	
	private String processDefinitionName;
	private Long processInstanceId;
	private Map<String, Object> variables;
	
	public VariableCreatedEvent(Object source, String processDefinitionName, Long processInstanceId, Map<String, Object> variables) {
		super(source);
		
		this.processDefinitionName = processDefinitionName;
		this.processInstanceId = processInstanceId;
		this.variables = variables;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}
	
	public Long getProcessInstanceId() {
		return processInstanceId;
	}
}