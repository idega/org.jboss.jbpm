package com.idega.jbpm.data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;

import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

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
@Cacheable
public class Variable implements Serializable {

	private static final long serialVersionUID = -8699501352332532101L;

	public static final String	QUERY_GET_BY_NAMES_AND_PROC_INST = "variable.getByNamesAndProcInst",
								QUERY_GET_BY_PROC_INST = "variable.getByProcInst",
								QUERY_GET_BY_NAMES_AND_PROC_INST_IDS = "variable.getByNamesAndProcInstIds",
								QUERY_GET_BY_NAMES_AND_IDS = "variable.getByNamesAndIds",
								PARAM_NAMES = "names",
								PARAM_IDS = "ids",
								PARAM_PROC_INST_ID = "procInstId",
								PARAM_PROC_INST_IDS = "procInstIds",
								PARAM_TASK_INST_IDS = "taskInstIds";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id_")
	private Long id;

	@Column(name = "name_")
	@Field(store=Store.YES)
	private String name;

	@Column(name = "PROCESSINSTANCE_")
	@Field(store=Store.YES)
	private Long processInstance;

	@Column(name = "TASKINSTANCE_")
	@Field(store=Store.YES)
	private Long taskInstance;

	@Column(name = "TOKEN_")
	@Field(store=Store.YES)
	private Long token;

	/**
	 * Value columns start
	 */
	@Column(name = "stringvalue_", length = Integer.MAX_VALUE)
	@Field(store=Store.YES)
	private String stringValue;

	@Column(name = "longvalue_")
	@Field(store=Store.YES)
	private Long longValue;

	@Column(name = "DOUBLEVALUE_")
	@Field(store=Store.YES)
	private Double doubleValue;

	@Column(name = "DATEVALUE_")
	@Field(store=Store.YES)
	@DateBridge(resolution=Resolution.MILLISECOND)
	private Timestamp dateValue;

	@Column(name = "BYTEARRAYVALUE_")
	@Field(store=Store.YES)
	private Long byteArrayValue;
	/**
	 * Value columns end
	 */

	@Column(name = "CLASS_")
	@Field(store=Store.YES)
	private Character classType;

	@Column(name = "VERSION_")
	@Field(store=Store.YES)
	private Integer version;

	@Transient
	private Collection<byte[]> bytesValue;
	@Transient
	private Serializable realObject;

	public Serializable getRealObject() {
		getValue();
		return realObject;
	}

	public Collection<byte[]> getBytesValue() {
		getValue();
		return bytesValue;
	}

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
		Character classType = getClassType();

		VariableInstanceType type = null;
		if (value instanceof String) {
			stringValue = (String) value;
			type = VariableInstanceType.STRING;

		} else if (value instanceof Number && (classType == null ? true : Character.valueOf('L').equals(classType))) {
			longValue = ((Number) value).longValue();
			type = VariableInstanceType.LONG;

		} else if (value instanceof Double && (classType == null ? true : Character.valueOf('O').equals(getClassType()))) {
			doubleValue = ((Number) value).doubleValue();
			type = VariableInstanceType.DOUBLE;

		} else if (value instanceof Timestamp) {
			dateValue = (Timestamp) value;
			type = VariableInstanceType.DATE;

		} else if (Character.valueOf('B').equals(getClassType())) {
			Logger.getLogger(Variable.class.getName()).info("Set bytes value: " + value +
					(value == null ? ", unknown type" : ", type: " + value.getClass()));
			type = VariableInstanceType.BYTE_ARRAY;

		} else {
			Logger.getLogger(Variable.class.getName()).warning("Do not know how to handle value: " + value);
		}

		if (classType == null && type != null) {
			setClassType(Character.valueOf(type.getTypeKeys().get(0).charAt(0)));
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
		} else if (classType == 'B' && byteArrayValue != null) {
			if (bytesValue != null)
				return (T) realObject;

			BPMDAO bpmDAO = ELUtil.getInstance().getBean("bpmBindsDAO");
			List<VariableBytes> varBytes = bpmDAO
					.findAllVariableBytesById(byteArrayValue);
			if (ListUtil.isEmpty(varBytes)) {
				bytesValue = new ArrayList<>(0);
				return null;
			}

			Collection<byte[]> allBytes = new ArrayList<>();
			for (VariableBytes vb: varBytes) {
				allBytes.add(vb.getBytes());
			}

			realObject = VariableByteArrayInstance.getValue(allBytes);
			return (T) realObject;
		}

		return null;
	}

	public <T extends Serializable> T getProcessInstance() {
		@SuppressWarnings("unchecked")
		T result = (T) processInstance;
		return result;
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

	public Long getByteArrayValue() {
		return byteArrayValue;
	}

	public void setByteArrayValue(Long byteArrayValue) {
		this.byteArrayValue = byteArrayValue;
	}

	public Character getClassType() {
		return classType;
	}

	public void setClassType(Character classType) {
		this.classType = classType;
	}

	public Long getToken() {
		return token;
	}

	public void setToken(Long token) {
		this.token = token;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "ID: " + getId() + ", name: " + getName() + ", proc. ins.: " + getProcessInstance() + ", value: " + getValue();
	}
}