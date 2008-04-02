package com.idega.jbpm.process.business.autoloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
import com.idega.idegaweb.IWMainSlideStartedEvent;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.def.ProcessBundle;
import com.idega.jbpm.def.ProcessBundleManager;
import com.idega.util.CoreConstants;
import com.idega.util.xml.XPathUtil;
import com.idega.util.xml.XmlUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/04/02 19:26:24 $ by $Author: civilis $
 */
public class ProcessDefinitionsAutoloader implements ApplicationListener, ApplicationContextAware {

	private final Logger logger;
	private ResourcePatternResolver resourcePatternResolver;
	private List<String> mappings;
	private IdegaJbpmContext idegaJbpmContext;
	private ApplicationContext appCtx;
	private ProcessBundleManager processBundleManager;
	
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
	
		if(applicationEvent instanceof IWMainApplicationStartedEvent || applicationEvent instanceof IWMainSlideStartedEvent) {
			
			final List<String> mappings = getMappings();
			final ArrayList<Resource> allResources = new ArrayList<Resource>();
			
			if(mappings != null) {
			
				try {
					for (String mapping : mappings) {
						
						final Resource[] resources = getResourcePatternResolver().getResources(mapping);
						
						for (int i = 0; i < resources.length; i++) {

							allResources.add(resources[i]);
						}
					}
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Exception while resolving pdm resources by pattern", e);
				}
			}
			
			if(applicationEvent instanceof IWMainApplicationStartedEvent) {
				
				IWMainApplication iwma = ((IWMainApplicationStartedEvent)applicationEvent).getIWMA();
				
				List<ProcessDefinition> processDefinitions = resolveProcessDefinitions(iwma, allResources);
				
				if(!processDefinitions.isEmpty()) {
				
					JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
					
					try {
						
						for (ProcessDefinition processDefinition : processDefinitions) {
							
							checkDeployProcessDefinition(ctx, processDefinition, true);
						}
						
					} finally {
						getIdegaJbpmContext().closeAndCommit(ctx);
					}
				}
				
			} else if (applicationEvent instanceof IWMainSlideStartedEvent) {
				
				IWMainApplication iwma = ((IWMainSlideStartedEvent)applicationEvent).getIWMA();
				
				JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
				
				try {
					createProcessBundles(ctx, iwma, allResources);
					
				} finally {
					getIdegaJbpmContext().closeAndCommit(ctx);
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
					
					String bundleIdentifier = PDDesc.getAttribute("bundle");
					String pathToPDWithinBundle = PDDesc.getAttribute("uriWithinBundle");
					String version = PDDesc.getAttribute("version");
					
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
	
	protected void createProcessBundles(JbpmContext ctx, IWMainApplication iwma, List<Resource> resources) {
		
		try {
			final String xpathExp = ".//processBundle[@autoload='true']";
			
			final DocumentBuilder docBuilder = XmlUtil.getDocumentBuilder();
			final XPathUtil ut = new XPathUtil(xpathExp);
			
			for (Resource resource : resources) {
		
				final Document doc;
				try {
					doc = docBuilder.parse(resource.getInputStream());
					
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Exception while parsting resource: "+resource.getFilename(), e);
					continue;
				}
	
				NodeList bundles = ut.getNodeset(doc);
				
				for (int i = 0; i < bundles.getLength(); i++) {
					
					final Element BndlDesc = (Element)bundles.item(i);
					
					
					
					String bundleIdentifier = BndlDesc.getAttribute("bundle");
					String pathToPropsWithinBundle = BndlDesc.getAttribute("propsUriWithinBundle");
					String version = BndlDesc.getAttribute("version");
					
					IWBundle bundle = iwma.getBundle(bundleIdentifier);
					InputStream is = bundle.getResourceInputStream(pathToPropsWithinBundle);
					
					Properties props = new Properties();
					props.load(is);
					
					checkCreateBundle(iwma, ctx, props, new Integer(version), pathToPropsWithinBundle, bundleIdentifier);
				}
			}
			
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving process bundles", e);
		}
	}
	
	protected void checkCreateBundle(IWMainApplication iwma, JbpmContext ctx, Properties props, Integer version, String pathToPropsWithinBundle, String bundleIdentifier) {
		
		String processBundleIdentifier = props.getProperty("process_definition.processBundle.beanIndentifier");
		
		try {
			ProcessBundle procBundle = (ProcessBundle)getApplicationContext().getBean(processBundleIdentifier);
			procBundle.setBundlePropertiesLocationWithinBundle(pathToPropsWithinBundle);
			procBundle.setBundle(iwma.getBundle(bundleIdentifier));
			ProcessDefinition pd = procBundle.getProcessDefinition();
			pd.setVersion(version);
			
			if(checkDeployProcessDefinition(ctx, pd, false))
				getProcessBundleManager().createBundle(procBundle, pd.getName(), iwma);
			
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "process definition not found", e);
			return;
		}
	}
	
	protected Logger getLogger() {
		
		return logger;
	}
	
	protected boolean checkDeployProcessDefinition(JbpmContext ctx, ProcessDefinition processDefinition, boolean deploy) {

		try {
			String pdName = processDefinition.getName();
			ProcessDefinition pd = ctx.getGraphSession().findLatestProcessDefinition(pdName);
			
			if(pd != null) {
				
				if(processDefinition.getVersion() <= pd.getVersion()) {
				
					getLogger().log(Level.INFO, "Found deployed process: "+pd.getName());
					return false;
				}
			}
			
		} catch (JbpmException e) {
//			thrown when nothing found
		}
		
		if(deploy) {
		
			getLogger().log(Level.INFO, "Deploying process definition: "+processDefinition.getName());
			
			try {
				ctx.deployProcessDefinition(processDefinition);
				getLogger().log(Level.INFO, "Deployed process definition: "+processDefinition.getName());
				
			} catch (JbpmException ee) {
				getLogger().log(Level.WARNING, "Failed to deploy process: "+processDefinition.getName(), ee);
			}
		}
		
		return true;
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
	
	public void setApplicationContext(ApplicationContext appCtx)
			throws BeansException {
		this.appCtx = appCtx;
	}
	
	public ApplicationContext getApplicationContext() {
		return appCtx;
	}

	public ProcessBundleManager getProcessBundleManager() {
		return processBundleManager;
	}

	@Autowired
	public void setProcessBundleManager(ProcessBundleManager processBundleManager) {
		this.processBundleManager = processBundleManager;
	}
}