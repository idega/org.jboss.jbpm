package com.idega.jbpm.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.user.data.User;

/**
 *  
 * Wrapper of at least one, or two user entities, which correspond to bpm-user account and/or to logged-in user's account. <br />
 * bpm-user account represents shared account, which unifies not logged in users, and/or logged in users. <br /> 
 * Use case example is when user gets to the process by following link.
 *   
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/05/24 10:25:51 $ by $Author: civilis $
 */
@Scope("session")
//TODO: change scope to prototype and use seam conversation scope (after seam integration of course) 
@Service("BPMUser")
public class BPMUserImpl implements BPMUser {
	
	public static final String bpmUsrParam = "bpmusr";

	private Boolean isAssociated;
	private User bpmUser;
	private User realUser;
	private BPMUserFactory bpmUserFactory;
	
	public User getBpmUser() {
		return bpmUser;
	}
	public void setBpmUser(User bpmUser) {
		setIsAssociated(null);
		this.bpmUser = bpmUser;
	}
	public User getRealUser() {
		return realUser;
	}
	public void setRealUser(User realUser) {
		setIsAssociated(null);
		this.realUser = realUser;
	}
	public Boolean getIsAssociated() {
		
		if(isAssociated == null) {

			if(getBpmUser() != null && getRealUser() != null) {
				
				isAssociated = getBpmUserFactory().isAssociated(getRealUser(), getBpmUser(), true);
			}
		}
		
		return isAssociated == null ? false : isAssociated;
	}
	public void setIsAssociated(Boolean isAssociated) {
		this.isAssociated = isAssociated;
	}
	
	public BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}
	@Autowired
	public void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}
	public Integer getIdToUse() {
		
		Object pk;
	
		if(getRealUser() != null && (getIsAssociated() || getBpmUser() == null)) {
			
			pk = getRealUser().getPrimaryKey();
		} else if(getBpmUser() != null) {
			pk = getBpmUser().getPrimaryKey();
		} else
			pk = null;
		
		if(pk != null) {
			
			if(pk instanceof Integer)
				return (Integer)pk;
			else
				return new Integer(pk.toString());
		}
		
		return null;
	}
}