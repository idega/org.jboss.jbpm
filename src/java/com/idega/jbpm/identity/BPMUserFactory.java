package com.idega.jbpm.identity;

import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $ Last modified: $Date: 2009/01/22 11:12:35 $ by $Author: civilis $
 */
public interface BPMUserFactory {
	
	public abstract BPMUser createBPMUser(UserPersonalData upd, Role role,
	        long processInstanceId);
	
	/**
	 * @return currently logged in user's bpm user. BPMUser is in session scope
	 */
	public abstract BPMUser getCurrentBPMUser();
	
	/**
	 * @return bpm user and sets the usr as the related real user. BPMUser is in session scope
	 */
	public abstract BPMUser getLoggedInBPMUser(String bpmUserUUID, User usr);
	
	/**
	 * @param iwc
	 * @param bpmUserUUID
	 *            user entity bean primary key of bpm user optional, if it is null, then it is
	 *            resolved from request parameter, or bpmuser is resolved from session
	 * @param usr
	 * @return bpm user and sets the usr as the relatead real user. BPMUser is in session scope
	 */
	public abstract BPMUser getLoggedInBPMUser(IWContext iwc,
	        String bpmUserUUID, User usr);
	
	/**
	 * @param bpmUserPK
	 *            user entity bean primary key of bpm user optional, if it is null, then it is
	 *            resolved from request parameter, or bpmuser is resolved from session
	 * @return bpm user in prototype scope
	 */
	public abstract BPMUser getBPMUser(Integer bpmUserPK);
	
	/**
	 * checks if realUsr is associated with bpmUsr.
	 * 
	 * @param realUsr
	 * @param bpmUsr
	 * @param autoAssociate
	 *            if true, the association is made if it's not associated already
	 * @return
	 */
	public abstract boolean isAssociated(User realUsr, User bpmUsr,
	        boolean autoAssociate);
}