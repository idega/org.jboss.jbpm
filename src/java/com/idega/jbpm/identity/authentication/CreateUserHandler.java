package com.idega.jbpm.identity.authentication;

import java.util.Date;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.EmailHome;
import com.idega.event.UserCreatedEvent;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.user.business.StandardGroup;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Gender;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.text.Name;

/**
 *  Jbpm action handler, which creates ic_user by user personal data object information.
 *  Stores result (ic_user id) to variable provided.
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 * 
 * Last modified: $Date: 2009/06/23 15:37:21 $ by $Author: valdas $
 */
@Service("createUserHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateUserHandler extends DefaultSpringBean implements ActionHandler {

	private static final long serialVersionUID = -1181069105207752204L;
	private UserPersonalData userData;
	@Autowired(required=false)
	private StandardGroup standardGroup;
	
	public void execute(ExecutionContext ectx) throws Exception {
		if (getUserData() == null) {
			getLogger().warning("Called create user handler, but no user data provided. Process instance id="+ectx.getProcessInstance().getId());
			return;
		}
		
		UserPersonalData upd = getUserData();
		if (upd.getUserId() != null) {
			getLogger().warning("Tried to create user, but UserPersonalData already contained userId="+upd.getPersonalId());
			return;
		}
		
		String personalId = upd.getPersonalId();
		if (StringUtil.isEmpty(personalId)) {
			getLogger().warning("Tried to create user, but no personalId found in UserPersonalData. Skipping.");
			return;
		}
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		IWApplicationContext iwac = fctx == null? IWMainApplication.getDefaultIWApplicationContext() : IWMainApplication.getIWMainApplication(fctx).getIWApplicationContext();
		UserBusiness userBusiness = getUserBusiness(iwac);
		
		Gender gender = upd.getGender();
		IWTimestamp dateOfBirth = null;
		if (upd.getPersonalId() != null && upd.getPersonalId().length() != 0) {
			Date dob = userBusiness.getUserDateOfBirthFromPersonalId(upd.getPersonalId());
			if (dob != null)
				dateOfBirth = new IWTimestamp(dob);
		}
		
		final User usrCreated;
		if (upd.getCreateWithLogin()) {
			String firstName;
			String middleName;
			String lastName;
			String userName;
			
			if (upd.getFullName() != null) {
				Name name = new Name(upd.getFullName());
				firstName = name.getFirstName();
				middleName = name.getMiddleName();
				lastName = name.getLastName();
			} else {
				firstName = upd.getFirstName();
				middleName = null;
				lastName = upd.getLastName();
			}
			
			if ((userName = upd.getUserName()) == null)
				userName = upd.getPersonalId();
				
			String password = LoginDBHandler.getGeneratedPasswordForUser();
			upd.setUserPassword(password);
			
			StandardGroup standardGroup = getStandardGroup();
			final Integer standardGroupPK;
			if (standardGroup != null)
				standardGroupPK = new Integer(standardGroup.getGroup().getPrimaryKey().toString());
			else
				standardGroupPK = null;
			
//			doesn't check if login already exists, therefore this check needs to be made before calling this
			usrCreated = userBusiness.createUserWithLogin(firstName, middleName, lastName, upd.getPersonalId(), null, null, 
						gender != null ? new Integer(gender.getPrimaryKey().toString()) : null,
						dateOfBirth, standardGroupPK, userName, password, Boolean.TRUE, IWTimestamp.RightNow(), 5000, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, null);
		} else {
			if (upd.getFullName() != null) {
				usrCreated = userBusiness.createUserByPersonalIDIfDoesNotExist(upd.getFullName(), personalId, gender, dateOfBirth);
			} else {
				usrCreated = userBusiness.createUserByPersonalIDIfDoesNotExist(upd.getFirstName(), null, upd.getLastName(), personalId, gender, dateOfBirth);							
			}
		}
		
//		TODO: populated user with other personal data here
		if (upd.getUserEmail() != null) {
			EmailHome eHome = userBusiness.getEmailHome();
			Email uEmail = eHome.create();
			uEmail.setEmailAddress(upd.getUserEmail());
			uEmail.store();
			usrCreated.addEmail(uEmail);
		}
		
		LocateUserHandler.updateAddress(userBusiness, usrCreated, upd);
		if (upd.getUserPhone() != null)
			userBusiness.updateUserHomePhone(usrCreated, upd.getUserPhone());

//		put result back to user personal data
		final Object pk = usrCreated.getPrimaryKey();
//		crappy -no datatype pk- handling
		final Integer usrId = pk instanceof Integer ? (Integer) pk : new Integer(pk.toString());
		upd.setUserId(usrId);
		
		ELUtil.getInstance().publishEvent(new UserCreatedEvent(this, usrCreated));
	}
	
	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
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