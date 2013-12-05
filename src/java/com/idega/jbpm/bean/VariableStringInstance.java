package com.idega.jbpm.bean;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
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

			Connection connection = null;
			try {
				long maxClobSize = Long.valueOf(
						IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty("bpm.max_clob_size", String.valueOf(1024 * 1024 * 250))
				);

				connection = SimpleQuerier.getConnection();
				long clobLength = clob.length();
				if (clobLength > maxClobSize) {
					variableValue = clob.getSubString(1, Long.valueOf(maxClobSize).intValue());
					Logger.getLogger(getClass().getName()).warning("Shortened CLOB's value to " + maxClobSize + " bytes. It's original size: " +
							clobLength + " bytes. Variable ID: " + id);
				} else {
					variableValue = clob.getSubString(1, Long.valueOf(clobLength).intValue());
				}
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error while getting value from CLOB. Variable ID: " + id, e);
			} finally {
				if (connection != null) {
					SimpleQuerier.freeConnection(connection);
				}
			}

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

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(Serializable value) {
		String oldValue = getValue();

		setValue(getId(), value);
		String newValue = getValue();

		if (StringUtil.isEmpty(newValue) && !StringUtil.isEmpty(oldValue))
			this.value = oldValue;
	}

}