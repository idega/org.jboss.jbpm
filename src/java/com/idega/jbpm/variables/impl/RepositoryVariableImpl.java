package com.idega.jbpm.variables.impl;

import com.idega.repository.RepositoryService;
import com.idega.util.CoreConstants;
import com.idega.util.expression.ELUtil;

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
			RepositoryService repository = ELUtil.getInstance().getBean(RepositoryService.BEAN_NAME);
			return repository;
		} catch (Exception e) {}
		return null;
	}
}