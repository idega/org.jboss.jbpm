package com.idega.jbpm.exe.impl;

import java.io.Serializable;

import com.idega.jbpm.variables.BinaryVariable;

/**
 * the actual persisting and resolving is left to BinaryVariableHandler
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/05/19 13:52:40 $ by $Author: civilis $
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
}