package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.logging.Logger;

import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;

public abstract class VariableInstanceInfo implements Serializable {

	private static final long serialVersionUID = 7094674925493141143L;
	private static final Logger LOGGER = Logger.getLogger(VariableInstanceInfo.class.getName());
	
	private String name;
	private VariableInstanceType type;
	
	private Long processInstanceId;
	
	public VariableInstanceInfo() {
		super();
	}
	
	public VariableInstanceInfo(Serializable value) {
		this(null, value);
	}
	
	public VariableInstanceInfo(String name, Serializable value) {
		this(name, value, null);
	}
	
	public VariableInstanceInfo(String name, Serializable value, VariableInstanceType type) {
		this();
		
		this.name = name;
		setValue(value);
		this.type = type;
	}
	
	public VariableInstanceInfo(String name, String type) {
		this();
		this.name = name;
		
		VariableInstanceType varType = null;
		if (StringUtil.isEmpty(type)) {
			LOGGER.warning("Type is not defined!");
		} else {
			if (VariableInstanceType.STRING.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.STRING;
			} else if (VariableInstanceType.LONG.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.LONG;
			} else if (VariableInstanceType.DOUBLE.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.DOUBLE;
			} else if (VariableInstanceType.DATE.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.DATE;
			} else if (VariableInstanceType.BYTE_ARRAY.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.BYTE_ARRAY;
			} else {
				LOGGER.warning("Unknown type: " + type);
			}
		}
		this.type = varType == null ? VariableInstanceType.NULL : varType;
	}
	
	public abstract Serializable getValue();

	public abstract void setValue(Serializable value);

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public VariableInstanceType getType() {
		return type;
	}

	public void setType(VariableInstanceType type) {
		this.type = type;
	}
	
	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	@Override
	public String toString() {
		String prInstId = processInstanceId == null ? CoreConstants.MINUS : String.valueOf(processInstanceId);
		return "Variable ".concat(getName()).concat(", type ").concat(getType().toString()).concat(", value: ").concat((String) getValue())
			.concat(", process instance ID: ").concat(prInstId);
	}
}