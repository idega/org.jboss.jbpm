package com.idega.jbpm.bundle;

import java.io.IOException;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;

import com.idega.jbpm.view.ViewResource;
import com.idega.jbpm.view.ViewToTask;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/07/19 20:40:58 $ by $Author: civilis $
 */
public interface ProcessBundle {

	/**
	 * 
	 * @return should return process definition, which would be persisted
	 * @throws IOException
	 */
	public abstract ProcessDefinition getProcessDefinition() throws IOException;

	/**
	 * @param taskName
	 * @return all different type viewResources for given task name. There should be only one resource for one type
	 * @throws IOException
	 */
	public abstract List<ViewResource> getViewResources(String taskName) throws IOException;
	
	/**
	 * One of the main responsibilities is to set the start task
	 * 
	 * could add specific info, or configurations for process definition created
	 * @param pd - process definition
	 */
	public abstract void configure(ProcessDefinition pd);
	
	public abstract String getManagersType();
	
	public abstract ViewToTask getViewToTaskBinder();
	
//	public abstract void setBundlePropertiesLocationWithinBundle(String bundlePropertiesLocationWithinBundle);
//	
//	public abstract void setBundle(IWBundle bundle);
	
	public abstract void setBundleResources(ProcessBundleResources bundleResources);
}