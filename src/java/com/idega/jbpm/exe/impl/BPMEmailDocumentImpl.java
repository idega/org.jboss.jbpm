package com.idega.jbpm.exe.impl;

import com.idega.jbpm.exe.BPMEmailDocument;

public class BPMEmailDocumentImpl extends BPMDocumentImpl implements BPMEmailDocument{

	String subject;
	String fromPersonal;
	String fromAddress ;
	
	
	public String getSubject() {
		return subject;
	}
	public String getFromPersonal() {
		return fromPersonal;
	}
	public String getFromAddress() {
		return fromAddress;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public void setFromPersonal(String fromPersonal) {
		this.fromPersonal = fromPersonal;
	}
	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}
	
	
	
}
