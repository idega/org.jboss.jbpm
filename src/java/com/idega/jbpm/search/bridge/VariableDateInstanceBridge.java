package com.idega.jbpm.search.bridge;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayStringBridge;

import com.idega.util.StringUtil;

public class VariableDateInstanceBridge implements FieldBridge, TwoWayStringBridge {

	public static final SimpleDateFormat DATE_VAR_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if (value == null)
			return;

		Field field = new Field(name, String.valueOf(value), luceneOptions.getStore(), luceneOptions.getIndex(),
	            luceneOptions.getTermVector());
		document.add(field);
	}

	@Override
	public String objectToString(Object object) {
		if (object instanceof Timestamp)
			return String.valueOf(object);

		throw new IllegalArgumentException("Invalid object: " + object);
	}

	@Override
	public Object stringToObject(String stringValue) {
		if (StringUtil.isEmpty(stringValue))
			return null;

		try {
			Date date = DATE_VAR_FORMATTER.parse(stringValue);
			return date;
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return null;
	}

}