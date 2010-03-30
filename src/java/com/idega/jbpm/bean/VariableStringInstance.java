package com.idega.jbpm.bean;

import java.io.Serializable;

public class VariableStringInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -8266607249278518260L;

	private String value;
	
	public VariableStringInstance(String name, String value) {
		super(name, value, VariableInstanceType.STRING);
	}
	
	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(Serializable value) {
		this.value = value instanceof String ? (String) value : null;
	}

}