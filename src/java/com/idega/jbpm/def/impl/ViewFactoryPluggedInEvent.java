package com.idega.jbpm.def.impl;

import org.springframework.context.ApplicationEvent;

import com.idega.jbpm.def.ViewFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/01/25 15:24:26 $ by $Author: civilis $
 */
public class ViewFactoryPluggedInEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1577815015990328896L;
	
	private ViewFactory viewFactory;

	public ViewFactoryPluggedInEvent(Object source) {
        super(source);
        
        viewFactory = (ViewFactory)source;
    }

	public ViewFactory getViewFactory() {
		return viewFactory;
	}
}