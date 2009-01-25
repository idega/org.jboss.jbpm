package com.idega.jbpm.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
public class ViewResourceFactoryImpl {

	private Map<String, ViewResourceResolveStrategy> viewResourceResolveStrategies;

	public ViewResource getViewResource(String resourceType,
			String resourceIdentifier, String taskName,
			ProcessBundleResources resources) {

		if (StringUtil.isEmpty(resourceType))
			resourceType = "identifier"; // TODO ref constant of
											// ViewResourceResolveStrategyDefaultImpl.getViewResourceTypeHandler
											// here

		return getViewResourceResolveStrategy(resourceType).resolve(resources,
				taskName, resourceIdentifier);
	}

	public ViewResourceResolveStrategy getViewResourceResolveStrategy(
			String resourceType) {

		if (viewResourceResolveStrategies == null)
			throw new IllegalStateException(
					"Tried to get view resource by resource type = "
							+ resourceType
							+ ", but no resource resolve strategies defined");

		if (!viewResourceResolveStrategies.containsKey(resourceType)) {
			throw new IllegalArgumentException(
					"No resource resolve strategies found by resource type: "
							+ resourceType);
		}

		return viewResourceResolveStrategies.get(resourceType);
	}

	@Autowired(required = false)
	public void setViewResourceResolveStrategies(
			List<ViewResourceResolveStrategy> strategies) {

		viewResourceResolveStrategies = new HashMap<String, ViewResourceResolveStrategy>(
				strategies.size());

		for (ViewResourceResolveStrategy strat : strategies) {

			viewResourceResolveStrategies.put(strat
					.getViewResourceTypeHandler(), strat);
		}
	}
}