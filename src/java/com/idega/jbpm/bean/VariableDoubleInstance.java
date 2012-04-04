package com.idega.jbpm.bean;

import java.io.Serializable;

public class VariableDoubleInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -4328482318558210547L;

	private Double value;

	public VariableDoubleInstance(String name, Double value) {
		super(name, value, VariableInstanceType.DOUBLE);
	}

	@Override
	public Double getValue() {
		return value;
	}

	@Override
	public void setValue(Serializable value) {
		this.value = value instanceof Number ? ((Number) value).doubleValue() : null;
	}

}