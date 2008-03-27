package com.idega.jbpm.exe.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/27 14:14:03 $ by $Author: civilis $
 */
public class BinaryVariablesHandlerImpl {

	public Map<String, Object> storeBinaryVariables(Map<String, Object> variables) {
		
		for (Entry<String, Object> entry : variables.entrySet()) {
			
			Object val = entry.getValue();
			
			if(val == null)
				continue;
			
			if(val instanceof File) {

				
				
			} else if(val instanceof Collection) {
				
			}
		}
		
		return null;
	}
	
	public String storeFile() {
		
		return null;
	}
	
	public Map<String, Object> resolveBinaryVariables(Map<String, Object> variables) {
	
		return null;
	}
	
	public InputStream getBinaryVariableContent(String variableValue) {
		
		return null;
	}
}