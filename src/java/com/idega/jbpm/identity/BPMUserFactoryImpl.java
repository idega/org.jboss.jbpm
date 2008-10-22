package com.idega.jbpm.identity;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.faces.context.FacesContext;

import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.EmailHome;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.17 $
 * 
 * Last modified: $Date: 2008/10/22 15:11:29 $ by $Author: civilis $
 */
public abstract class BPMUserFactoryImpl implements BPMUserFactory {

	private static final Logger logger = Logger.getLogger(BPMUserFactoryImpl.class.getName());
	
	private BPMFactory BPMFactory; 
	private BPMDAO bpmDAO;
	
	/**
	 * creates ic_user for BpmUser and assigns to the role provided
	 */
	public User createBPMUser(UserPersonalData upd, Role role, long processInstanceId) {
	
		try {
			FacesContext fctx = FacesContext.getCurrentInstance();
			IWApplicationContext iwac;
			
			if(fctx == null) {
				iwac = IWMainApplication.getDefaultIWApplicationContext();
			} else
				iwac = IWMainApplication.getIWMainApplication(fctx).getIWApplicationContext();
			
			UserBusiness userBusiness = getUserBusiness(iwac);
			
			User bpmUserAcc = null;
			String email = upd.getUserEmail();

			if(email != null) {
				
//				try to find existing bpmUser by email
				
				Collection<User> users = userBusiness.getUserHome().findUsersByEmail(email);
				
				if(users != null) {
					
					String processInstanceIdStr = String.valueOf(processInstanceId);
				
					for (User user : users) {

						String usrType = user.getMetaData(BPMUser.USER_TYPE);
						String userProcessInstanceIdStr = user.getMetaData(BPMUser.PROCESS_INSTANCE_ID);
						
						if(processInstanceIdStr.equals(userProcessInstanceIdStr) && (BPMUser.USER_TYPE_NATURAL.equals(usrType) || BPMUser.USER_TYPE_LEGAL.equals(usrType))) {
//							found bpmUser 
							bpmUserAcc = user;
							break;
						}
					}
				}
				
				if(bpmUserAcc == null) {
					
//					no bpm user found for email address -> creating new one
					bpmUserAcc = createBpmUserAcc(userBusiness, upd, role, processInstanceId);
					
					EmailHome eHome = userBusiness.getEmailHome();
					Email uEmail = eHome.create();
					uEmail.setEmailAddress(email);
					uEmail.store();
					bpmUserAcc.addEmail(uEmail);
				}
//				TODO: add support for possible invitation for more than one role for the same email, i.e. bpm user account found by personal id, but the role doesn't match - add another
				
			} else {
//				no email found to look for existing bpm users -> just create new bpmUser
				bpmUserAcc = createBpmUserAcc(userBusiness, upd, role, processInstanceId);
			}
			
			ProcessInstance pi = getBPMFactory()
				.getProcessManagerByProcessInstanceId(processInstanceId)
				.getProcessInstance(processInstanceId)
				.getProcessInstance();
			
			ArrayList<Role> rolz = new ArrayList<Role>(1);
			rolz.add(role);
			
			assignBPMUserToRoles(pi, rolz, bpmUserAcc.getPrimaryKey().toString());
			
			return bpmUserAcc;
			
		} catch (Exception e) {
			Logger.getLogger(BPMUserFactoryImpl.class.getName()).log(Level.SEVERE, "Exception while creating bpm user", e);
			return null;
		}
	}
	
	protected void assignBPMUserToRoles(ProcessInstance pi, List<Role> rolz, String userId) {
		
		RolesManager rolesManager = getBPMFactory().getRolesManager();
		
		rolesManager.createProcessRoles(pi.getProcessDefinition().getName(), rolz, pi.getId());
		rolesManager.createIdentitiesForRoles(rolz, userId, IdentityType.USER, pi.getId());
	}
	
