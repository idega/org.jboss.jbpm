package com.idega.jbpm.def;

import java.io.IOException;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 * Last modified: $Date: 2008/01/27 13:11:35 $ by $Author: civilis $
 */
public interface ProcessBundle {

	public abstract ProcessDefinition getProcessDefinition() throws IOException;

	public abstract List<ViewResource> getViewResources(String taskName) throws IOException;
}