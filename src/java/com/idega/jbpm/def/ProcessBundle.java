package com.idega.jbpm.def;

import java.io.IOException;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 * 
 * Last modified: $Date: 2008/02/06 11:49:49 $ by $Author: civilis $
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
	 * could add specific info, or configurations for process definition created
	 * @param pd - process definition
	 */
	public abstract void configure(ProcessDefinition pd);
	
	public abstract String getManagersType();
}