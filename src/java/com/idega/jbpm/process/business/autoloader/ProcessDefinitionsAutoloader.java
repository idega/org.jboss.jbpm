package com.idega.jbpm.process.business.autoloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationStartedEvent;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.util.CoreConstants;
import com.idega.util.xml.XPathUtil;
import com.idega.util.xml.XmlUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/18 15:06:00 $ by $Author: civilis $
 */
public class ProcessDefinitionsAutoloader implements ApplicationListener {

	private final Logger logger;
	private ResourcePatternResolver resourcePatternResolver;
	private List<String> mappings;
	private IdegaJbpmContext idegaJbpmContext;
	
	private static final String bundleAtt = "bundle";
	private static final String uriWithinBundleAtt = "uriWithinBundle";
	private static final String versionAtt = "version";
	
	public ProcessDefinitionsAutoloader() {
		logger = Logger.getLogger(getClass().getName());
	}
	
	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public void onApplicationEvent(ApplicationEvent applicationEvent) {
	
		if(applicationEvent instanceof IWMainApplicationStartedEvent) {
			
			IWMainApplication iwma = ((IWMainApplicationStartedEvent)applicationEvent).getIWMA();
			
			final List<String> mappings = getMappings();
			
			if(mappings != null) {
			
				try {
					final ArrayList<Resource> allResources = new ArrayList<Resource>();
					
					for (String mapping : mappings) {
						
						final Resource[] resources = getResourcePatternResolver().getResources(mapping);
						
						for (int i = 0; i < resources.length; i++) {

							allResources.add(resources[i]);
						}
					}
					
					List<ProcessDefinition> processDefinitions = resolveProcessDefinitions(iwma, allResources);
					
					if(!processDefinitions.isEmpty()) {
					
						JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
						
						try {
							
							for (ProcessDefinition processDefinition : processDefinitions) {
								
								checkDeployProcessDefinition(ctx, processDefinition);
							}
							
						} finally {
							getIdegaJbpmContext().closeAndCommit(ctx);
						}
					}
					
				} catch (IOException e) {
					
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	protected List<ProcessDefinition> resolveProcessDefinitions(IWMainApplication iwma, List<Resource> resources) {
		
		try {
			final String xpathExp = ".//processDefinition[@autoload='true']";
			
			final DocumentBuilder docBuilder = XmlUtil.getDocumentBuilder();
			final XPathUtil ut = new XPathUtil(xpathExp);
			
			ArrayList<ProcessDefinition> pds = new ArrayList<ProcessDefinition>();
			
			for (Resource resource : resources) {
		
				final Document doc;
				try {
					doc = docBuilder.parse(resource.getInputStream());
					
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Exception while parsting resource: "+resource.getFilename(), e);
					continue;
				}
	
				NodeList pdDescs = ut.getNodeset(doc);
				
				for (int i = 0; i < pdDescs.getLength(); i++) {
					
					final Element PDDesc = (Element)pdDescs.item(i);
					
					String bundleIdentifier = PDDesc.getAttribute(bundleAtt);
					String pathToPDWithinBundle = PDDesc.getAttribute(uriWithinBundleAtt);
					String version = PDDesc.getAttribute(versionAtt);
					
					IWBundle bundle = iwma.getBundle(bundleIdentifier);
					InputStream is = bundle.getResourceInputStream(pathToPDWithinBundle);
					
					ProcessDefinition pd = ProcessDefinition.parseXmlInputStream(is);
					
					if(version != null && !CoreConstants.EMPTY.equals(version)) {
						try {
							pd.setVersion(new Integer(version));
						} catch (NumberFormatException e) { }
					}
					
					pds.add(pd);
				}
			}
			
			return pds;
			
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving process definitions", e);
			return null;
		}
	}
	
	protected Logger getLogger() {
		
		return logger;
	}
	
	protected void checkDeployProcessDefinition(JbpmContext ctx, ProcessDefinition processDefinition) {

		try {
			String pdName = processDefinition.getName();
			ProcessDefinition pd = ctx.getGraphSession().findLatestProcessDefinition(pdName);
			
			if(pd != null) {
				
				if(processDefinition.getVersion() <= pd.getVersion()) {
				
					getLogger().log(Level.INFO, "Found deployed process: "+pd.getName());
					return;
				}
			}
			
		} catch (JbpmException e) {
//			thrown when nothing found
		}
		
		getLogger().log(Level.INFO, "Deploying process definition: "+processDefinition.getName());
		
		try {
			ctx.deployProcessDefinition(processDefinition);
			getLogger().log(Level.INFO, "Deployed process definition: "+processDefinition.getName());
			
		} catch (JbpmException ee) {
			getLogger().log(Level.WARNING, "Failed to deploy process: "+processDefinition.getName(), ee);
		}
	}
	
	public ResourcePatternResolver getResourcePatternResolver() {
		return resourcePatternResolver;
	}

	public void setResourcePatternResolver(ResourcePatternResolver resourcePatternResolver) {
		this.resourcePatternResolver = resourcePatternResolver;
	}

	public List<String> getMappings() {
		return mappings;
	}

	public void setMappings(List<String> mappings) {
		this.mappings = mappings;
	}
}