package com.idega.jbpm.identity.authentication;

import org.jbpm.security.AuthenticationService;

public class IdentityAuthenticationService implements AuthenticationService {

	private static final long serialVersionUID = 356148645298265464L;

	public String getActorId() {
		
		return "actor_id";
	}

	public void close() { }
}