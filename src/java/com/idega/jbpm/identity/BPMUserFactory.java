package com.idega.jbpm.identity;

import javax.faces.context.FacesContext;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/05/19 13:52:39 $ by $Author: civilis $
 */
public class BPMUserFactory {

	public BPMUser createBPMUser() {
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		IWMainApplication iwma;
		
		if(fctx != null) {
			iwma = IWMainApplication.getIWMainApplication(fctx);
		} else
			iwma = IWMainApplication.getDefaultIWMainApplication();
		
		UserBusiness ub = getUserBusiness(iwma.getIWApplicationContext());
		
		//ub.create
		
		return null;
	}

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