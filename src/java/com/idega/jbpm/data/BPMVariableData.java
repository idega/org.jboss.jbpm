package com.idega.jbpm.data;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Index;

@Entity
@Table(name=BPMVariableData.TABLE_NAME)
public class BPMVariableData implements Serializable {

	private static final long serialVersionUID = -6007687538989188145L;

	public static final String TABLE_NAME = "BPM_VARIABLE_DATA";
	
	@Id
	@Column(name="id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(name="variable_id")
	@Index(columnNames={"variable_id"}, name="IDX_" + TABLE_NAME + "_1")
	private Long variableId;
	
	@Column(name="stringvalue")
	@Index(columnNames={"stringvalue"}, name="IDX_" + TABLE_NAME + "_2")
	private String stringvalue;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getValue() {
		return stringvalue;
	}

	public void setValue(String value) {
		this.stringvalue = value;
	}

	public Long getVariableId() {
		return variableId;
	}

	public void setVariableId(Long variableId) {
		this.variableId = variableId;
	}

}