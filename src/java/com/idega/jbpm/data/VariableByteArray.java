package com.idega.jbpm.data;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;

//@Entity
//@Indexed
//@Table(name = "JBPM_BYTEARRAY")
public class VariableByteArray implements Serializable {

	private static final long serialVersionUID = -542767802784233348L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID_")
	private Long id;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "processFile")
	private Collection<VariableBytes> bytes;

	@Column(name = "PROCESSFILE_")
	private Long file;

	public VariableByteArray() {
		super();

		file = Long.valueOf(0);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Collection<VariableBytes> getBytes() {
		return bytes;
	}

	public void setBytes(Collection<VariableBytes> bytes) {
		this.bytes = bytes;
	}

	public Long getFile() {
		return file;
	}

	public void setFile(Long file) {
		this.file = file;
	}

	@PrePersist
	public void setDefaultValues() {
		if (file == null) {
			file = Long.valueOf(0);
		}
	}

	@Override
	public String toString() {
		return "ID: " + getId() + ", bytes: " + getBytes();
	}
}