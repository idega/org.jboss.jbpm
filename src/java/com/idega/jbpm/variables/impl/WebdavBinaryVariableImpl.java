package com.idega.jbpm.variables.impl;

import org.apache.webdav.lib.WebdavResource;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/30 17:25:35 $ by $Author: civilis $
 */
public class WebdavBinaryVariableImpl extends BinaryVariableImpl {
	
	private static final long serialVersionUID = -1208364150392494130L;
	
	private WebdavResource webdavResource;
	
	public WebdavBinaryVariableImpl() {
		
		setStorageType(BinaryVariablesHandlerImpl.STORAGE_TYPE);
	}
	
	public WebdavBinaryVariableImpl(WebdavResource webdavResource) {
		this.webdavResource = webdavResource;
		setStorageType(BinaryVariablesHandlerImpl.STORAGE_TYPE);
		setIdentifier(webdavResource.getPath());
	}
	
	@Override
	public Object getPersistentResource() {
		
		if (webdavResource != null)
			return webdavResource;
		else
			return super.getPersistentResource();
	}
}