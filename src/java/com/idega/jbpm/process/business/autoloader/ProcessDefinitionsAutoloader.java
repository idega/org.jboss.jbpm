package com.idega.jbpm.process.business.autoloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.xml.parsers.DocumentBuilder;

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
import com.idega.idegaweb.IWMainSlideStartedEvent;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.def.ProcessBundle;
import com.idega.util.CoreConstants;
import com.idega.util.xml.XPathUtil;
import com.idega.util.xml.XmlUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/05/10 18:10:25 $ by $Author: civilis $
 */
public abstract class ProcessDefinitionsAutoloader implements ApplicationListener, ApplicationContextAware {

	private final Logger logger;
	private ResourcePatternResolver resourcePatternResolver;
	private List<String> mappings;
	private IdegaJbpmContext idegaJbpmContext;
	private ApplicationContext appCtx;
	
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
	
	public synchronized void autodeploy() {
		
		IWMainApplication iwma;
		FacesContext fctx = FacesContext.getCurrentInstance();
		
		if(fctx != null)
			iwma = IWMainApplication.getIWMainApplication(fctx);
		else
			iwma = IWMainApplication.getDefaultIWMainApplication();
		
		autodeploy(iwma);
	}
	
	protected void autodeploy(IWMainApplication iwma) {
		
		final List<String> mappings = getMappings();
		final List<Resource> allResources = resolveResources(mappings);
		
		List<AutoDeployable> processDefinitionsAutos = resolveProcessDefinitionsAutodeployables(iwma, allResources);
		List<AutoDeployable> processBundlesAutos = resolveProcessBundlesAutodeployables(iwma, allResources);
		
		processDefinitionsAutos.addAll(processBundlesAutos);
		
		HashSet<AutoDeployable> autos = new HashSet<AutoDeployable>(processDefinitionsAutos.size());
		
		for (Iterator<AutoDeployable> iterator = processDefinitionsAutos.iterator(); iterator.hasNext();) {
			AutoDeployable auto = iterator.next();
			
			if(auto.getNeedsDeploy())
				autos.add(auto);
		}
		
		for (AutoDeployable autoDeployable : autos) {
			
			autoDeployable.deploy(iwma);
		}
	}

	public void onApplicationEvent(ApplicationEvent applicationEvent) {
	
		if(applicationEvent instanceof IWMainSlideStartedEvent && false) {
			
			IWMainApplication iwma = ((IWMainSlideStartedEvent)applicationEvent).getIWMA();
			autodeploy(iwma);
		}
	}
	
	protected List<Resource> resolveResources(List<String> mappings) {
		
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
		return allResources;
	}
	
	protected List<AutoDeployable> resolveProcessDefinitionsAutodeployables(IWMainApplication iwma, List<Resource> resources) {
		
		ArrayList<AutoDeployable> autos = new ArrayList<AutoDeployable>();
		
		try {
			final String xpathExp = ".//processDefinition[@autoload='true']";
			
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
					
					AutoDeployable auto = createAutoDeployable();
					auto.setProcessDefinition(pd);
					autos.add(auto);
				}
			}
			
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving process definitions", e);
		}
		
		return autos;
	}
	
	protected abstract AutoDeployable createAutoDeployable();
	
	protected List<AutoDeployable> resolveProcessBundlesAutodeployables(IWMainApplication iwma, List<Resource> resources) {
		
		ArrayList<AutoDeployable> autos = new ArrayList<AutoDeployable>(resources.size());
		
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
					
					ProcessBundle pb = createProcessBundle(iwma, props, new Integer(version), pathToPropsWithinBundle, bundleIdentifier);
					
					AutoDeployable auto = createAutoDeployable();
					auto.setProcessBundle(pb);
					autos.add(auto);
				}
			}
			
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving process bundles", e);
		}
		
		return autos;
	}
	
	protected ProcessBundle createProcessBundle(IWMainApplication iwma, Properties props, Integer version, String pathToPropsWithinBundle, String bundleIdentifier) {
		
		String processBundleIdentifier = props.getProperty("process_definition.processBundle.beanIndentifier");
		
		try {
			ProcessBundle procBundle = (ProcessBundle)getApplicationContext().getBean(processBundleIdentifier);
			procBundle.setBundlePropertiesLocationWithinBundle(pathToPropsWithinBundle);
			procBundle.setBundle(iwma.getBundle(bundleIdentifier));
			ProcessDefinition pd = procBundle.getProcessDefinition();
			pd.setVersion(version);
			
			return procBundle;
			
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "process definition not found", e);
			return null;
		}
	}
	
	protected Logger getLogger() {
		
		return logger;
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
}