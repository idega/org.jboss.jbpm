package com.idega.jbpm.process.business.autoloader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

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
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.bundle.JarModuleBundleResourcesImpl;
import com.idega.jbpm.bundle.ProcessBundle;
import com.idega.jbpm.bundle.ProcessBundleFactory;
import com.idega.util.StringUtil;
import com.idega.util.xml.XPathUtil;
import com.idega.util.xml.XmlUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 * 
 *          Last modified: $Date: 2009/02/20 14:24:52 $ by $Author: civilis $
 */
public abstract class ProcessDefinitionsAutoloader implements
		ApplicationListener, ApplicationContextAware {

	private final Logger logger;
	private ResourcePatternResolver resourcePatternResolver;
	private List<String> mappings;
	private BPMContext idegaJbpmContext;
	private ApplicationContext appCtx;
	@Autowired
	private ProcessBundleFactory processBundleFactory;

	public ProcessDefinitionsAutoloader() {
		logger = Logger.getLogger(getClass().getName());
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public synchronized void forceAutodeploy() {

		IWMainApplication iwma = IWMainApplication
				.getDefaultIWMainApplication();
		autodeploy(iwma, true);
	}

	protected void autodeploy(IWMainApplication iwma, boolean force) {

		final List<String> mappings = getMappings();
		final List<Resource> allResources = resolveResources(mappings);

		List<AutoDeployable> autodeployables = resolveProcessBundlesAutodeployables(
				iwma, allResources);

		HashSet<AutoDeployable> autos = new HashSet<AutoDeployable>(
				autodeployables.size());

		for (Iterator<AutoDeployable> iterator = autodeployables.iterator(); iterator
				.hasNext();) {
			AutoDeployable auto = iterator.next();

			if (force) {
				auto.setNeedsDeploy(true);
				autos.add(auto);
			} else if (auto.getNeedsDeploy())
				autos.add(auto);
		}

		for (AutoDeployable autoDeployable : autos) {

			autoDeployable.deploy(iwma);
		}
	}

	public void onApplicationEvent(ApplicationEvent applicationEvent) {

		if (applicationEvent instanceof IWMainSlideStartedEvent) {

			IWMainApplication iwma = ((IWMainSlideStartedEvent) applicationEvent)
					.getIWMA();
			autodeploy(iwma, false);
		}
	}

	protected List<Resource> resolveResources(List<String> mappings) {

		final ArrayList<Resource> allResources = new ArrayList<Resource>();

		if (mappings != null) {

			try {
				for (String mapping : mappings) {

					final Resource[] resources = getResourcePatternResolver()
							.getResources(mapping);

					for (int i = 0; i < resources.length; i++) {

						allResources.add(resources[i]);
					}
				}
			} catch (Exception e) {
				getLogger()
						.log(
								Level.WARNING,
								"Exception while resolving pdm resources by pattern",
								e);
			}
		}
		return allResources;
	}

	protected abstract AutoDeployable createAutoDeployable();

	protected List<AutoDeployable> resolveProcessBundlesAutodeployables(
			IWMainApplication iwma, List<Resource> resources) {

		ArrayList<AutoDeployable> autos = new ArrayList<AutoDeployable>(
				resources.size());

		try {
			final String xpathExp = ".//processBundle[@autoload='true']";

			final DocumentBuilder docBuilder = XmlUtil.getDocumentBuilder();
			final XPathUtil ut = new XPathUtil(xpathExp);

			for (Resource resource : resources) {

				final Document doc;
				try {
					doc = docBuilder.parse(resource.getInputStream());

				} catch (Exception e) {
					getLogger().log(
							Level.WARNING,
							"Exception while parsting resource: "
									+ resource.getFilename(), e);
					continue;
				}

				NodeList bundles = ut.getNodeset(doc);

				for (int i = 0; i < bundles.getLength(); i++) {

					final Element BndlDesc = (Element) bundles.item(i);

					String bundleIdentifier = BndlDesc.getAttribute("bundle");
					String processBundleLocationWithinBundle = BndlDesc
							.getAttribute("processBundleLocationWithinBundle");
					String versionStr = BndlDesc.getAttribute("version");

					String autoloadStr = BndlDesc.getAttribute("autoload");

					Boolean autoload = "true".equals(autoloadStr);

					if (!autoload)
						// skip this autodeployabe if said so in the cfg
						continue;

					if (StringUtil.isEmpty(processBundleLocationWithinBundle)) {
						continue;
					}

					final Integer version;

					if (!StringUtil.isEmpty(versionStr))
						version = new Integer(versionStr);
					else
						version = null;

					try {
						IWBundle bundle = iwma.getBundle(bundleIdentifier);
						JarModuleBundleResourcesImpl jarModuleResources = new JarModuleBundleResourcesImpl();
						jarModuleResources.setBundle(bundle);
						jarModuleResources
								.setProcessBundleLocationWithinBundle(processBundleLocationWithinBundle);

						ProcessBundle pb = getProcessBundleFactory()
								.createProcessBundle(jarModuleResources,
										version);

						if (pb != null) {

							AutoDeployable auto = createAutoDeployable();
							auto.setProcessBundle(pb);
							autos.add(auto);
						}

					} catch (Exception e) {
						getLogger().log(Level.WARNING,
								"Exception while resolving proces bundle", e);
					}
				}
			}

		} catch (Exception e) {
			getLogger().log(Level.SEVERE,
					"Exception while resolving process bundles", e);
		}

		return autos;
	}

	protected Logger getLogger() {

		return logger;
	}

	public ResourcePatternResolver getResourcePatternResolver() {
		return resourcePatternResolver;
	}

	public void setResourcePatternResolver(
			ResourcePatternResolver resourcePatternResolver) {
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

	public ProcessBundleFactory getProcessBundleFactory() {
		return processBundleFactory;
	}
}