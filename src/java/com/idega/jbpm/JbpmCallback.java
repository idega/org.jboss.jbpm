package com.idega.jbpm;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;

/**
 * Copied from jbpm springmodules
 * 
 * Jbpm 3.1 callback which allows code to be executed directly on the
 * jbpmContext.
 * 
 * original @author Costin Leau
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2008/12/28 12:08:03 $ by $Author: civilis $
 */
public interface JbpmCallback {

	/**
	 * JbpmContext callback.
	 * 
	 * @param context
	 * @return
	 * @throws JbpmException
	 */
	public Object doInJbpm(JbpmContext context) throws JbpmException;
}
