package com.idega.jbpm.exe.impl;

import java.io.Serializable;

/**
 * the actual persisting and resolving is left to BinaryVariableHandler
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/28 12:11:00 $ by $Author: civilis $
 */
public class BinaryVariableImpl implements Serializable, BinaryVariable {

	private static final long serialVersionUID = -3000823293006596573L;
	private String fileName;
	private String identifier;
	private String storageType;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}
}