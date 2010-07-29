package com.idega.jbpm.bean;

import java.io.Serializable;
import java.sql.Clob;
import java.util.logging.Logger;

import com.idega.util.CoreConstants;
import com.idega.util.StringHandler;

public class VariableStringInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -8266607249278518260L;

	private String value;
	
	public VariableStringInstance(String name, Object value) {
		super(name, VariableInstanceType.STRING);
		
		String variableValue = null;
		if (value instanceof String) {
			variableValue = (String) value;
		} else if (value instanceof Clob) {
			Clob clob = (Clob) value;
			
			try {
				variableValue = clob.getSubString(1, (int)clob.length());
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (variableValue == null) {
				try {
					variableValue = StringHandler.getContentFromInputStream(clob.getAsciiStream());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (variableValue == null) {
				try {
					variableValue = StringHandler.getContentFromReader(clob.getCharacterStream(1, (int)clob.length()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (variableValue == null) {
				Logger.getLogger(this.getClass().getName()).warning("Unable to fetch value from Clob: " + clob);
			}
		}
		
		this.value = variableValue == null ? CoreConstants.EMPTY : variableValue;
	}
	
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