package com.idega.jbpm.view;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.bundle.ProcessBundleResources;
import com.idega.util.StringUtil;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/01/25 15:36:31 $ by
 *          $Author: civilis $
 */
@Service
@Scope("singleton")
public class ViewResourceResolveStrategyDefaultImpl implements
		ViewResourceResolveStrategy {

	public ViewResource resolve(ProcessBundleResources resources,
			String taskName, String identifier) {

		String pathWithinBundle = identifier;

		if (StringUtil.isEmpty(pathWithinBundle)) {
			throw new IllegalArgumentException(
					"Tried to resolve xform view resource, but no identifier specified");
		}

		ViewResourceByViewIdentifierDefaultImpl resource = new ViewResourceByViewIdentifierDefaultImpl();
		resource.setTaskName(taskName);
		resource.setViewResourceIdentifier(identifier);
		return resource;
	}

	public String getViewResourceTypeHandler() {
		return "identifier";
	}
}