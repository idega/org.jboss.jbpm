package com.idega.jbpm.identity;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/05/19 15:46:44 $ by $Author: civilis $
 */
public abstract class BPMUserFactory {
	
	public BPMUser createBPMUser(String name) {
		
		try {
			User bpmUserAcc = ((com.idega.user.data.UserHome) com.idega.data.IDOLookup.getHome(User.class)).create();
			bpmUserAcc.setFirstName(name);
			bpmUserAcc.store();
			
			BPMUser user = createUser();
			user.setBpmUser(bpmUserAcc);
			
			return user;
			
		} catch (Exception e) {
			Logger.getLogger(BPMUserFactory.class.getName()).log(Level.SEVERE, "Exception while creating bpm user", e);
			return null;
		}
	}
	
	public abstract BPMUser createUser();

	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected GroupBusiness getGroupBusiness(IWApplicationContext iwac) {
		try {
			return (GroupBusiness) IBOLookup.getServiceInstance(iwac, GroupBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}