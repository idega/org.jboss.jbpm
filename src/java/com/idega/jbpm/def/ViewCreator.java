package com.idega.jbpm.def;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.idega.jbpm.def.impl.ViewFactoryPluggedInEvent;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/27 16:33:26 $ by $Author: civilis $
 */
public class ViewCreator implements ApplicationListener {
	
	private Map<String, ViewFactory> viewFactories;
	
	public ViewCreator() {
		viewFactories = new HashMap<String, ViewFactory>();
	}

	public void onApplicationEvent(ApplicationEvent applicationevent) {
		
		if(applicationevent instanceof ViewFactoryPluggedInEvent) {
			
			ViewFactory viewFactory = (ViewFactory)applicationevent.getSource();
			String viewType = viewFactory.getViewType();
			
			if(viewType != null && !CoreConstants.EMPTY.equals(viewType)) {
				
				viewFactories.put(viewType, viewFactory);
			} else {
				Logger.getLogger(ViewCreator.class.getName()).log(Level.WARNING, "View factory got, which didn't return view type. View factory class: "+viewFactory.getClass().getName());
			}
		}
	}
	
	public ViewFactory getViewFactory(String viewType) {
		
		return viewFactories.get(viewType);
	}
}