package com.idega.jbpm.business;

import java.util.Collection;

import com.idega.jbpm.bean.JBPMCompany;
import com.idega.user.data.User;

public interface JBPMCompanyBusiness{
	public static final String BEAN_NAME = "jBPMCompanyBusinessImpl";
	public Collection<JBPMCompany> getJBPMCompaniesForUser(User user);
}
