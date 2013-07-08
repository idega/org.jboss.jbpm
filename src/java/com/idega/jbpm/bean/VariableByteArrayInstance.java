package com.idega.jbpm.bean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.data.SimpleQuerier;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

public class VariableByteArrayInstance extends VariableInstanceInfo {

	private static final long serialVersionUID = -4612911188630523581L;

	private Serializable value;

	private static final Logger LOGGER = Logger.getLogger(VariableByteArrayInstance.class.getName());

	public VariableByteArrayInstance(String name, Object value) {
		super(name, VariableInstanceType.BYTE_ARRAY);

		if (value instanceof Collection<?>) {
			this.value = (Serializable) value;
		} else if (value instanceof Number) {
			this.value = (Number) value;
		} else if (value instanceof Serializable) {
			this.value = (Serializable) value;
		}
	}

	private static Serializable getConvertedValue(byte[] bytes, Long variableId, Long procInstId) {
		InputStream input = null;
		ObjectInputStream objectInput = null;
		try {
			Object realValue = null;
			input = new ByteArrayInputStream(bytes);
			objectInput = new ObjectInputStream(input);
			realValue = objectInput.readObject();
			if (realValue instanceof Serializable)
				return (Serializable) realValue;
		} catch (Exception e) {
			String message = "Couldn't deserialize stream (made from bytes: " + (bytes == null ? "not provided" : ("length: " +
					bytes.length +	", representation: '" + new String(bytes))) + "'). Returning empty String. Variable ID: " + variableId +
					", process instance ID: " + procInstId;
			LOGGER.log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		} finally {
			IOUtil.close(objectInput);
			IOUtil.close(input);
		}
		return CoreConstants.EMPTY;
	}

	public VariableByteArrayInstance(String name, Byte[] value) {
		super(name, value, VariableInstanceType.BYTE_ARRAY);
	}

	@Override
	public <T extends Serializable> T getValue() {
		return getValue(value, getId(), getProcessInstanceId());
	}

	public static <T extends Serializable> T getValue(Collection<byte[]> allBytes) {
		if (ListUtil.isEmpty(allBytes))
			return null;

		byte[] bytes = new byte[allBytes.size() * 1024];
		int pos = 0;
		for (Iterator<byte[]> it = allBytes.iterator(); it.hasNext();) {
			byte[] tmp = it.next();
			System.arraycopy(tmp, 0, bytes, pos, tmp.length);
			pos += tmp.length;
		}

		@SuppressWarnings("unchecked")
		T value = (T) getConvertedValue(bytes, null, null);
		return value;
	}

	public static <T extends Serializable> T getValue(String name, Long piId) {
		if (piId == null || StringUtil.isEmpty(name))
			return null;

		String query = "select b.BYTES_ from JBPM_BYTEBLOCK b, JBPM_VARIABLEINSTANCE v where v.PROCESSINSTANCE_ = " + piId +
				" and name_ = '" + name +  "' and b.PROCESSFILE_ = v.BYTEARRAYVALUE_";
		return getValue(query, null, null);
	}
	public static <T extends Serializable> T getValue(Long tiId, String name) {
		if (tiId == null || StringUtil.isEmpty(name))
			return null;

		String query = "select b.BYTES_ from JBPM_BYTEBLOCK b, JBPM_VARIABLEINSTANCE v where v.TASKINSTANCE_ = " + tiId +
				" and name_ = '" + name +  "' and b.PROCESSFILE_ = v.BYTEARRAYVALUE_";
		return getValue(query, null, null);
	}

	private static <T extends Serializable> T getValue(String query, Long variableId, Long procInstId) {
		try {
			List<Serializable[]> values = SimpleQuerier.executeQuery(query, 1);
			if (ListUtil.isEmpty(values)) {
				return null;
			}
			Object bytes = null;

			if (values.size() > 1) {
				bytes = new byte[values.size() * 1024];
				int pos = 0;
				for (Iterator<Serializable[]> it = values.iterator(); it.hasNext();) {
					byte[] tmp = (byte[]) it.next()[0];
					System.arraycopy(tmp, 0, bytes, pos, tmp.length);
					pos += tmp.length;
				}
			} else {
				bytes = values.iterator().next()[0];
			}

			Serializable value = bytes instanceof byte[] ? getConvertedValue((byte[]) bytes, variableId, procInstId) : null;

			if (value instanceof Serializable) {
				@SuppressWarnings("unchecked")
				T realValue = (T) value;
				return realValue;
			}

			return null;
		} catch (Exception e) {
			Logger.getLogger(VariableByteArrayInstance.class.getName()).log(Level.WARNING, "Error executing query: " + query, e);
		}

		return null;
	}

	public static <T extends Serializable> T getValue(Serializable value, Long variableId, Long procInstId) {
		if (value instanceof Number) {
			String query = "select b.BYTES_ from JBPM_BYTEBLOCK b where b.PROCESSFILE_ = " + value;
			value = getValue(query, variableId, procInstId);
		} else if (value instanceof byte[]) {
			value = getConvertedValue((byte[]) value, variableId, procInstId);
		} else if (value instanceof Blob) {
			Blob blob = (Blob) value;

			byte[] bytes = null;
			try {
				bytes = IOUtil.getBytesFromInputStream(blob.getBinaryStream());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (bytes != null) {
				value = getConvertedValue(bytes, variableId, procInstId);
			}
		}

		if (value instanceof Serializable) {
			@SuppressWarnings("unchecked")
			T realValue = (T) value;
			return realValue;
		}

		return null;
	}

	@Override
	public <T extends Serializable> void setValue(T value) {
		this.value = value instanceof Serializable ? value : null;
	}
}