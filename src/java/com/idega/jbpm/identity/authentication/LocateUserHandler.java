package com.idega.jbpm.identity.authentication;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

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
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * Jbpm action handler, which searches for ic_user by personal id, or email
 * address. If ic_user found, updates missing user data by user personal data
 * provided.
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 *
 *          Last modified: $Date: 2009/06/08 14:29:25 $ by $Author: valdas $
 */
@Service("locateUserHandler")
@Scope("prototype")
public class LocateUserHandler implements ActionHandler {

	private static final long serialVersionUID = -3732028335572353838L;
	private UserPersonalData userData;

	@Override
	public void execute(ExecutionContext ectx) throws Exception {

		if (getUserData() != null) {

			final UserPersonalData upd = getUserData();

			final String personalId = upd.getPersonalId();

			final FacesContext fctx = FacesContext.getCurrentInstance();
			final IWApplicationContext iwac;

			if (fctx == null) {
				iwac = IWMainApplication.getDefaultIWApplicationContext();
			} else
				iwac = IWMainApplication.getIWMainApplication(fctx)
						.getIWApplicationContext();

			final UserBusiness userBusiness = getUserBusiness(iwac);
			User usrFound = null;

			if (!StringUtil.isEmpty(personalId)) {
				// lookup by personal id if present
				try {
					usrFound = userBusiness.getUser(personalId);
				} catch (IDOFinderException e) {
					usrFound = null;
				}
			}

			if (usrFound == null && upd.getUserEmail() != null) {
				// degrade to lookup by email if present
				Collection<User> users = userBusiness.getUserHome().findUsersByEmail(upd.getUserEmail());

				if (!ListUtil.isEmpty(users)) {
					for (Iterator<User> usersIter = users.iterator(); (usrFound == null && usersIter.hasNext());) {
						usrFound = usersIter.next();
						if (!personalId.equals(usrFound.getPersonalID())) {
							usrFound = null;
						}
					}
				}
			}

			if (usrFound != null) {

				//if (false)
					// TODO: finish update user info, and test
					//updateAddress(userBusiness, usrFound, upd);

				// s_upd.setUserAddress(userAddress);
				// s_upd.setUserPostalCode(userPostalCode);
				// s_upd.setUserMunicipality(userMunicipality);
				// s_upd.setUserPhone(userPhone);

				// userBusiness.

				// put result back to user personal data

				final Object pk = usrFound.getPrimaryKey();
				// crappy -no datatype pk- handling
				final Integer usrId = pk instanceof Integer ? (Integer) pk
						: new Integer(pk.toString());

				upd.setUserId(usrId);
			}
		} else {
			Logger.getLogger(getClass().getName()).log(
					Level.WARNING,
					"Called locate user handler, but no user data provided. Process instance id="
							+ ectx.getProcessInstance().getId());
		}
	}

	static void updateAddress(UserBusiness userBusiness, User usr,
			UserPersonalData upd) throws RemoteException, FinderException {

		try {

			// gather as much info about address provided as possible
			PostalCode postalCode;

			String postalCodeStr = upd.getUserPostalCode();
			String municipalityName = upd.getUserMunicipality();

			Commune commune;

			if (municipalityName != null) {

				try {
					commune = userBusiness.getAddressBusiness()
							.getCommuneHome().findByCommuneName(
									municipalityName);
				} catch (FinderException e) {
					commune = userBusiness.getAddressBusiness().createCommuneIfNotExisting(municipalityName);
				}

			} else
				commune = null;

			if (postalCodeStr != null) {

				try {
					postalCode = userBusiness.getAddressBusiness()
							.getPostalCodeHome()
							.findByPostalCode(postalCodeStr);
				} catch (FinderException e) {
					postalCode = userBusiness.getAddressBusiness().getPostalCodeAndCreateIfDoesNotExist(postalCodeStr, municipalityName);
				}

			} else
				postalCode = null;

			String streetName;
			String streetNr;

			if (upd.getUserAddress() != null) {

				streetName = userBusiness.getAddressBusiness()
						.getStreetNameFromAddressString(upd.getUserAddress());
				streetNr = userBusiness.getAddressBusiness()
						.getStreetNumberFromAddressString(upd.getUserAddress());
			} else {

				streetName = null;
				streetNr = null;
			}

			if (streetName != null && streetNr != null) {
				// try to find address, by street name and nr

				Collection<Address> allAddresses = usr.getAddresses();
				Address addrFound = null;

				for (Address address : allAddresses) {

					if (streetName.equals(address.getStreetName())
							&& streetNr.equals(address.getStreetNumber())) {
						// candidate

						if ((address.getPostalCode() != null && address
								.getPostalCode().equals(postalCode))
								|| (address.getCommune() != null && address
										.getCommune().equals(commune))) {

							// street is in either postal code area or commune -
							// found
							addrFound = address;
							break;
						}
					}
				}

				if (addrFound != null) {
					// found address - try to update missing info

					boolean needsUpdate = false;

					if (addrFound.getCommune() == null && commune != null)
						addrFound.setCommune(commune);

					if (addrFound.getPostalCode() == null && postalCode != null)
						addrFound.setPostalCode(postalCode);

					if (needsUpdate)
						// TODO: is the store necessary?
						addrFound.store();

				} else {
					// create new address

					Integer communeId = commune != null ? (commune.getPrimaryKey() instanceof Integer ? (Integer) commune.getPrimaryKey()
							: new Integer(commune.getPrimaryKey().toString()))
							: null;

					if (usr.getUsersMainAddress() == null) {
						userBusiness
								.updateUsersMainAddressOrCreateIfDoesNotExist(
										usr, upd.getUserAddress(), postalCode,
										null, null, null, null, communeId);

					} else {
						userBusiness
								.updateUsersCoAddressOrCreateIfDoesNotExist(
										usr, upd.getUserAddress(), postalCode,
										null, null, null, null, communeId);
					}
				}

			} else {
				Logger
						.getLogger(LocateUserHandler.class.getName())
						.log(Level.WARNING,
								"No street name or street nr resolved - skipping updating user address");
			}

		} catch (Exception e) {
			Logger.getLogger(LocateUserHandler.class.getName()).log(
					Level.SEVERE,
					"Exception while updating user address. User id="
							+ usr.getPrimaryKey(), e);
		}
	}

	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac,
					UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	public UserPersonalData getUserData() {
		return userData;
	}

	public void setUserData(UserPersonalData userData) {
		this.userData = userData;
	}
}