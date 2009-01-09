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
 * @version $Revision: 1.6 $ Last modified: $Date: 2009/01/09 10:31:21 $ by $Author: juozas $
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
	
	@Autowired
	private transient VariablesHandler variablesHandler;
	private transient Variable variable;
	private transient URI uri;
	
	public long getTaskInstanceId() {
		return taskInstanceId;
	}
	
	public void setTaskInstanceId(long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Integer getHash() {
		
		checkGenHash();
		return hash;
	}
	
	public void setHash(int hash) {
		this.hash = hash;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
		checkGenHash();
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
		checkGenHash();
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
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
	
	public Long getContentLength() {
		return contentLength;
	}
	
	public void setContentLength(Long contentLength) {
		this.contentLength = contentLength;
	}
	
	public String getVariableName() {
		return variableName;
	}
	
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	
	public Boolean getSigned() {
		return signed;
	}
	
	public void setSigned(Boolean signed) {
		this.signed = signed;
	}
	
	public Boolean getHidden() {
		return hidden;
	}
	
	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}
	
	public Variable getVariable() {
		
		if (variable == null && getVariableName() != null) {
			
			variable = Variable
			        .parseDefaultStringRepresentation(getVariableName());
		}
		
		return variable;
	}
	
	public void setVariable(Variable variable) {
		this.variable = variable;
		setVariableName(variable.getDefaultStringRepresentation());
	}
	
	public boolean isPersisted() {
		return getIdentifier() != null;
	}
	
	public void persist() {
		
		if (!isPersisted()) {
			
			if (getUri() != null) {
				
				getVariablesHandler().getBinaryVariablesHandler()
				        .persistBinaryVariable(this, getUri());
			} else {
				Logger.getLogger(getClass().getName()).log(Level.WARNING,
				    "Tried to persist, but no uri provided");
			}
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING,
			    "Called persist on already persisted binary variable");
		}
	}
	
	public void update() {
		
		List<BinaryVariable> binVars = getVariablesHandler()
		        .resolveBinaryVariables(getTaskInstanceId(), getVariable());
		
		if (binVars != null && !binVars.isEmpty()) {
			
			for (Iterator<BinaryVariable> iterator = binVars.iterator(); iterator
			        .hasNext();) {
				BinaryVariable binVar = iterator.next();
				
				if (binVar.getHash().equals(getHash())) {
					iterator.remove();
					binVars.add(this);
					
					Map<String, Object> variable = new HashMap<String, Object>(
					        1);
					variable
					        .put(
					            getVariable().getDefaultStringRepresentation(),
					            binVars);
					getVariablesHandler().submitVariablesExplicitly(variable,
					    getTaskInstanceId());
					return;
				}
			}
		}
		
		Logger.getLogger(getClass().getName()).log(
		    Level.WARNING,
		    "Called update, but no matching binary variable resolved by variable hash="
		            + getHash() + ". Variable name="
		            + getVariable().getDefaultStringRepresentation()
		            + " and task instanceid=" + getTaskInstanceId());
	}
	
	private VariablesHandler getVariablesHandler() {
		if (variablesHandler == null) {
			ELUtil.getInstance().autowire(this);
		}
		
		return variablesHandler;
	}
	
	public URI getUri() {
		return uri;
	}
	
	public void setUri(URI uri) {
		this.uri = uri;
	}
	
	public boolean isSignable() {
		if (getFileName().toLowerCase().endsWith(".pdf"))
			return true;
		
		return false;
	}
}