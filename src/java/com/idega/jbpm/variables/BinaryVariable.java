package com.idega.jbpm.variables;

import com.idega.block.process.variables.Variable;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/10/15 14:56:40 $ by $Author: civilis $
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
	
	public abstract void setHash(int hash);
	
	public abstract String getMimeType();
	
	public abstract void setContentLength(Long contentLength);
	
	public abstract Long getContentLength();
	
	public abstract Boolean getSigned();
	
	public abstract void setSigned(Boolean signed);
	
	public abstract Boolean getHidden();

	public abstract void setHidden(Boolean hidden);
	
	public abstract Variable getVariable();
	
	public abstract void setVariable(Variable var);
	
	/**
	 * updates existing binary variable values - doesn't persist if the variable isn't already persisted
	 */
	public abstract void update();
	
	/**
	 * persists variable using it's internal persisting logic. 
	 * Should be called after it has been created only. Shouldn't work after resolving it from persistent state.
	 */
	public abstract void persist();
	
	public abstract void setTaskInstanceId(long taskInstanceId);
	
	public abstract long getTaskInstanceId();
	
	public abstract boolean isPersisted();
}