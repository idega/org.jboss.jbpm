package com.idega.jbpm.exe;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/01/25 15:24:25 $ by $Author: civilis $
 */
public interface BPMFactory {
	
	/**
	 * 
	 * @param taskInstanceId
	 * @param viewTypes - optional. Declares the ordered list to look for views
	 * @return Finds first init task of process definition and returns View Manager for it.
	 */
	public abstract ViewManager getViewManager(long processDefinitionId);
	
	/**
	 * 
	 * @param viewType
	 * @return If no viewType is provided, the default process manager would be returned (not implemented yet)
	 */
	public abstract ProcessManager getProcessManager(long processDefinitionId);
}