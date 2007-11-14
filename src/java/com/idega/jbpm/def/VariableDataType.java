package com.idega.jbpm.def;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/14 13:11:08 $ by $Author: civilis $
 */
public enum VariableDataType {

	DATE {public String toString() { return date; }},
	STRING {public String toString() { return string; }},
	LIST {public String toString() { return list; }},
	FILE {public String toString() { return file; }};
	
	private static final String date = "date";
	private static final String string = "string";
	private static final String list = "list";
	private static final String file = "file";
	
	public static Set<String> getAllTypesInStrings() {
		
		return getAllDataTypesEnumsMappings().keySet();
	}
	
	private static Map<String, VariableDataType> allDataTypesEnumsMappings;
	
	private synchronized static Map<String, VariableDataType> getAllDataTypesEnumsMappings() {
		
		if(allDataTypesEnumsMappings == null) {
			
			allDataTypesEnumsMappings = new HashMap<String, VariableDataType>();
			
			for (VariableDataType type : values())
				allDataTypesEnumsMappings.put(type.toString(), type);
		}
		
		return allDataTypesEnumsMappings;
	}
	
	public static VariableDataType getByStringRepresentation(String type) {
		
		return getAllDataTypesEnumsMappings().get(type);
	}
	
	public abstract String toString();
}