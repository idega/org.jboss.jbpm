package com.idega.jbpm.variables.impl;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.util.expression.ELUtil;

/**
 * the actual persisting and resolving is left to BinaryVariableHandler
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $ Last modified: $Date: 2009/03/30 13:14:27 $ by $Author: civilis $
 */
public class BinaryVariableImpl implements Serializable, BinaryVariable {
	
	private static final long serialVersionUID = -297008331574790442L;
	private String fileName;
	private String identifier;
	private String storageType;
	private Integer hash;
	private Long contentLength;
	private String mimeType;
	private String description;
	private String variableName;
	private Boolean signed;
	private Boolean hidden;
	private long taskInstanceId;
	private Map<String, Object> metadata;
	private boolean persistedToRepository;
	
	@Autowired
	private transient VariablesHandler variablesHandler;
	
	private transient Variable variable;
	private transient URI uri;
	
	@Override
	public long getTaskInstanceId() {
		return taskInstanceId;
	}
	
	@Override
	public void setTaskInstanceId(long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public Integer getHash() {
		
		checkGenHash();
		return hash;
	}
	
	@Override
	public void setHash(int hash) {
		this.hash = hash;
	}
	
	@Override
	public String getIdentifier() {
		return identifier;
	}
	
	@Override
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
		checkGenHash();
	}
	
	@Override
	public String getFileName() {
		return fileName;
	}
	
	@Override
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	@Override
	public String getStorageType() {
		return storageType;
	}
	
	@Override
	public void setStorageType(String storageType) {
		this.storageType = storageType;
		checkGenHash();
	}
	
	@Override
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	@Override
	public String getMimeType() {
		return mimeType;
	}
	
	protected void checkGenHash() {
		if (hash == null && identifier != null && storageType != null) {
			hash = identifier.hashCode() + storageType.hashCode();
		}
	}
	
	// TODO: implement hashCode
	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		
		return obj instanceof BinaryVariable
		        && getStorageType() != null
		        && getStorageType().equals(
		            ((BinaryVariable) obj).getStorageType())
		        && getIdentifier() != null
		        && getIdentifier().equals(
		            ((BinaryVariable) obj).getIdentifier());
	}
	
	@Override
	public Long getContentLength() {
		return contentLength;
	}
	
	@Override
	public void setContentLength(Long contentLength) {
		this.contentLength = contentLength;
	}
	
	public String getVariableName() {
		return variableName;
	}
	
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	
	@Override
	public Boolean getSigned() {
		return signed;
	}
	
	@Override
	public void setSigned(Boolean signed) {
		this.signed = signed;
	}
	
	@Override
	public Boolean getHidden() {
		return hidden;
	}
	
	@Override
	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}
	
	@Override
	public Variable getVariable() {
		if (variable == null && getVariableName() != null)
			variable = Variable.parseDefaultStringRepresentation(getVariableName());
		
		return variable;
	}
	
	@Override
	public void setVariable(Variable variable) {
		this.variable = variable;
		setVariableName(variable.getDefaultStringRepresentation());
	}
	
	@Override
	public boolean isPersisted() {
		return getIdentifier() != null;
	}
	
	@Override
	public void persist() {
		if (!isPersisted()) {
			if (getUri() != null) {
				getVariablesHandler().getBinaryVariablesHandler().persistBinaryVariable(this, getUri());
			} else {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to persist, but no uri provided");
			}
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING,
			    "Called persist on already persisted binary variable");
		}
	}
	
	@Override
	public void update() {
		List<BinaryVariable> binVars = getVariablesHandler().resolveBinaryVariables(getTaskInstanceId(), getVariable());
		if (binVars != null && !binVars.isEmpty()) {
			for (Iterator<BinaryVariable> iterator = binVars.iterator(); iterator.hasNext();) {
				BinaryVariable binVar = iterator.next();
				
				if (binVar.getHash().equals(getHash())) {
					iterator.remove();
					binVars.add(this);
					
					Map<String, Object> variable = new HashMap<String, Object>(1);
					variable.put(getVariable().getDefaultStringRepresentation(), binVars);
					getVariablesHandler().submitVariablesExplicitly(variable, getTaskInstanceId());
					return;
				}
			}
		}
		
		Logger.getLogger(getClass().getName()).log(Level.WARNING, "Called update, but no matching binary variable resolved by variable hash="
		            + getHash() + ". Variable name="
		            + getVariable().getDefaultStringRepresentation()
		            + " and task instanceid=" + getTaskInstanceId());
	}
	
	@Override
	public Object getPersistentResource() {
		return getVariablesHandler().getBinaryVariablesHandler().getBinaryVariablePersistentResource(this);
	}
	
	private VariablesHandler getVariablesHandler() {
		if (variablesHandler == null)
			ELUtil.getInstance().autowire(this);
		
		return variablesHandler;
	}
	
	public URI getUri() {
		return uri;
	}
	
	public void setUri(URI uri) {
		this.uri = uri;
	}
	
	@Override
	public boolean isSignable() {
		if (getFileName().toLowerCase().endsWith(".pdf"))
			return true;
		
		return false;
	}
	
	@Override
	public Map<String, Object> getMetadata() {
		if (metadata == null)
			metadata = new HashMap<String, Object>();
		return metadata;
	}
	
	@Override
	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}
	
	@Override
	public String toString() {
		return getFileName() + ": " + getIdentifier();
	}

	@Override
	public boolean isPersistedToRepository() {
		return persistedToRepository;
	}

	public void setPersistedToRepository(boolean persistedToRepository) {
		this.persistedToRepository = persistedToRepository;
	}

}