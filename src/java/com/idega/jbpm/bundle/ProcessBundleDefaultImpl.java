package com.idega.jbpm.bundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.idega.jbpm.view.ViewResource;
import com.idega.jbpm.view.ViewResourceFactoryImpl;
import com.idega.util.StringUtil;
import com.idega.util.xml.XPathUtil;

/**
 * Default implementation of ProcessBundle
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2009/02/20 14:24:52 $ by $Author: civilis $
 * 
 */
@Scope("prototype")
@Service("defaultBPMProcessBundle")
public class ProcessBundleDefaultImpl extends
		ProcessBundleSingleProcessDefinitionImpl {

	@Autowired
	private ViewResourceFactoryImpl viewResourceFactory;

	@Override
	public List<ViewResource> getViewResources(String taskName) throws IOException {
		ProcessBundleResources resources = getBundleResources();
		Document cfg = resources.getConfiguration();

		XPathUtil xut = new XPathUtil("//tasks/task[@name='" + taskName + "']/viewResource");

		NodeList viewResourcesCfg = xut.getNodeset(cfg);
		List<ViewResource> viewResources = new ArrayList<ViewResource>(viewResourcesCfg.getLength());

		for (int i = 0; i < viewResourcesCfg.getLength(); i++) {
			Element viewResourceElement = (Element) viewResourcesCfg.item(i);

			Integer order = null;
			Node parent = viewResourceElement.getParentNode();
			if (parent instanceof Element) {
				String orderValue = ((Element) parent).getAttribute("order");
				if (!StringUtil.isEmpty(orderValue)) {
					order = Integer.valueOf(orderValue);
				}
			}
			
			String viewResourceIdentifier = viewResourceElement.getAttribute("resourceIdentifier");
			String viewType = viewResourceElement.getAttribute("viewType");
			String viewResourceType = viewResourceElement.getAttribute("viewResourceType");

			ViewResource viewResource = getViewResourceFactory().getViewResource(viewResourceType, viewResourceIdentifier, taskName, resources);
			viewResource.setViewType(viewType);
			viewResource.setOrder(order);
			viewResources.add(viewResource);
		}

		return viewResources;
	}

	@Override
	public void configure(ProcessDefinition pd) {

		super.configure(pd);

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

	protected ViewResourceFactoryImpl getViewResourceFactory() {
		return viewResourceFactory;
	}
}