	private User createBpmUserAcc(UserBusiness userBusiness, UserPersonalData upd, Role role, long processInstanceId) throws CreateException, RemoteException {
		
		final User bpmUserAcc;

		final String pref = String.valueOf(System.currentTimeMillis());
		String personalIdToUse;
		
		if(upd.getPersonalId() != null) {
			
			personalIdToUse = pref+upd.getPersonalId();
		} else
			personalIdToUse = pref;
		
		int truncateToLength = 19;
		
		if(personalIdToUse.length() > truncateToLength) {

//			truncating to the limit of userbmpbean property length or long length
			personalIdToUse = personalIdToUse.substring(personalIdToUse.length()-truncateToLength);
		}
		
		if(BPMUser.USER_TYPE_NATURAL.equals(upd.getUserType())) {

			if(upd.getFullName() != null) {
				
				bpmUserAcc = userBusiness.createUser(null, null, null, null, personalIdToUse, null, null, null, null, upd.getFullName());
			} else {
			
				bpmUserAcc = userBusiness.createUser(upd.getFirstName(), null, upd.getLastName(), personalIdToUse);
			}
			
		} else if(BPMUser.USER_TYPE_LEGAL.equals(upd.getUserType())) {
			
//			for legal person always using full name
			bpmUserAcc = userBusiness.createUser(null, upd.getFullName(), null, personalIdToUse);
		} else
			throw new IllegalArgumentException("Illegal user type in user data. Type="+upd.getUserType());
		
//		meta data meant to store bpmUser related data
		bpmUserAcc.setMetaData(BPMUser.USER_TYPE, upd.getUserType());
		bpmUserAcc.setMetaData(BPMUser.USER_ROLE, role.getRoleName());
		bpmUserAcc.setMetaData(BPMUser.PROCESS_INSTANCE_ID, String.valueOf(processInstanceId));
		
		if(upd.getHideInContacts())
			bpmUserAcc.setMetaData(BPMUser.HIDE_IN_CONTACTS, CoreConstants.EMPTY);
		
		bpmUserAcc.store();
		
		return bpmUserAcc;
	}
	
	public abstract BPMUserImpl createBPMUser();
	public abstract BPMUserImpl createLoggedInBPMUser();
	
	
	/**
	 * @return currently logged in user's bpm user. BPMUser is in session scope
	 */
	public BPMUser getCurrentBPMUser() {
		
		return getLoggedInBPMUser(null, null);
	}
	
	/**
	 * @return bpm user and sets the usr as the relatead real user. BPMUser is in session scope
	 */
	public BPMUser getLoggedInBPMUser(Integer bpmUserPK, User usr) {
		return getLoggedInBPMUser(IWContext.getCurrentInstance(), bpmUserPK, usr);
	}
	
	/**
	 * @return bpm user and sets the usr as the relatead real user. BPMUser is in session scope
	 */
	public BPMUser getLoggedInBPMUser(IWContext iwc, Integer bpmUserPK, User usr) {
		
		BPMUserImpl bpmUsr = getBPMUser(iwc, bpmUserPK, true);
		
		if(usr == null) {
			
			if(iwc != null && iwc.isLoggedOn())
				usr = iwc.getCurrentUser();
		}
		
		if(usr != null && (bpmUsr.getRealUser() == null || !bpmUsr.getRealUser().getPrimaryKey().equals(usr.getPrimaryKey()))) {

			bpmUsr.setRealUser(usr);
		}
		
		return bpmUsr;
	}
	
	/**
	 * 
	 * @param iwc
	 * @param bpmUserPK - user entity bean primary key of bpm user
	 * @return bpm user in prototype scope
	 */
	public BPMUser getBPMUser(IWContext iwc, Integer bpmUserPK) {
		
		return getBPMUser(iwc, bpmUserPK, false);
	}
	
	private BPMUserImpl getBPMUser(IWContext iwc, Integer bpmUserPK, boolean sessionScope) {
		
		if(iwc == null) {
			logger.log(Level.WARNING, "Tried to get bpm user, but no IWContext provided");
			return null;
		}
		
		if(!sessionScope && bpmUserPK == null) {
			
			logger.log(Level.WARNING, "Tried to get bpm user, but no bpmUser primary key provided");
			return null;
		}
		
//		TODO: perhaps cache this bpmUser somewhere
		final BPMUserImpl bpmUsr;
		
		if(sessionScope) {
			bpmUsr = createLoggedInBPMUser();
		} else {
			bpmUsr = createBPMUser();
		}
		
		if(bpmUserPK != null && (bpmUsr.getBpmUser() == null || !bpmUsr.getBpmUser().getPrimaryKey().equals(bpmUserPK))) {
			
			IWApplicationContext iwac = IWMainApplication.getIWMainApplication(iwc).getIWApplicationContext();
			
			try {
				User bpmUsrAcc = getUserBusiness(iwac).getUser(bpmUserPK);
				
				if(bpmUsrAcc != null) {
					
					bpmUsr.setBpmUser(bpmUsrAcc);
					
				} else {
					logger.log(Level.WARNING, "no BPMUser account found by bpmUserPK provided: "+bpmUserPK);
				}
				
			} catch (RemoteException e) {
				throw new IBORuntimeException(e);
			}
		}
		
		return bpmUsr;
	}
	
