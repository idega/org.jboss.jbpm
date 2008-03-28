package com.idega.jbpm.exe.impl;

/**
 * the actual persisting and resolving is left to BinaryVariableHandler
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/28 12:11:00 $ by $Author: civilis $
 */
public interface BinaryVariable {

	public abstract String getIdentifier();

	public abstract void setIdentifier(String identifier);

	public abstract String getFileName();

	public abstract void setFileName(String fileName);

	public abstract String getStorageType();

	public abstract void setStorageType(String storageType);
}