package com.idega.jbpm.bean;

import java.io.Serializable;

public class VariableLongInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -3160663144002624271L;

	private Long value;

	public VariableLongInstance(String name, Long value) {
		super(name, value, VariableInstanceType.LONG);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Serializable> T getValue() {
		return (T) value;
	}

	@Override
	public <T extends Serializable> void setValue(T value) {
		this.value = value instanceof Number ? ((Number) value).longValue() : null;
	}

}