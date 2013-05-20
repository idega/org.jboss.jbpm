package com.idega.jbpm.event;

import org.springframework.context.ApplicationEvent;

public class ProcessInstanceCreatedEvent extends ApplicationEvent {

	private static final long serialVersionUID = 8587349554332579987L;

	private String processDefinitionName;
	private Long processInstanceId;

	public ProcessInstanceCreatedEvent(String processDefinitionName, Long processInstanceId) {
		super(processDefinitionName);

		this.processDefinitionName = processDefinitionName;
		this.processInstanceId = processInstanceId;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	@Override
	public String toString() {
		return "Notification about new process instance (ID: " + getProcessInstanceId() + ") for proc. def. with name '" + getProcessDefinitionName() +
				"'";
	}

}