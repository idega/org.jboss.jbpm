package com.idega.jbpm.variables;

import java.util.Map;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/19 13:52:39 $ by $Author: civilis $
 */
public interface Converter {

	public abstract Map<String, Object> convert(Object specificData);
	public abstract Object revert(Map<String, Object> variables, Object objectToPopulate);
}