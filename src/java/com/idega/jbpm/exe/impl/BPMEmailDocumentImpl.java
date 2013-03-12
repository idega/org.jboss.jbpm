package com.idega.jbpm.exe.impl;

import com.idega.jbpm.exe.BPMEmailDocument;

public class BPMEmailDocumentImpl extends BPMDocumentImpl implements BPMEmailDocument {

	private static final long serialVersionUID = -7116546416787997558L;

	private String subject, message, fromPersonal, fromAddress;

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String getFromPersonal() {
		return fromPersonal;
	}

	@Override
	public String getFromAddress() {
		return fromAddress;
	}

	@Override
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
	public void setFromPersonal(String fromPersonal) {
		this.fromPersonal = fromPersonal;
	}

	@Override
	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

}