package com.idega.jbpm.identity;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.EmailHome;
import com.idega.core.persistence.GenericDao;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.presentation.IWContext;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 * 
 * Last modified: $Date: 2008/06/06 16:33:48 $ by $Author: civilis $
 */
public abstract class BPMUserFactoryImpl implements BPMUserFactory {

	private static final Logger logger = Logger.getLogger(BPMUserFactoryImpl.class.getName());
	
	private BPMFactory BPMFactory; 
	private GenericDao genericDao;
	
	public User createBPMUser(String name, String roleName, String email, long processInstanceId) {
	
		try {
			FacesContext fctx = FacesContext.getCurrentInstance();
			IWApplicationContext iwac;
			
			if(fctx == null) {
				iwac = IWMainApplication.getDefaultIWApplicationContext();
			} else
				iwac = IWMainApplication.getIWMainApplication(fctx).getIWApplicationContext();
			
			UserBusiness userBusiness = getUserBusiness(iwac);
			
			User bpmUserAcc = null;
			
			if(email != null) {
				
				Collection<User> users = userBusiness.getUserHome().findUsersByEmail(email);
				
				if(users != null) {
				
					for (User user : users) {
						
						if(BPMUser.bpmUserIdentifier.equals(user.getMiddleName())) {
							bpmUserAcc = user;
							break;
						}
					}
					
					if(bpmUserAcc == null && !users.isEmpty()) {
						bpmUserAcc = users.iterator().next();
					}
				}
				
				//bpmUserAcc
				
				if(bpmUserAcc == null) {
					
					bpmUserAcc = userBusiness.createUser(name, BPMUser.bpmUserIdentifier, roleName, String.valueOf(processInstanceId));
					
					EmailHome eHome = userBusiness.getEmailHome();
					Email uEmail = eHome.create();
					uEmail.setEmailAddress(email);
					uEmail.store();
					bpmUserAcc.addEmail(uEmail);
				}
				
			} else {
				bpmUserAcc = userBusiness.createUser(name, BPMUser.bpmUserIdentifier, roleName, String.valueOf(processInstanceId));
			}
			
			return bpmUserAcc;
			
		} catch (Exception e) {
			Logger.getLogger(BPMUserFactoryImpl.class.getName()).log(Level.SEVERE, "Exception while creating bpm user", e);
			return null;
		}
	}
	
	public abstract BPMUserImpl createUser();
	
	public BPMUser getCurrentBPMUser() {
		
		return getBPMUser(null, null);
	}
	
	public BPMUser getBPMUser(Integer bpmUserPK, User usr) {
		return getBPMUser(IWContext.getCurrentInstance(), bpmUserPK, usr);
	}
	
	public BPMUser getBPMUser(IWContext iwc, Integer bpmUserPK, User usr) {
		
		if(iwc == null)
			return null;
		
		BPMUserImpl bpmUsr = createUser();
		
		if(bpmUserPK != null && (bpmUsr.getBpmUser() == null || !bpmUsr.getBpmUser().getPrimaryKey().equals(bpmUserPK))) {
			
			IWApplicationContext iwac = IWMainApplication.getIWMainApplication(iwc).getIWApplicationContext();
			
			try {
				User bpmUsrAcc = getUserBusiness(iwac).getUser(bpmUserPK);
				
				if(bpmUsrAcc == null) {
					
					logger.log(Level.WARNING, "no BPMUser account found by bpmUserPK provided: "+bpmUserPK);
					
				} else {
					bpmUsr.setBpmUser(bpmUsrAcc);
				}
				
			} catch (RemoteException e) {
				throw new IBORuntimeException(e);
			}
		}
		
		if(usr == null) {
			
			if(iwc != null && iwc.isLoggedOn())
				usr = iwc.getCurrentUser();
		}
		
		if(usr != null && (bpmUsr.getRealUser() == null || !bpmUsr.getRealUser().getPrimaryKey().equals(usr.getPrimaryKey()))) {

			bpmUsr.setRealUser(usr);
		}
		
		return bpmUsr;
	}
	
	@Transactional(readOnly = true)
	public boolean isAssociated(User realUsr, User bpmUsr, boolean autoAssociate) {
		
		if(bpmUsr.equals(realUsr))
			return true;
		
		try {
			String roleName = bpmUsr.getLastName();
			String processInstanceIdStr = bpmUsr.getPersonalID();
			
			if(roleName != null && !roleName.equals(CoreConstants.EMPTY) && processInstanceIdStr != null && !processInstanceIdStr.equals(CoreConstants.EMPTY)) {

				Long processInstanceId = new Long(processInstanceIdStr);

				ArrayList<String> prn = new ArrayList<String>(1);
				prn.add(roleName);
				
				List<ProcessRole> proles = getBPMFactory().getRolesManager().getProcessRoles(prn, processInstanceId);
				
				if(proles != null && !proles.isEmpty()) {
					
					ProcessRole prole = proles.iterator().next();
					
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
							ni.setProcessRole(prole);
							nis.add(ni);
						}
						
						if(!foundForBPMUsr) {
							
							NativeIdentityBind ni = new NativeIdentityBind();
							ni.setIdentityId(bpmUsr.getPrimaryKey().toString());
							ni.setIdentityType(IdentityType.USER);
							ni.setProcessRole(prole);
							nis.add(ni);
						}
						
						getGenericDao().persist(prole);
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
	
	public GenericDao getGenericDao() {
		return genericDao;
	}

	@Resource(name="genericDAO")
	public void setGenericDao(GenericDao genericDao) {
		this.genericDao = genericDao;
	}
	
	public BPMFactory getBPMFactory() {
		return BPMFactory;
	}

	@Autowired
	public void setBPMFactory(BPMFactory factory) {
		BPMFactory = factory;
	}
}