package com.idega.jbpm.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.idegaweb.IWMainApplication;
import com.idega.util.CoreConstants;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
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
		this(new HashMap<String, Class<?>>());
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

	@SuppressWarnings("unchecked")
	public <T>T convertToObject(String jsonStr) {
		if (StringUtil.isEmpty(jsonStr) || !jsonStr.startsWith(CoreConstants.CURLY_BRACKET_LEFT)) {
			return null;
		}

		for (String alias: aliasMap.keySet()) {
			xstream.alias(alias, aliasMap.get(alias));
		}

		T obj = null;
		try {
			if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("jbpm.fix_json_start_end", false)) {
				String invalidStart = "{\"list\":{\"string\":\"{";
				if (jsonStr.startsWith(invalidStart)) {
					jsonStr = StringHandler.replace(jsonStr, invalidStart, "{\"list\":{\"string\":[\"{");
				}
				String invalidEnd = "\"}}";
				if (jsonStr.endsWith(invalidEnd)) {
					jsonStr = jsonStr.substring(0, jsonStr.length() - invalidEnd.length()).concat("\"]}}");
				}
			}
			obj = (T) xstream.fromXML(jsonStr);
		} catch (Exception e) {
			Logger.getLogger(JSONUtil.class.getName()).log(Level.WARNING, "Error converting JSON ('" + jsonStr + "') to object", e);
		}
		return obj;
	}

	public void setAliases(Map<String, Class<?>> aliases) {
		this.aliasMap = aliases;
	}

	public void addAlias(String key, Class<?> theClass) {
		if (aliasMap == null) {
			aliasMap = new HashMap<>();
		}
		aliasMap.put(key, theClass);
	}
}