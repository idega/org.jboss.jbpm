package com.idega.jbpm.identity;

import java.io.Serializable;

/**
 *  
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 * Last modified: $Date: 2008/06/21 16:47:53 $ by $Author: civilis $
 */
public class UserPersonalData implements Serializable {
	
	private static final long serialVersionUID = -5359330743741377568L;
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
	private Boolean hideInContacts;
	
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
}