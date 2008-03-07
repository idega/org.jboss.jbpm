package com.idega.jbpm.identity.authentication;

import javax.faces.context.FacesContext;

import org.jbpm.security.AuthenticationService;

import com.idega.presentation.IWContext;

public class IdentityAuthenticationService implements AuthenticationService {

	private static final long serialVersionUID = 356148645298265464L;

	public String getActorId() {
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		String currentUserId = String.valueOf(iwc.getCurrentUserId());
		return currentUserId;
	}

	public void close() { }
}