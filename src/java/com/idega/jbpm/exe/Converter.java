package com.idega.jbpm.exe;

import java.util.Map;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/21 11:29:39 $ by $Author: civilis $
 */
public interface Converter {

	public abstract Map<String, Object> convert(Object specificData);
	public abstract Object revert(Map<String, Object> variables, Object objectToPopulate);
}