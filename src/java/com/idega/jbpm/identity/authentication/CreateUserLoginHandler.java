package com.idega.jbpm.identity.authentication;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.user.business.StandardGroup;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;

/**
 * Creates login for user account. User account id (userId property) should be
 * in the UserPersonalData provided. Password is generated and personal id is
 * used as login name.
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2009/01/27 14:19:37 $ by $Author: civilis $
 */
@Service("createUserLoginHandler")
@Scope("prototype")
public class CreateUserLoginHandler implements ActionHandler {

	private static final long serialVersionUID = -1181069105207752204L;
	private UserPersonalData userData;
	@Autowired(required = false)
	private StandardGroup standardGroup;

	public void execute(ExecutionContext ectx) throws Exception {

		if (getUserData() != null) {

			UserPersonalData upd = getUserData();

			if (upd.getUserId() == null)
				throw new IllegalArgumentException(
						"Tried to create login for user account, but no userId found in userPersonalData");

			UserBusiness userBusiness = getUserBusiness();

			String password = LoginDBHandler.getGeneratedPasswordForUser();
			upd.setUserPassword(password);

			User usr = userBusiness.getUser(upd.getUserId());

			userBusiness.createUserLogin(usr, upd.getPersonalId(), password,
					true, IWTimestamp.RightNow(), 5000, false, true, false,
					null);

		} else {
			Logger.getLogger(getClass().getName()).log(
					Level.WARNING,
					"Called create user handler, but no user data provided. Process instance id="
							+ ectx.getProcessInstance().getId());
		}
	}

	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(
					IWMainApplication.getDefaultIWApplicationContext(),
					UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public StandardGroup getStandardGroup() {

		return standardGroup;
	}

	public void setStandardGroup(StandardGroup standardGroup) {
		this.standardGroup = standardGroup;
	}

	public UserPersonalData getUserData() {
		return userData;
	}

	public void setUserData(UserPersonalData userData) {
		this.userData = userData;
	}
}