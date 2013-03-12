package com.idega.jbpm.exe;

public interface BPMEmailDocument extends BPMDocument {

	public String getSubject();
	public String getMessage();
	public String getFromPersonal();
	public String getFromAddress();
	public void setSubject(String subject);
	public void setMessage(String message);
	public void setFromPersonal(String fromPersonal);
	public void setFromAddress(String fromAddress);

}