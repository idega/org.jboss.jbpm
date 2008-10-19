package com.idega.jbpm.identity;

import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 * 
 * Last modified: $Date: 2008/10/19 10:57:36 $ by $Author: civilis $
 */
public interface BPMUserFactory {

	public abstract User createBPMUser(UserPersonalData upd, Role role, long processInstanceId);

	public abstract BPMUserImpl createUser();

	public abstract BPMUser getCurrentBPMUser();

	public abstract BPMUser getBPMUser(Integer bpmUserPK, User usr);
	
	public abstract BPMUser getBPMUser(IWContext iwc, Integer bpmUserPK, User usr);

	public abstract boolean isAssociated(User realUsr, User bpmUsr,
			boolean autoAssociate);
}