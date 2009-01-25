package com.idega.jbpm.bundle;

import java.io.InputStream;

import org.w3c.dom.Document;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 *          Last modified: $Date: 2009/01/25 15:36:31 $ by $Author: civilis $
 */
public interface ProcessBundleResources {

	public abstract InputStream getResourceIS(String path);

	public abstract Document getConfiguration();
}