package com.idega.jbpm.bean;

import java.io.Serializable;

public class VariableByteArrayInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -4612911188630523581L;

	private Byte[] value;
	
	public VariableByteArrayInstance(String name, Byte[] value) {
		super(name, value, VariableInstanceType.BYTE_ARRAY);
	}
	
	@Override
	public Byte[] getValue() {
		return value;
	}

	@Override
	public void setValue(Serializable value) {
		this.value = value instanceof Byte[] ? (Byte[]) value : null;
	}

}