package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.presentation.ui.handlers.IWDatePickerHandler;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

public class BPMProcessVariable implements Serializable {

	private static final long serialVersionUID = -8899276268497865624L;

	public static final List<String>	DATE_TYPES = Collections.unmodifiableList(Arrays.asList("D")),
										DOUBLE_TYPES = Collections.unmodifiableList(Arrays.asList("O")),
										LONG_TYPES = Collections.unmodifiableList(Arrays.asList("L", "H")),
										STRING_TYPES = Collections.unmodifiableList(Arrays.asList("S", "I")),
										NULL_TYPES = Collections.unmodifiableList(Arrays.asList("N")),
										JCR_NODE_TYPES = Collections.unmodifiableList(Arrays.asList("J")),
										BYTE_ARRAY_TYPES = Collections.unmodifiableList(Arrays.asList("B"));

	public static final String EXPRESSION_GREATER_THAN = ">";
	public static final String EXPRESSION_LESS_THAN = "<";
	public static final String EXPRESSION_GREATER_OR_EQUALS = ">=";
	public static final String EXPRESSION_LESS_OR_EQUALS = "<=";
	public static final String EXPRESSION_EQUALS = "=";

	public BPMProcessVariable() {
		super();
	}

	public BPMProcessVariable(String name, String value, String type) {
		this();

		this.name = name;
		this.value = value;
		this.type = type;
	}

	/**
	 *
	 * @param name jBPM variable name.
	 * For example: string_someString, list_someList.
	 * @param value jBPM variable value.
	 * @param type jBPM variable type, for example:
	 * {@link BPMProcessVariable#DATE_TYPES}.
	 * @param expression values could be "=",">","<","<=",">=".
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public BPMProcessVariable(String name, String value, String type,
			String expression) {
		this(name, value, type);
		this.expression = expression;
	}

	private String name, value, type, expression = null;
	private boolean flexible, multiple;
	private int order = 0;

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

	public String getExpression() {
		return this.expression;
	}

	/**
	 * @param expression values could be: "=",">","<","<=",">=".
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public void setExpression(String expression) {
		this.expression = expression;
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

	public boolean isFlexible() {
		return flexible;
	}

	public void setFlexible(boolean flexible) {
		this.flexible = flexible;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public String toString() {
		return new StringBuilder("Name: " ).append(getName()).append(", type: ").append(getType()).append(", value: ").append(getValue()).toString();
	}

	@SuppressWarnings("unchecked")
	public <T extends Serializable> T getRealValue() {
		return (T) getRealValue(null);	//	Casting is needed to avoid compilation error in Maven 2
	}

	@SuppressWarnings("unchecked")
	public <T extends Serializable> T getRealValue(Locale locale) {
		String value = getValue();
		if (StringUtil.isEmpty(value)) {
			return null;
		}
		boolean multiValues = value.indexOf(CoreConstants.SEMICOLON) != -1;

		Serializable realValue = null;
		if (isStringType()) {
			if (locale != null) {
				value = value.toLowerCase(locale);
			}
			if (isMultiple() && value.indexOf(CoreConstants.SEMICOLON) != -1) {
				String[] values = value.split(CoreConstants.SEMICOLON);
				if (!ArrayUtil.isEmpty(values))
					realValue = (T) Arrays.asList(values);
			}
			if (realValue == null)
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
				if (multiValues) {
					ArrayList<Long> longValues = new ArrayList<Long>();
					String[] values = value.split(CoreConstants.SEMICOLON);
					for (String longValue: values) {
						longValue = longValue.trim();
						if (StringHandler.isNumeric(longValue)) {
							longValues.add(Long.valueOf(longValue));
						}
					}
					realValue = longValues;
				} else {
					realValue = Long.valueOf(getValue());
				}
			} catch (Exception e) {
				Logger.getLogger(BPMProcessVariable.class.getName()).log(Level.WARNING, "Error converting string to long: " + getValue(), e);
			}
		} else if (isListType()) {
			List<? extends Serializable> listValue = getValue().indexOf(CoreConstants.SEMICOLON) == -1 ?
					Arrays.asList(getValue()) : Arrays.asList(getValue().split(CoreConstants.SEMICOLON));
			return (T) listValue;
		} else {
			Logger.getLogger(BPMProcessVariable.class.getName()).warning("Unsuported type of variable: " + getType() + ", value: " + getValue());
		}

		return (T) realValue;
	}
}