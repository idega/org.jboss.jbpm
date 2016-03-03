package com.idega.jbpm.event;

import org.springframework.context.ApplicationEvent;

public class BPMContextSavedEvent extends ApplicationEvent {

	private static final long serialVersionUID = -2399630327984258149L;

	private Long procInstId;

	public BPMContextSavedEvent(Object source, Long procInstId) {
		super(source);

		this.procInstId = procInstId;
	}

	public Long getProcInstId() {
		return procInstId;
	}

	public void setProcInstId(Long procInstId) {
		this.procInstId = procInstId;
	}

	@Override
	public String toString() {
		return "Proc. inst. ID: " + getProcInstId();
	}

}