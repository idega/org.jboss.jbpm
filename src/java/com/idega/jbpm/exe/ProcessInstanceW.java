package com.idega.jbpm.exe;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/04 18:12:27 $ by $Author: civilis $
 */
public interface ProcessInstanceW {
	
	public abstract TaskInstanceW getTaskInstance(long tiId);
}