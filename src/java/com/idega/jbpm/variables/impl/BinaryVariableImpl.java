package com.idega.jbpm.variables.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.util.expression.ELUtil;
/**
 * the actual persisting and resolving is left to BinaryVariableHandler
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/10/03 12:44:55 $ by $Author: anton $
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
	
	@Autowired
	private transient VariablesHandler variablesHandler;

	private Variable variable;
	private long taskInstanceId;
	
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
		return hash;
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
		
		if(hash == null && identifier != null && storageType != null) {
			hash = identifier.hashCode() + storageType.hashCode();
		}
	}
	
//	TODO: implement hashCode
	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj))
			return true;
		
		return obj instanceof BinaryVariable && ((BinaryVariable)obj).getStorageType().equals(getStorageType()) && ((BinaryVariable)obj).getIdentifier().equals(getIdentifier());
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
		return variable;
	}

	public void setVariable(Variable variable) {
		this.variable = variable;
	}
	
	public void store() {
		Map<String, Object> variable = new HashMap<String, Object>(1);
		variable.put(getVariable().getDefaultStringRepresentation(), this);
		getVariablesHandler().submitVariablesExplicitly(variable, getTaskInstanceId());
	}

	private VariablesHandler getVariablesHandler() {
		if(variablesHandler == null) {
			ELUtil.getInstance().autowire(this);
		} 
		
		return variablesHandler;
	}
}