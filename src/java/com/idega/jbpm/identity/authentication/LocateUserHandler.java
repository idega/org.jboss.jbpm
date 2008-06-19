package com.idega.jbpm.identity.authentication;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.location.data.Address;
import com.idega.core.location.data.Commune;
import com.idega.core.location.data.PostalCode;
import com.idega.data.IDOFinderException;
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
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/06/19 07:52:18 $ by $Author: civilis $
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
			User usrFound;
			
			if(personalId != null && !CoreConstants.EMPTY.equals(personalId)) {
				
//				lookup by personal id if present
				try {
					usrFound = userBusiness.getUser(personalId);
					
				} catch (IDOFinderException e) {
					usrFound = null;
				}

			} else
				usrFound = null;
				
			if(usrFound == null && upd.getUserEmail() != null) {
				
//				degrade to lookup by email if present
				
				Collection<User> users = userBusiness.getUserHome().findUsersByEmail(upd.getUserEmail());
				
				if(users != null && !users.isEmpty()) {
					usrFound = users.iterator().next();
				}
			}
			
			if(usrFound != null) {
				
				if(false)
//					TODO: finish update user info, and test
					updateAddress(userBusiness, usrFound, upd);
				
//				s_upd.setUserAddress(userAddress);
//                s_upd.setUserPostalCode(userPostalCode);
//                s_upd.setUserMunicipality(userMunicipality);
//                s_upd.setUserPhone(userPhone);
				
				//userBusiness.
			
//				put result back to user personal data
				
				final Object pk = usrFound.getPrimaryKey();
//				crappy -no datatype pk- handling
				final Integer usrId = pk instanceof Integer ? (Integer)pk : new Integer(pk.toString());
				
				upd.setUserId(usrId);
			}
		}
	}
	
	private void updateAddress(UserBusiness userBusiness, User usr, UserPersonalData upd) throws RemoteException, FinderException {
		
		try {
			
//			gather as much info about address provided as possible
			PostalCode postalCode;
			
			String postalCodeStr = upd.getUserPostalCode();
			String municipalityName = upd.getUserMunicipality();
			
			Commune commune = userBusiness.getAddressBusiness().getCommuneHome().findByCommuneName(municipalityName);
			
			if(postalCodeStr != null) {
				
				postalCode = userBusiness.getAddressBusiness().getPostalCodeHome().findByPostalCode(postalCodeStr);
			} else
				postalCode = null;
			
			String streetName;
			String streetNr;
			
			if(upd.getUserAddress() != null) {
			
				streetName = userBusiness.getAddressBusiness().getStreetNameFromAddressString(upd.getUserAddress());
				streetNr = userBusiness.getAddressBusiness().getStreetNumberFromAddressString(upd.getUserAddress());
			} else {
				
				streetName = null;
				streetNr = null;
			}

			if(streetName != null && streetNr != null) {
//				try to find address, by street name and nr
				
				@SuppressWarnings("unchecked")
				Collection<Address> allAddresses = usr.getAddresses();
				Address addrFound = null;
				
				for (Address address : allAddresses) {

					if(streetName.equals(address.getStreetName()) && streetNr.equals(address.getStreetNumber())) {
//						candidate
						
						if((address.getPostalCode() != null && address.getPostalCode().equals(postalCode)) ||
								(address.getCommune() != null && address.getCommune().equals(commune))	) {
							
//							street is in either postal code area or commune - found
							addrFound = address;
							break;
						}
					}
				}
				
				if(addrFound != null) {
//					found address - try to update missing info
					
					boolean needsUpdate = false;
					
					if(addrFound.getCommune() == null && commune != null)
						addrFound.setCommune(commune);
					
					if(addrFound.getPostalCode() == null && postalCode != null)
						addrFound.setPostalCode(postalCode);
					
					if(needsUpdate)
//						TODO: is the store necessary?
						addrFound.store();
					
				} else {
//					create new address
					
					Integer communeId = commune != null ? (commune.getPrimaryKey() instanceof Integer ? (Integer)commune.getPrimaryKey() : new Integer(commune.getPrimaryKey().toString())) : null;
					
					if(usr.getUsersMainAddress() == null) {
						userBusiness.updateUsersMainAddressOrCreateIfDoesNotExist(usr, upd.getUserAddress(), postalCode, null, null, null, null, communeId);
						
					} else {
						userBusiness.updateUsersCoAddressOrCreateIfDoesNotExist(usr, upd.getUserAddress(), postalCode, null, null, null, null, communeId);
					}
				}
				
			} else {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "No street name or street nr resolved - skipping updating user address");
			}
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while updating user address. User id="+usr.getPrimaryKey(), e);
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