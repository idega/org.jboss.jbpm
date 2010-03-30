package com.idega.jbpm.bean;

import java.io.Serializable;
import java.sql.Timestamp;

public class VariableDateInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -2590225930963717609L;

	private Timestamp value;
	
	public VariableDateInstance(String name, Timestamp value) {
		super(name, value, VariableInstanceType.DATE);
	}
	
	@Override
	public Timestamp getValue() {
		return value;
	}

	@Override
	public void setValue(Serializable value) {
		this.value = value instanceof Timestamp ? (Timestamp) value : null;
	}

}