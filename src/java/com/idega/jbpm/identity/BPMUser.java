package com.idega.jbpm.identity;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.user.data.User;

/**
 *  
 * Wrapper of at least one, or two user entities, which correspond to bpm-user account and-if to logged-in user's account. <br />
 * bpm-user account represents shared account, which unifies not logged in users, and/or logged in users. <br /> 
 * Use case example is when user gets to the process by following link.
 *   
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/05/19 15:46:44 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("BPMUser")
public class BPMUser {

	private User bpmUser;
	private User realUser;
	
	public User getBpmUser() {
		return bpmUser;
	}
	public void setBpmUser(User bpmUser) {
		this.bpmUser = bpmUser;
	}
	public User getRealUser() {
		return realUser;
	}
	public void setRealUser(User realUser) {
		this.realUser = realUser;
	}
}