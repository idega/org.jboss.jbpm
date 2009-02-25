package com.idega.jbpm.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.instantiation.Delegation;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWBundle;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.proxy.JbpmHandlerProxy;
import com.idega.jbpm.task.TaskInstanceViewBindHandler;
import com.idega.jbpm.view.ViewResource;

/**
 * Implementation of ProcessBundle for single processdefinition.xml
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $ Last modified: $Date: 2009/02/25 14:18:16 $ by $Author: civilis $
 */
@Scope("prototype")
@Service(ProcessBundleSingleProcessDefinitionImpl.beanIdentifier)
public class ProcessBundleSingleProcessDefinitionImpl implements ProcessBundle {
	
	private ProcessDefinition pd;
	private static final String processDefinitionFileName = "processdefinition.xml";
	
	public static final String beanIdentifier = "BPMProcessBundleSingleProcessDefinition";
	
	private String processManagerType;
	
	private ProcessBundleResources bundleResources;
	private IWBundle bundle;
	
	public List<ViewResource> getViewResources(String taskName)
	        throws IOException {
		
		return Collections.emptyList();
	}
	
	public IWBundle getBundle() {
		return bundle;
	}
	
	public void setBundle(IWBundle bundle) {
		this.bundle = bundle;
	}
	
	public void configure(ProcessDefinition pd) {
		
		// adding task instance create event handler, which will bind/take view
		// for created task instance
		Event event;
		String eventType = Event.EVENTTYPE_TASK_CREATE;
		
		if (!pd.hasEvent(eventType)) {
			
			event = pd.addEvent(new Event(eventType));
		} else {
			event = pd.getEvent(eventType);
		}
		
		Delegation del = new Delegation(JbpmHandlerProxy.class.getName());
		del.setConfiguration(new StringBuilder("<handlerName>")
		        .append(TaskInstanceViewBindHandler.beanIdentifier)
		        .append("</handlerName>")
		        // =here we add async execution for the task-create handler only for non shared
		        // tasks. That's because for shared tasks, they are created immediately on load, and
		        // if we do take view async there, we might not be able to take view before it is
		        // displayed. So it needs to be synchronous in that case
		        .append("<async>").append(
		            "${return taskInstance.getPriority() != ")
		        .append(ProcessConstants.PRIORITY_SHARED_TASK).append(";}")
		        .append("</async>")
		        // =/
		        .toString());
		
		Action act = new Action(del);
		// act.setAsync(true);
		act.setName(TaskInstanceViewBindHandler.beanIdentifier);
		event.addAction(act);
	}
	
	public ProcessBundleResources getBundleResources() {
		return bundleResources;
	}
	
	public void setBundleResources(ProcessBundleResources bundleResources) {
		this.bundleResources = bundleResources;
	}
	
	public String getProcessManagerType() {
		return processManagerType != null ? processManagerType : "default";
	}
	
	public void setProcessManagerType(String processManagerType) {
		this.processManagerType = processManagerType;
	}
	
	public ProcessDefinition getProcessDefinition() throws IOException {
		
		if (pd == null) {
			
			// TODO: create method to resolve process definition in the
			// bundleResource object
			InputStream pdIs = getBundleResources().getResourceIS(
			    processDefinitionFileName);
			
			if (pdIs != null) {
				ProcessDefinition pd = com.idega.jbpm.graph.def.IdegaProcessDefinition
				        .parseXmlInputStream(pdIs);
				this.pd = pd;
			}
			if (pd == null)
				throw new RuntimeException("Process Defintion is " + pd);
			
		}
		
		return pd;
	}
}