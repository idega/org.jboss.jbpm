package com.idega.jbpm.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.idega.idegaweb.IWBundle;
import com.idega.jbpm.view.ViewResource;
import com.idega.jbpm.view.ViewResourceFactoryImpl;
import com.idega.util.StringUtil;
import com.idega.util.xml.XPathUtil;

/**
 * Default implementation of ProcessBundle
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 *          Last modified: $Date: 2009/01/29 10:35:55 $ by $Author: arunas $
 * 
 */
@Scope("prototype")
@Service("defaultBPMProcessBundle")
public class ProcessBundleDefaultImpl implements ProcessBundle {

	private static final String processDefinitionFileName = "processdefinition.xml";

	private String processManagerType;
	@Autowired
	private ViewResourceFactoryImpl viewResourceFactory;

	private ProcessBundleResources bundleResources;
	private IWBundle bundle;
	private String bundlePropertiesLocationWithinBundle;
	private ProcessDefinition pd;

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

		return pd ;
	}

	public List<ViewResource> getViewResources(String taskName)
			throws IOException {

		ProcessBundleResources resources = getBundleResources();
		Document cfg = resources.getConfiguration();

		XPathUtil xut = new XPathUtil("//tasks/task[@name='" + taskName
				+ "']/viewResource");

		NodeList viewResourcesCfg = xut.getNodeset(cfg);

		ArrayList<ViewResource> viewResources = new ArrayList<ViewResource>(
				viewResourcesCfg.getLength());

		for (int i = 0; i < viewResourcesCfg.getLength(); i++) {

			Element viewResourceElement = (Element) viewResourcesCfg.item(i);

			String viewResourceIdentifier = viewResourceElement
					.getAttribute("resourceIdentifier");
			String viewType = viewResourceElement.getAttribute("viewType");
			String viewResourceType = viewResourceElement
					.getAttribute("viewResourceType");

			ViewResource viewResource = getViewResourceFactory()
					.getViewResource(viewResourceType, viewResourceIdentifier,
							taskName, resources);
			viewResource.setViewType(viewType);
			viewResources.add(viewResource);
		}

		return viewResources;
	}

	public IWBundle getBundle() {
		return bundle;
	}

	public void setBundle(IWBundle bundle) {
		this.bundle = bundle;
	}

	public String getBundlePropertiesLocationWithinBundle() {
		return bundlePropertiesLocationWithinBundle;
	}

	public void setBundlePropertiesLocationWithinBundle(
			String bundlePropertiesLocationWithinBundle) {
		this.bundlePropertiesLocationWithinBundle = bundlePropertiesLocationWithinBundle;
	}

	public void configure(ProcessDefinition pd) {

		Document cfg = getBundleResources().getConfiguration();

		XPathUtil xut = new XPathUtil("//tasks/task[@initial='true']");
		Element initialTaskElement = xut.getNode(cfg);

		if (initialTaskElement == null)
			throw new IllegalArgumentException(
					"Wrong bundle configuration file, no initial task element found. One task element should be annotated with initial=\"true\" attribute");

		String initialTaskName = initialTaskElement.getAttribute("name");

		if (StringUtil.isEmpty(initialTaskName))
			throw new IllegalArgumentException(
					"Wrong bundle configuration file, initial task element didn't have name attribute, which should reflect the task name");

		Task initialTask = pd.getTaskMgmtDefinition().getTask(initialTaskName);
		pd.getTaskMgmtDefinition().setStartTask(initialTask); // TODO: perhaps
		// this is done
		// automatically
		// by jbpm?
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

	protected ViewResourceFactoryImpl getViewResourceFactory() {
		return viewResourceFactory;
	}
}