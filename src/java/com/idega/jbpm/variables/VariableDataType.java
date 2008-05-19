package com.idega.jbpm.variables;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/19 13:52:39 $ by $Author: civilis $
 */
public enum VariableDataType {

	DATE {public String toString() { return date; }},
	STRING {public String toString() { return string; }},
	LIST {public String toString() { return list; }},
	FILE {public String toString() { return file; }},
	FILES {public String toString() { return files; }};
	
	private static final String date = 		"date";
	private static final String string = 	"string";
	private static final String list = 		"list";
	private static final String file = 		"file";
	private static final String files =  	"files";
	
	public static Set<String> getAllTypesInStrings() {
		
		return getAllDataTypesEnumsMappings().keySet();
	}
	
	final private static Map<String, VariableDataType> allDataTypesEnumsMappings = new HashMap<String, VariableDataType>();
	
	static {
		
		for (VariableDataType type : values())
			allDataTypesEnumsMappings.put(type.toString(), type);
	}
	
	private static Map<String, VariableDataType> getAllDataTypesEnumsMappings() {
		
		return allDataTypesEnumsMappings;
	}
	
	public static VariableDataType getByStringRepresentation(String type) {
		
		return getAllDataTypesEnumsMappings().get(type);
	}
	
	public abstract String toString();
}