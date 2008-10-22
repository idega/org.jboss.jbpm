package com.idega.jbpm.identity;

import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 * 
 * Last modified: $Date: 2008/10/22 15:10:53 $ by $Author: civilis $
 */
public interface BPMUserFactory {

	public abstract User createBPMUser(UserPersonalData upd, Role role, long processInstanceId);

	/**
	 * @return currently logged in user's bpm user. BPMUser is in session scope
	 */
	public abstract BPMUser getCurrentBPMUser();

	/**
	 * @return bpm user and sets the usr as the relatead real user. BPMUser is in session scope
	 */
	public abstract BPMUser getLoggedInBPMUser(Integer bpmUserPK, User usr);
	
	/**
	 * @return bpm user and sets the usr as the relatead real user. BPMUser is in session scope
	 */
	public abstract BPMUser getLoggedInBPMUser(IWContext iwc, Integer bpmUserPK, User usr);
	
	/**
	 * 
	 * @param iwc
	 * @param bpmUserPK - user entity bean primary key of bpm user
	 * @return bpm user in prototype scope
	 */
	public abstract BPMUser getBPMUser(IWContext iwc, Integer bpmUserPK);

	public abstract boolean isAssociated(User realUsr, User bpmUsr,
			boolean autoAssociate);
}