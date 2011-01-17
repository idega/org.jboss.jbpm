package com.idega.jbpm.bean;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;

import com.idega.util.IOUtil;

public class VariableByteArrayInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -4612911188630523581L;

	private Serializable value;
	
	public VariableByteArrayInstance(String name, Object value) {
		super(name, VariableInstanceType.BYTE_ARRAY);
		
		Byte[] variableValue = null;
		if (value instanceof Byte[]) {
			variableValue = (Byte[]) value;
		} else if (value instanceof Blob) {
			Blob blob = (Blob) value;
			
			byte[] bytes = null;
			try {
				bytes = IOUtil.getBytesFromInputStream(blob.getBinaryStream());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (bytes != null) {
				variableValue = new Byte[bytes.length];
				for (int i = 0; i < bytes.length; i++) {
					variableValue[i] = bytes[i];
				}
			}
		} else if (value instanceof Collection<?>) {
			this.value = (Serializable) value;
		}
		
		this.value = variableValue == null ? this.value : variableValue;
	}
	
	public VariableByteArrayInstance(String name, Byte[] value) {
		super(name, value, VariableInstanceType.BYTE_ARRAY);
	}
	
	@Override
	public Serializable getValue() {
		return value;
	}

	@Override
	public void setValue(Serializable value) {
		this.value = value instanceof Byte[] ? (Byte[]) value : null;
	}

}