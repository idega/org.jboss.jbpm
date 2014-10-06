package com.idega.jbpm.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

@Indexed
@Entity
@Table(name = "JBPM_BYTEBLOCK")
@NamedQueries({
	@NamedQuery(
			name = VariableBytes.QUERY_FIND_BY_ID, 
			query = "FROM VariableBytes vb "
					+ "WHERE vb.processFile = :processFile "
					+ "AND vb.bytes IS NOT NULL "
					+ "ORDER BY vb.index")
})
public class VariableBytes implements Serializable {

	private static final long serialVersionUID = -5044002250556419438L;

	public static final String QUERY_FIND_BY_ID = "variableBytes.findById";

	public static final String processFileProp = "processFile";
	@Id
	@Column(name = "PROCESSFILE_")
	private Long processFile;

	public static final String bytesProp = "bytes";
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

	public VariableBytes(Long processFile, byte[] bytes, Integer index) {
		this.processFile = processFile;
		this.index = index;
		this.bytes = bytes;
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