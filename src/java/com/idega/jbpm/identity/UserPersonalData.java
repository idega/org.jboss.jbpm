package com.idega.jbpm.identity;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;

import com.idega.data.IDOLookup;
import com.idega.user.data.Gender;
import com.idega.user.data.GenderHome;

/**
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/10/09 18:30:13 $ by $Author: civilis $
 */
public class UserPersonalData implements Serializable {

	private static final long serialVersionUID = -5359330743741377568L;
	private String userName;
	private String userPassword;
	private String personalId;
	private String firstName;
	private String lastName;
	private String fullName;
	private String userEmail;
	private String userType;
	private String userAddress;
	private String userPostalCode;
	private String userMunicipality;
	private String userPhone;
	private String genderName;
	private Boolean hideInContacts;
	private Boolean createWithLogin;
	private Boolean juridicalPerson;
	private String uuid;

	private Integer userId;

	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public String getPersonalId() {
		return personalId;
	}
	public void setPersonalId(String personalId) {
		this.personalId = personalId;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getUserEmail() {
		return userEmail;
	}
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}
	public String getUserAddress() {
		return userAddress;
	}
	public void setUserAddress(String userAddress) {
		this.userAddress = userAddress;
	}
	public String getUserPostalCode() {
		return userPostalCode;
	}
	public void setUserPostalCode(String userPostalCode) {
		this.userPostalCode = userPostalCode;
	}
	public String getUserMunicipality() {
		return userMunicipality;
	}
	public void setUserMunicipality(String userMunicipality) {
		this.userMunicipality = userMunicipality;
	}
	public String getUserPhone() {
		return userPhone;
	}
	public void setUserPhone(String userPhone) {
		this.userPhone = userPhone;
	}
	public Boolean getHideInContacts() {
		return hideInContacts != null && hideInContacts;
	}
	public void setHideInContacts(Boolean hideInContacts) {
		this.hideInContacts = hideInContacts;
	}
	public Boolean getCreateWithLogin() {
		return createWithLogin != null && createWithLogin;
	}
	public void setCreateWithLogin(Boolean createWithLogin) {
		this.createWithLogin = createWithLogin;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserPassword() {
		return userPassword;
	}
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
	public String getGenderName() {
		return genderName;
	}
	public void setGenderName(String genderName) {
		this.genderName = genderName;
	}
	public Gender getGender() {

		if(getGenderName() != null && getGenderName().length() != 0) {

			Gender gender;

			try {
				gender = getGenderHome().findByGenderName(getGenderName());

			} catch (FinderException e) {
				gender = null;
			}

			if(gender == null)
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "No gender found by gender name="+getGenderName());
			else
				return gender;
		}

		return null;
	}

	public GenderHome getGenderHome() {

		try {
			return (GenderHome) IDOLookup.getHome(Gender.class);

		} catch (RemoteException rme) {
			throw new RuntimeException(rme.getMessage());
		}
	}
	public Boolean getJuridicalPerson() {
		return juridicalPerson;
	}
	public void setJuridicalPerson(Boolean juridicalPerson) {
		this.juridicalPerson = juridicalPerson;
	}
	public void setJuridicalPerson(String juridicalPerson) {
		if (juridicalPerson != null) {
			this.juridicalPerson = juridicalPerson.toLowerCase().equals("true") ? Boolean.TRUE : Boolean.FALSE ;
		}
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}