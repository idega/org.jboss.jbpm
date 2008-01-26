package com.idega.jbpm.exe;

import java.util.List;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/01/26 09:46:42 $ by $Author: civilis $
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
	
	public abstract View getView(long taskId, boolean submitable);
	
	public abstract View getView(long taskId, boolean submitable, List<String> preferredTypes);
}