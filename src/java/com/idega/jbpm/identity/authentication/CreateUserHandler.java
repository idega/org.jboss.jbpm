package com.idega.jbpm.identity.authentication;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.EmailHome;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.text.Name;

/**
 *  Jbpm action handler, which creates ic_user by user personal data object information.
 *  Stores result (ic_user id) to variable provided.
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 * 
 * Last modified: $Date: 2008/09/19 16:31:09 $ by $Author: civilis $
 */
public class CreateUserHandler implements ActionHandler {

	private static final long serialVersionUID = -1181069105207752204L;
	private String userDataExp;
	
	public void execute(ExecutionContext ectx) throws Exception {

		if(getUserDataExp() != null) {

			UserPersonalData upd = (UserPersonalData)JbpmExpressionEvaluator.evaluate(getUserDataExp(), ectx);
			
			if(upd.getUserId() == null) {
			
				String personalId = upd.getPersonalId();
				
				if(personalId != null) {
				
					FacesContext fctx = FacesContext.getCurrentInstance();
					IWApplicationContext iwac;
					
					if(fctx == null) {
						iwac = IWMainApplication.getDefaultIWApplicationContext();
					} else
						iwac = IWMainApplication.getIWMainApplication(fctx).getIWApplicationContext();
					
					UserBusiness userBusiness = getUserBusiness(iwac);
					
					final User usrCreated;
					
					if(upd.getCreateWithLogin()) {
						
						String firstName;
						String middleName;
						String lastName;
						String userName;
						
						if(upd.getFullName() != null) {
							
							Name name = new Name(upd.getFullName());
							firstName = name.getFirstName();
							middleName = name.getMiddleName();
							lastName = name.getLastName();
							
						} else {
							
							firstName = upd.getFirstName();
							middleName = null;
							lastName = upd.getLastName();
						}
						
						if((userName = upd.getUserName()) == null)
							userName = upd.getPersonalId();
						
						String password = LoginDBHandler.getGeneratedPasswordForUser();
						upd.setUserPassword(password);
						
//						doesn't check if login already exists, therefore this check needs to be made before calling this
						usrCreated = userBusiness.createUserWithLogin(
								firstName, middleName, lastName, null, null, null, null, null, userName, password, Boolean.TRUE, IWTimestamp.RightNow(), 5000, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, null);
					} else {
					
						if(upd.getFullName() != null) {
						
							usrCreated = userBusiness.createUserByPersonalIDIfDoesNotExist(upd.getFullName(), personalId, null, null);
						} else {
							usrCreated = userBusiness.createUserByPersonalIDIfDoesNotExist(upd.getFirstName(), null, upd.getLastName(), personalId, null, null);							
						}
					}
					
//					TODO: populated user with other personal data here
					
					if(upd.getUserEmail() != null) {
						
						EmailHome eHome = userBusiness.getEmailHome();
						Email uEmail = eHome.create();
						uEmail.setEmailAddress(upd.getUserEmail());
						uEmail.store();
						usrCreated.addEmail(uEmail);
					}
					
					LocateUserHandler.updateAddress(userBusiness, usrCreated, upd);

					if(upd.getUserPhone() != null)
						userBusiness.updateUserHomePhone(usrCreated, upd.getUserPhone());
					
//					put result back to user personal data
					
					final Object pk = usrCreated.getPrimaryKey();
//					crappy -no datatype pk- handling
					final Integer usrId = pk instanceof Integer ? (Integer)pk : new Integer(pk.toString());
					
					upd.setUserId(usrId);
					
				} else {
				
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to create user, but no personalId found in UserPersonalData. Skipping.");
				}
				
			} else {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to create user, but UserPersonalData already contained userId="+upd.getPersonalId());
			}
		}
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public String getUserDataExp() {
		return userDataExp;
	}

	public void setUserDataExp(String userDataExp) {
		this.userDataExp = userDataExp;
	}
}