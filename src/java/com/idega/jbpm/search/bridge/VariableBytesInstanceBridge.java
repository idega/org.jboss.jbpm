package com.idega.jbpm.search.bridge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayStringBridge;

import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.jbpm.data.VariableBytes;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;

public class VariableBytesInstanceBridge implements FieldBridge, TwoWayStringBridge {

	private static final Logger LOGGER = Logger.getLogger(VariableBytesInstanceBridge.class.getName());

	@Override
	public String objectToString(Object object) {
		if (object instanceof String) {
			return (String) object;
		}
		return null;
	}

	@Override
	public Object stringToObject(String stringValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if (value == null)
			return;

		if (value instanceof Collection<?> && ListUtil.isEmpty((Collection<?>) value))
			return;

		if (value instanceof byte[]) {
			try {
				byte[] bytes = (byte[]) value;
				setIndexedValue(Arrays.asList(bytes), name, document, luceneOptions);
			} catch (Exception e) {}
		} else if (value instanceof Collection<?>) {
			try {
				@SuppressWarnings("unchecked")
				Collection<VariableBytes> bytes = (Collection<VariableBytes>) value;
				Collection<byte[]> allBytes = new ArrayList<byte[]>();
				for (VariableBytes vb: bytes) {
					allBytes.add(vb.getBytes());
				}

				setIndexedValue(allBytes, name, document, luceneOptions);
			} catch (Exception e) {

			}
		} else {
			LOGGER.warning("Do not know how to set value: " + value + " for field " + name);
		}
	}

	private void setIndexedValue(Collection<byte[]> bytes, String name, Document document, LuceneOptions luceneOptions) {
		@SuppressWarnings("unchecked")
		Collection<Serializable> objects = (Collection<Serializable>) VariableByteArrayInstance.getValue(bytes);
		if (!ListUtil.isEmpty(objects)) {
			String values = CoreConstants.EMPTY;
			for (Iterator<?> objIter = objects.iterator(); objIter.hasNext();) {
				Object o = objIter.next();
				if (o == null)
					continue;

				values = values.concat(o.toString());
				if (objIter.hasNext())
					values = values.concat(CoreConstants.SPACE);
			}

			luceneOptions.addFieldToDocument(name, values, document);
		}
	}

}