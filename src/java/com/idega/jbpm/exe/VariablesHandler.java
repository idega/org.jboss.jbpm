package com.idega.jbpm.exe;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/10/01 16:32:27 $ by $Author: civilis $
 */
public interface VariablesHandler {

	public abstract void submit(long tiId, Object submissionData);
	
	public abstract Object populate(long tiId, Object objectToPopulate);
}