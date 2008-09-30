package com.idega.jbpm.signing;

public interface Signsdf {

	//	IWResourceBundle iwrb, String taskInstanceId, String hashValue, String image, String uri, String message,
	//	String errorMessage
	public abstract String getSigningAction(Long taskInstanceId,
			String hashValue, String message, String errorMessage);

}