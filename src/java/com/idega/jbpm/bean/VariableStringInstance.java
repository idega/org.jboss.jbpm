package com.idega.jbpm.bean;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import com.idega.data.SimpleQuerier;
import com.idega.util.CoreConstants;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

public class VariableStringInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -8266607249278518260L;

	private String value;

	public VariableStringInstance(String name, Object value) {
		this(null, name, value);
	}

	public VariableStringInstance(Long id, String name, Object value) {
		super(name, VariableInstanceType.STRING);

		setValue(id, value);
	}

	private void setValue(Long id, Object value) {
		String variableValue = null;
		if (value instanceof String) {
			variableValue = (String) value;
		} else if (value instanceof Clob) {
			Clob clob = (Clob) value;

			try {
				variableValue = clob.getSubString(1, (int) clob.length());
			} catch (Exception e) {}

			if (variableValue == null && id != null) {
				Connection conn = null;
				Statement statement = null;
				try {
					conn = SimpleQuerier.getConnection();
					statement = conn.createStatement();
					ResultSet results = statement.executeQuery("select var.STRINGVALUE_ from JBPM_VARIABLEINSTANCE var where var.ID_ = " + id);

					if (results.next()) {
						InputStream stream = results.getAsciiStream(1);
						variableValue = StringHandler.getContentFromInputStream(stream);
					}

					results.close();
					statement.close();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (conn != null)
						SimpleQuerier.freeConnection(conn);
				}
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
					variableValue = StringHandler.getContentFromReader(clob.getCharacterStream(1, (int) clob.length()));
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

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Serializable> T getValue() {
		return (T) value;
	}

	@Override
	public <T extends Serializable> void setValue(T value) {
		String oldValue = getValue();

		setValue(getId(), value);
		String newValue = getValue();

		if (StringUtil.isEmpty(newValue) && !StringUtil.isEmpty(oldValue))
			this.value = oldValue;
	}

}