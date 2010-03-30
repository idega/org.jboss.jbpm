package com.idega.jbpm.bean;

import java.io.Serializable;

public class VariableDefaultInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = 844279522688868122L;

	public VariableDefaultInstance(String name, String type) {
		super(name, type);
	}
	
	@Override
	public Serializable getValue() {
		return null;
	}

	@Override
	public void setValue(Serializable value) {
	}

}