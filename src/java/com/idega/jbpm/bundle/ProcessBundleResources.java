package com.idega.jbpm.bundle;

import java.io.InputStream;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/07/19 20:41:08 $ by $Author: civilis $
 */
public interface ProcessBundleResources {

	public abstract InputStream getResourceIS(String path);
}