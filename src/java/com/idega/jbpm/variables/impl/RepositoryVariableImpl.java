package com.idega.jbpm.variables.impl;

import com.idega.business.IBOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;

public class RepositoryVariableImpl extends BinaryVariableImpl {

	private static final long serialVersionUID = 5094640907176930728L;

	public RepositoryVariableImpl() {
		setStorageType(BinaryVariablesHandlerImpl.STORAGE_TYPE);
	}
	
	public RepositoryVariableImpl(String repositoryUri) {
		this();
		
		if (repositoryUri != null) {
			if (!repositoryUri.startsWith(CoreConstants.WEBDAV_SERVLET_URI)) {
				repositoryUri = CoreConstants.WEBDAV_SERVLET_URI.concat(repositoryUri);
			}
			
			setIdentifier(repositoryUri);
		}
	}
	
	@Override
	public Object getPersistentResource() {
		try {
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
		} catch (Exception e) {}
		return null;
	}
}