	@Transactional(readOnly = true)
	public boolean isAssociated(User realUsr, User bpmUsr, boolean autoAssociate) {
		
		if(bpmUsr.equals(realUsr))
			return true;
		
		try {
			String roleName = bpmUsr.getMetaData(BPMUser.USER_ROLE);
			String processInstanceIdStr = bpmUsr.getMetaData(BPMUser.PROCESS_INSTANCE_ID);
			
			if(roleName != null && roleName.length() != 0 && processInstanceIdStr != null && processInstanceIdStr.length() != 0) {

				Long processInstanceId = new Long(processInstanceIdStr);

				ArrayList<String> prn = new ArrayList<String>(1);
				prn.add(roleName);
				
				List<Actor> proles = getBPMFactory().getRolesManager().getProcessRoles(prn, processInstanceId);
				
				if(proles != null && !proles.isEmpty()) {
					
					Actor prole = proles.iterator().next();
					
					List<NativeIdentityBind> nis = prole.getNativeIdentities();
					
					if(nis == null)
						nis = new ArrayList<NativeIdentityBind>(2);
					
					boolean foundForRealUsr = false;
					boolean foundForBPMUsr = false;
					
					for (NativeIdentityBind ni : nis) {
						
						if(!foundForRealUsr && ni.getIdentityType() == IdentityType.USER && ni.getIdentityId().equals(realUsr.getPrimaryKey().toString())) {
							
							foundForRealUsr = true;
							continue;
						}
							
						if(!foundForBPMUsr && ni.getIdentityType() == IdentityType.USER && ni.getIdentityId().equals(bpmUsr.getPrimaryKey().toString())) {
							
							foundForBPMUsr = true;
							continue;
						}
					}
					
					if(foundForRealUsr && foundForBPMUsr)
						return true;
					else if(autoAssociate) {
						
						if(!foundForRealUsr) {
							
							NativeIdentityBind ni = new NativeIdentityBind();
							ni.setIdentityId(realUsr.getPrimaryKey().toString());
							ni.setIdentityType(IdentityType.USER);
							ni.setActor(prole);
							nis.add(ni);
						}
						
						if(!foundForBPMUsr) {
							
							NativeIdentityBind ni = new NativeIdentityBind();
							ni.setIdentityId(bpmUsr.getPrimaryKey().toString());
							ni.setIdentityType(IdentityType.USER);
							ni.setActor(prole);
							nis.add(ni);
						}
						
						getBpmDAO().persist(prole);
						return true;
					}
				}
				
				
			} else {
				logger.log(Level.WARNING, "Tried to associate user with bpmUser, but bpmUser contained insufficient information. RoleName resolved="+roleName+", processInstanceId="+processInstanceIdStr);
			}
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while associating bpmUser with user provided.", e);
		}
		
		return false;
	}

	private UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
//	private GroupBusiness getGroupBusiness(IWApplicationContext iwac) {
//		try {
//			return (GroupBusiness) IBOLookup.getServiceInstance(iwac, GroupBusiness.class);
//		}
//		catch (IBOLookupException ile) {
//			throw new IBORuntimeException(ile);
//		}
//	}
	
	public BPMDAO getBpmDAO() {
		return bpmDAO;
	}

	@Autowired
	public void setBpmDAO(BPMDAO bpmDAO) {
		this.bpmDAO = bpmDAO;
	}

	public BPMFactory getBPMFactory() {
		return BPMFactory;
	}

	@Autowired
	public void setBPMFactory(BPMFactory factory) {
		BPMFactory = factory;
	}
}