package com.idega.jbpm.utils;

import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 *
 * 
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2009/03/18 11:55:55 $ by $Author: civilis $
 *
 */

public class JSONUtil {
	private XStream xstream;
	
	private Map<String, Class<?>> aliasMap;
	
	public JSONUtil() {
		xstream = new XStream(new JettisonMappedXmlDriver());
	}
	
	public JSONUtil(Map<String, Class<?>> aliases) {
		xstream = new XStream(new JettisonMappedXmlDriver());
		aliasMap = aliases;
	}
	
	public String convertToJSON(Object obj, Map<String, Class<?>> aliases) {
		aliasMap = aliases;
		for(String alias: aliasMap.keySet()) {
			xstream.alias(alias, aliasMap.get(alias));
		}

		String jsonStr = xstream.toXML(obj);
		return jsonStr;
	}
	
	public Object convertToObject(String jsonStr, Map<String, Class<?>> aliases) {
		aliasMap = aliases;
		for(String alias: aliasMap.keySet()) {
			xstream.alias(alias, aliasMap.get(alias));
		}
		Object obj = xstream.fromXML(jsonStr);
		return obj;
	}
	
	public String convertToJSON(Object obj) {
		for(String alias: aliasMap.keySet()) {
			xstream.alias(alias, aliasMap.get(alias));
		}

		String jsonStr = xstream.toXML(obj);
		return jsonStr;
	}
	
	public <T>T convertToObject(String jsonStr) {
		for(String alias: aliasMap.keySet()) {
			xstream.alias(alias, aliasMap.get(alias));
		}
		@SuppressWarnings("unchecked")
		T obj = (T)xstream.fromXML(jsonStr);
		return obj;
	}
}
