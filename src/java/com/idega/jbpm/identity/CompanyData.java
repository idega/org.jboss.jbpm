package com.idega.jbpm.identity;

import java.io.Serializable;

public class CompanyData implements Serializable {

	private static final long serialVersionUID = 8182426866826289395L;

	private String ssn;
	private String name;
	private String address;
	private String postalCode;
	
	public String getSsn() {
		return ssn;
	}
	public void setSsn(String ssn) {
		this.ssn = ssn;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPostalCode() {
		return postalCode;
	}
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
}
