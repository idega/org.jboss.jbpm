package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.presentation.ui.handlers.IWDatePickerHandler;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

public class BPMProcessVariable {
	
	public static final List<String>	DATE_TYPES = Collections.unmodifiableList(Arrays.asList("D")),
										DOUBLE_TYPES = Collections.unmodifiableList(Arrays.asList("O")),
										LONG_TYPES = Collections.unmodifiableList(Arrays.asList("L", "H")),
										STRING_TYPES = Collections.unmodifiableList(Arrays.asList("S", "I")),
										NULL_TYPES = Collections.unmodifiableList(Arrays.asList("N")),
										JCR_NODE_TYPES = Collections.unmodifiableList(Arrays.asList("J")),
										BYTE_ARRAY_TYPES = Collections.unmodifiableList(Arrays.asList("B"));
	
	private String name;
	private String value;
	private String type;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public boolean isTypeOf(List<String> types) {
		return (ListUtil.isEmpty(types) || StringUtil.isEmpty(getType())) ? false : types.contains(getType());
	}
	
	public boolean isDateType() {
		return isTypeOf(DATE_TYPES);
	}
	
	public boolean isDoubleType() {
		return isTypeOf(DOUBLE_TYPES);
	}
	
	public boolean isLongType() {
		return isTypeOf(LONG_TYPES);
	}
	
	public boolean isStringType() {
		return isTypeOf(STRING_TYPES);
	}
	
	public boolean isListType() {
		return isTypeOf(BYTE_ARRAY_TYPES);
	}
	
	@Override
	public String toString() {
		return new StringBuilder("Name: " ).append(getName()).append(", type: ").append(getType()).append(", value: ").append(getValue()).toString();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T getRealValue(Locale locale) {
		Serializable realValue = null;
		if (isStringType()) {
			String value = getValue();
			if (locale != null) {
				value = value.toLowerCase(locale);
			}
			realValue = value;
		} else if (isDateType()) {
			try {
				realValue = IWDatePickerHandler.getParsedTimestampByCurrentLocale(getValue());
			} catch (Exception e) {
				Logger.getLogger(BPMProcessVariable.class.getName()).log(Level.WARNING, "Error converting string to timestamp: "+ getValue(), e);
			}
		} else if (isDoubleType()) {
			try {
				realValue = Double.valueOf(getValue());
			} catch (NumberFormatException e) {
				Logger.getLogger(BPMProcessVariable.class.getName()).log(Level.WARNING, "Error converting string to double: "+ getValue(), e);
			}
		} else if (isLongType()) {
			try {
				realValue = Long.valueOf(getValue());
			} catch (Exception e) {
				Logger.getLogger(BPMProcessVariable.class.getName()).log(Level.WARNING, "Error converting string to long: " + getValue(), e);
			}
		} else if (isListType()) {
			List<? extends Serializable> listValue = Arrays.asList(getValue());
			return (T) listValue;
		} else {
			Logger.getLogger(BPMProcessVariable.class.getName()).warning("Unsuported type of variable: " + getType() + ", value: " + getValue());
		}
		
		return (T) realValue;
	}
}