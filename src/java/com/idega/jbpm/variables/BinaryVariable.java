package com.idega.jbpm.variables;

/**
 * the actual persisting and resolving is left to BinaryVariableHandler
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/09/30 12:20:07 $ by $Author: valdas $
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
	
	public abstract Boolean getSigned();
	
	public abstract void setSigned(Boolean signed);
	
	public abstract Boolean getHidden();

	public abstract void setHidden(Boolean hidden);

}