package com.idega.jbpm.bean;

import java.io.Serializable;

public class VariableLongInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -3160663144002624271L;

	private Long value;
	
	public VariableLongInstance(String name, Long value) {
		super(name, value, VariableInstanceType.LONG);
	}
	
	@Override
	public Long getValue() {
		return value;
	}

	@Override
	public void setValue(Serializable value) {
		this.value = value instanceof Long ? (Long) value : null;
	}

}