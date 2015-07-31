package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.Random;
import java.util.logging.Logger;

import com.idega.jbpm.data.Variable;
import com.idega.util.StringUtil;

public abstract class VariableInstanceInfo implements Serializable {

	private static final long serialVersionUID = 7094674925493141143L;
	private static final Logger LOGGER = Logger.getLogger(VariableInstanceInfo.class.getName());

	private int hash;

	private String name, caseId;
	private VariableInstanceType type;

	private Long id, processInstanceId, taskInstanceId;

	public VariableInstanceInfo() {
		super();

		hash = new Random().nextInt(Integer.MAX_VALUE);
	}

	public <T extends Serializable> VariableInstanceInfo(Variable variable) {
		this(variable.getName(), variable.getValue());

		type = getType(variable.getClassType());

		this.id = variable.getId();
		this.processInstanceId = variable.getProcessInstance();
		this.taskInstanceId = variable.getTaskInstance();
	}

	public <T extends Serializable> VariableInstanceInfo(T value) {
		this(null, value);
	}

	public <T extends Serializable> VariableInstanceInfo(String name, T value) {
		this(name, value, null);
	}

	public VariableInstanceInfo(String name, VariableInstanceType type) {
		this(name, null, type);
	}

	public <T extends Serializable> VariableInstanceInfo(String name, T value, VariableInstanceType type) {
		this();

		this.name = name;
		setValue(value);
		this.type = type;
	}

	public VariableInstanceInfo(String name, String type) {
		this();
		this.name = name;
		this.type = getType(type);
	}

	private VariableInstanceType getType(Character type) {
		return getType(type == null ? null : String.valueOf(type));
	}

	private VariableInstanceType getType(String type) {
		if (StringUtil.isEmpty(type)) {
			LOGGER.warning("Type is not defined for variable: '" + name + "'!");
		} else {
			for (VariableInstanceType varType: VariableInstanceType.ALL_TYPES) {
				if (varType.getTypeKeys().contains(type))
					return varType;
			}
		}

		LOGGER.warning("Unknown type: " + type + " for variable " + name + ". Using NULL type");
		return VariableInstanceType.NULL;
	}

	public abstract <T extends Serializable> T getValue();

	public abstract <T extends Serializable> void setValue(T value);

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

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof VariableInstanceInfo) {
			VariableInstanceInfo var = (VariableInstanceInfo) object;
			Long varId = var.getId();
			if (getId() == null || varId == null)
				return false;

			String varName = var.getName();
			if (getName() == null || varName == null)
				return false;

			Serializable varValue = var.getValue();
			if (getValue() == null || varValue == null)
				return false;

			return getId().longValue() == varId.longValue() && getName().equals(varName) && getValue().toString().equals(varValue.toString());
		}
		return false;
	}

	public String getCaseId() {
		return caseId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	@Override
	public String toString() {
		return "Variable " + getName() + ", type " + getType() + ", value: " + getValue() + ", ID: " + getId() + ", task instance ID: " +
				getTaskInstanceId() + ", process instance ID: " + getProcessInstanceId();
	}

	public static VariableInstanceInfo getDefaultVariable(String name) {
		VariableInstanceInfo info = new VariableInstanceInfo() {

			private static final long serialVersionUID = 4538526463690356958L;

			private Serializable value = null;

			@Override
			public <T extends Serializable> void setValue(T value) {
				this.value = value;
			}
			@SuppressWarnings("unchecked")
			@Override
			public <T extends Serializable> T getValue() {
				return (T) value;
			}
		};
		info.setName(name);
		return info;
	}

}