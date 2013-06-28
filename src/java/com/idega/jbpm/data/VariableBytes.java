package com.idega.jbpm.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

@Indexed
@Entity
@Table(name = "JBPM_BYTEBLOCK")
public class VariableBytes implements Serializable {

	private static final long serialVersionUID = -5044002250556419438L;

	@Id
	@Column(name = "PROCESSFILE_")
	private Long processFile;

	@Lob
	@Column(name = "BYTES_")
	private byte[] bytes;

	@Column(name = "INDEX_", nullable = false)
	private Integer index;

	public VariableBytes() {
		super();

		this.processFile = Long.valueOf(0);
		this.index = 0;
	}

	public Long getProcessFile() {
		return processFile;
	}

	public void setProcessFile(Long processFile) {
		this.processFile = processFile;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	@PrePersist
	public void setDefaultValues() {
		if (processFile == null) {
			processFile = Long.valueOf(0);
		}
	}

	@Override
	public String toString() {
		return "Process file: " + getProcessFile() + ", bytes: " + getBytes();
	}
}