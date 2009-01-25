package com.idega.jbpm.view;

import com.idega.jbpm.bundle.ProcessBundleResources;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/01/25 15:36:31 $ by
 *          $Author: civilis $
 */
public interface ViewResourceResolveStrategy {

	public abstract ViewResource resolve(ProcessBundleResources resources,
			String taskName, String identifier);

	public abstract String getViewResourceTypeHandler();
}