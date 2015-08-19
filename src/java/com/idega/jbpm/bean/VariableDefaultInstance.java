package com.idega.jbpm.bean;

import java.io.Serializable;

public class VariableDefaultInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = 844279522688868122L;

	public VariableDefaultInstance(String name, String type) {
		super(name, null, getType(type));
	}

	@Override
	public <T extends Serializable> T getValue() {
		return null;
	}

	@Override
	public <T extends Serializable> void setValue(T value) {
	}

}