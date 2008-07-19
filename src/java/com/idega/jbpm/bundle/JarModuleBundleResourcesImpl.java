package com.idega.jbpm.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.idegaweb.IWBundle;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/07/19 20:41:08 $ by $Author: civilis $
 */
public class JarModuleBundleResourcesImpl implements ProcessBundleResources {

	private IWBundle bundle;
	private String bundlePropertiesLocationWithinBundle;
	
	public void close() {
		
	}
	
	public InputStream getResourceIS(String path) {
		
		String templateBundleLocationWithinBundle = getBundlePropertiesLocationWithinBundle().substring(0, getBundlePropertiesLocationWithinBundle().lastIndexOf(CoreConstants.SLASH)+1);

		if (templateBundleLocationWithinBundle == null)
			throw new IllegalStateException(
					"No templateBundleLocationWithinBundle set");
		
		path = templateBundleLocationWithinBundle + path;
		
		try {
			InputStream is = getBundle().getResourceInputStream(path);
			
			return is;
			
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Resource not found by the path="+path+" at bundle="+getBundle().getBundleIdentifier());
		}

		return null;
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
}