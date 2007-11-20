package com.idega.jbpm.exe;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2007/11/20 19:58:24 $ by $Author: civilis $
 */
public interface VariablesHandler {

	public abstract void submit(long tiId, Object submissionData);
	
	public abstract Object populate(long tiId, Object objectToPopulate);
	
	public abstract Object populateFromProcess(long processInstanceId, Object objectToPopulate);
}