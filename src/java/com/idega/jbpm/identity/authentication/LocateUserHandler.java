package com.idega.jbpm.identity.authentication;

import java.util.Collection;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;

/**
 * Jbpm action handler, which searches for ic_user by personal id, or email address.
 * If ic_user found, updates missing user data by user personal data provided.
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/06/12 18:29:53 $ by $Author: civilis $
 */
public class LocateUserHandler implements ActionHandler {

	private static final long serialVersionUID = -3732028335572353838L;
	private String userDataExp;
	
	public void execute(ExecutionContext ectx) throws Exception {

		if(getUserDataExp() != null) {

			final UserPersonalData upd = (UserPersonalData)JbpmExpressionEvaluator.evaluate(getUserDataExp(), ectx);
			
			final String personalId = upd.getPersonalId();
			
			final FacesContext fctx = FacesContext.getCurrentInstance();
			final IWApplicationContext iwac;
			
			if(fctx == null) {
				iwac = IWMainApplication.getDefaultIWApplicationContext();
			} else
				iwac = IWMainApplication.getIWMainApplication(fctx).getIWApplicationContext();
			
			final UserBusiness userBusiness = getUserBusiness(iwac);
			final User usrFound;
			
			if(personalId != null && !CoreConstants.EMPTY.equals(personalId)) {
				
//				lookup by personal id if present
				usrFound = userBusiness.getUser(personalId);
				
			} else if(upd.getUserEmail() != null) {
				
//				degrade to lookup by email if present
				
				Collection<User> users = userBusiness.getUserHome().findUsersByEmail(upd.getUserEmail());
				
				if(users != null && !users.isEmpty()) {
					usrFound = users.iterator().next();
				} else
					usrFound = null;
			} else
//				not found. lookup by name could be performed here
				usrFound = null;
			
			upd.setFirstName("blabla");
			
			if(usrFound != null) {
				
//				TODO: update user data here
			
//				put result back to user personal data
				
				final Object pk = usrFound.getPrimaryKey();
//				crappy -no datatype pk- handling
				final Integer usrId = pk instanceof Integer ? (Integer)pk : new Integer(pk.toString());
				
				upd.setUserId(usrId);
			}
		}
	}

	public String getUserDataExp() {
		return userDataExp;
	}

	public void setUserDataExp(String userDataExp) {
		this.userDataExp = userDataExp;
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}