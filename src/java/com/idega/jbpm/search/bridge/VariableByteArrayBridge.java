package com.idega.jbpm.search.bridge;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayStringBridge;

public class VariableByteArrayBridge implements FieldBridge, TwoWayStringBridge {

	@Override
	public String objectToString(Object object) {
		// TODO Auto-generated method stub
		return object.toString();
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

		System.out.println("Set " + value + " for " + name);
	}

}