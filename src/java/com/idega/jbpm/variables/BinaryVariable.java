package com.idega.jbpm.variables;

import java.util.Date;
import java.util.Map;

import com.idega.block.process.variables.Variable;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.12 $ Last modified: $Date: 2009/03/30 13:14:27 $ by $Author: civilis $
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

	public abstract void setMimeType(String mimeType);

	public abstract void setContentLength(Long contentLength);

	public abstract Long getContentLength();

	public abstract Boolean getSigned();

	public abstract void setSigned(Boolean signed);

	public abstract Boolean getHidden();

	public abstract void setHidden(Boolean hidden);

	public abstract Variable getVariable();

	public abstract void setVariable(Variable var);

	/**
	 * updates existing binary variable values - doesn't persist if the variable isn't already
	 * persisted
	 */
	public abstract void update();

	/**
	 * persists variable using it's internal persisting logic. Should be called after it has been
	 * created only. Shouldn't work after resolving it from persistent state.
	 */
	public abstract void persist();

	public abstract void setTaskInstanceId(long taskInstanceId);

	public abstract long getTaskInstanceId();

	public boolean isPersistedToRepository();
	public void setPersistedToRepository(boolean persistedToRepository);

	public abstract boolean isPersisted();

	public abstract boolean isSignable();

	public Map<String, Object> getMetadata();

	public void setMetadata(Map<String, Object> metadata);

	/**
	 * @return resource for binaryVariable, which reflects the actual persistence method. The one
	 *         used now is WebdavExtendedResource. TODO: we should either use here the standard
	 *         filesystem resource api, or create our own
	 */
	public abstract Object getPersistentResource();

	public Date getDate();

	public void setDate(Date date);

}