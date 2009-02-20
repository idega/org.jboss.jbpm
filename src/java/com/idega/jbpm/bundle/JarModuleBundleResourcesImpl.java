package com.idega.jbpm.bundle;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;

import com.idega.idegaweb.IWBundle;
import com.idega.util.CoreConstants;
import com.idega.util.xml.XmlUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2009/02/20 14:24:52 $ by $Author: civilis $
 */
public class JarModuleBundleResourcesImpl implements ProcessBundleResources {

	private IWBundle bundle;
	private String processBundleLocationWithinBundle;
	private Document bundleConfigXML;

	public InputStream getResourceIS(String path) {

		String processBundleLocationWithinBundle = getProcessBundleLocationWithinBundle();

		if (processBundleLocationWithinBundle == null)
			throw new IllegalStateException(
					"No templateBundleLocationWithinBundle set");

		if (path.startsWith(CoreConstants.SLASH))
			path = path.substring(1);

		path = processBundleLocationWithinBundle + path;

		try {
			InputStream is = getBundle().getResourceInputStream(path);

			return is;

		} catch (IOException e) {
//			Logger.getLogger(getClass().getName()).log(
//					Level.SEVERE,
//					"Resource not found by the path=" + path + " at bundle="
//							+ getBundle().getBundleIdentifier());
		}

		return null;
	}

	public IWBundle getBundle() {
		return bundle;
	}

	public void setBundle(IWBundle bundle) {
		this.bundle = bundle;
	}

	public Document getConfiguration() {

		if (bundleConfigXML == null) {

			String configFileName = "bundle.xml";
			InputStream bundleConfigIS = getResourceIS(configFileName);

			if (bundleConfigIS == null)
				bundleConfigXML = null;
			else
				bundleConfigXML = XmlUtil.getXMLDocument(bundleConfigIS);
		}

		return bundleConfigXML;
	}

	public String getProcessBundleLocationWithinBundle() {

		if (!processBundleLocationWithinBundle.endsWith(CoreConstants.SLASH))
			processBundleLocationWithinBundle += CoreConstants.SLASH;

		return processBundleLocationWithinBundle;
	}

	public void setProcessBundleLocationWithinBundle(
			String processBundleLocationWithinBundle) {
		this.processBundleLocationWithinBundle = processBundleLocationWithinBundle;
	}
}