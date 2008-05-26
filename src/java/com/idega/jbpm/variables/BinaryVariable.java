package com.idega.jbpm.variables;

/**
 * the actual persisting and resolving is left to BinaryVariableHandler
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/05/26 14:13:32 $ by $Author: civilis $
 */
public interface BinaryVariable {
	
	public abstract String getDescription();
	
	public abstract void setDescription(String description);

	public abstract String getIdentifier();

	public abstract void setIdentifier(String identifier);

	public abstract String getFileName();

	public abstract void setFileName(String fileName);

	public abstract String getStorageType();

	public abstract void setStorageType(String storageType);
	
	public abstract Integer getHash();
	
	public abstract String getMimeType();
	
	public abstract Long getContentLength();
}