package com.idega.jbpm.variables.impl;

import com.idega.repository.bean.RepositoryItem;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/30 17:25:35 $ by $Author: civilis $
 */
public class WebdavBinaryVariableImpl extends BinaryVariableImpl {

	private static final long serialVersionUID = -1208364150392494130L;

	private RepositoryItem webdavResource;

	public WebdavBinaryVariableImpl() {

		setStorageType(BinaryVariablesHandlerImpl.STORAGE_TYPE);
	}

	public WebdavBinaryVariableImpl(RepositoryItem webdavResource) {
		this.webdavResource = webdavResource;
		setStorageType(BinaryVariablesHandlerImpl.STORAGE_TYPE);
		String path = webdavResource.getPath();
		if (path != null && !path.startsWith(CoreConstants.WEBDAV_SERVLET_URI)) {
			path = CoreConstants.WEBDAV_SERVLET_URI.concat(path);
		}
		setIdentifier(path);
	}

	@Override
	public Object getPersistentResource() {
		if (webdavResource != null)
			return webdavResource;
		else
			return super.getPersistentResource();
	}
}