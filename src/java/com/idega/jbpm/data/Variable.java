package com.idega.jbpm.data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.util.StringUtil;

@Indexed
@Entity
@Table(name = "jbpm_variableinstance")
@NamedQueries({
	@NamedQuery(name=Variable.QUERY_GET_BY_NAMES_AND_PROC_INST, query="from Variable v where v.name in (:" + Variable.PARAM_NAMES +
			") and v.processInstance = :" + Variable.PARAM_PROC_INST_ID + " and v.taskInstance is not null order by v.id desc"),
	@NamedQuery(name=Variable.QUERY_GET_BY_PROC_INST, query="from Variable v where v.processInstance = :" + Variable.PARAM_PROC_INST_ID +
			" order by v.id desc"),
	@NamedQuery(name=Variable.QUERY_GET_BY_NAMES_AND_PROC_INST_IDS, query="from Variable v where v.name in (:" + Variable.PARAM_NAMES +
			") and v.processInstance in (:" + Variable.PARAM_PROC_INST_IDS + ") and v.taskInstance is not null order by v.id desc"),
	@NamedQuery(name=Variable.QUERY_GET_BY_NAMES_AND_IDS, query="from Variable v where v.id in (:" + Variable.PARAM_IDS +
			") and v.name in (:" + Variable.PARAM_NAMES + ") and v.taskInstance is not null order by v.id desc")
})
public class Variable implements Serializable {

	private static final long serialVersionUID = -8699501352332532101L;

	public static final String	QUERY_GET_BY_NAMES_AND_PROC_INST = "variable.getByNamesAndProcInst",
								QUERY_GET_BY_PROC_INST = "variable.getByProcInst",
								QUERY_GET_BY_NAMES_AND_PROC_INST_IDS = "variable.getByNamesAndProcInstIds",
								QUERY_GET_BY_NAMES_AND_IDS = "variable.getByNamesAndIds",
								PARAM_NAMES = "names",
								PARAM_IDS = "ids",
								PARAM_PROC_INST_ID = "procInstId",
								PARAM_PROC_INST_IDS = "procInstIds";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id_")
	private Long id;

	@Column(name = "name_")
	private String name;

	@Column(name = "PROCESSINSTANCE_")
	@Field(store=Store.YES)
	private Long processInstance;

	@Column(name = "TASKINSTANCE_")
	private Long taskInstance;

	/**
	 * Value columns start
	 */
	@Column(name = "stringvalue_", length = Integer.MAX_VALUE)
	private String stringValue;

	@Column(name = "longvalue_")
	private Long longValue;

	@Column(name = "DOUBLEVALUE_")
	private Double doubleValue;

	@Column(name = "DATEVALUE_")
	private Timestamp dateValue;

	@Column(name = "BYTEARRAYVALUE_")
	private Long byteValue;
	/**
	 * Value columns end
	 */

	@Column(name = "CLASS_")
	private Character classType;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public <T extends Serializable> void setValue(T value) {
		if (value instanceof String) {
			stringValue = (String) value;
		} else if (value instanceof Number && Character.valueOf('L').equals(getClassType())) {
			longValue = ((Number) value).longValue();
		} else if (value instanceof Double && Character.valueOf('O').equals(getClassType())) {
			doubleValue = ((Number) value).doubleValue();
		} else if (value instanceof Timestamp) {
			dateValue = (Timestamp) value;
		} else if (value instanceof Number && Character.valueOf('B').equals(getClassType())) {
			byteValue = ((Number) value).longValue();
		} else {
			Logger.getLogger(Variable.class.getName()).warning("Do not know how to handle value: " + value);
		}
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	public Long getLongValue() {
		return longValue;
	}

	public void setLongValue(Long longValue) {
		this.longValue = longValue;
	}

	@SuppressWarnings("unchecked")
	public <T extends Serializable> T getValue() {
		if (!StringUtil.isEmpty(stringValue)) {
			return (T) stringValue;
		} else if (longValue instanceof Number) {
			return (T) Long.valueOf(((Number) longValue).longValue());
		} else if (doubleValue instanceof Number) {
			return (T) Double.valueOf(((Number) doubleValue).doubleValue());
		} else if (dateValue instanceof Timestamp) {
			return (T) dateValue;
		} else if (byteValue != null) {
			return VariableByteArrayInstance.getValue(byteValue, getId(), getProcessInstance());
		}

		return null;
	}

	public Long getProcessInstance() {
		return processInstance;
	}

	public void setProcessInstance(Long processInstance) {
		this.processInstance = processInstance;
	}

	public Long getTaskInstance() {
		return taskInstance;
	}

	public void setTaskInstance(Long taskInstance) {
		this.taskInstance = taskInstance;
	}

	public Double getDoubleValue() {
		return doubleValue;
	}

	public void setDoubleValue(Double doubleValue) {
		this.doubleValue = doubleValue;
	}

	public Timestamp getDateValue() {
		return dateValue;
	}

	public void setDateValue(Timestamp dateValue) {
		this.dateValue = dateValue;
	}

	public Long getByteValue() {
		return byteValue;
	}

	public void setByteValue(Long byteValue) {
		this.byteValue = byteValue;
	}

	public Character getClassType() {
		return classType;
	}

	public void setClassType(Character classType) {
		this.classType = classType;
	}

	@Override
	public String toString() {
		return "ID: " + getId() + ", name: " + getName() + ", proc. ins.: " + getProcessInstance() + ", value: " + getValue();
	}
}