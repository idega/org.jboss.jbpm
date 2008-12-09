package com.idega.jbpm.bundle;

import java.io.IOException;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;

import com.idega.jbpm.view.ViewResource;

/**
 * represents deployable process bundle, which resolves process definition and
 * viewResources for this bundle
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 * 
 *          Last modified: $Date: 2008/12/09 02:46:40 $ by $Author: civilis $
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
	 * @return all different type viewResources for given task name. There
	 *         should be only one resource for one view type
	 * @throws IOException
	 */
	public abstract List<ViewResource> getViewResources(String taskName)
			throws IOException;

	/**
	 * One of the main responsibilities is to set the start task
	 * 
	 * could add specific info, or configurations for process definition created
	 * 
	 * @param pd
	 *            - process definition
	 */
	public abstract void configure(ProcessDefinition pd);

	public abstract String getManagersType();

	public abstract void setBundleResources(
			ProcessBundleResources bundleResources);
}