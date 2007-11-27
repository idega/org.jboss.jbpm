package com.idega.jbpm.def.impl;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.idega.jbpm.def.ViewFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/27 16:33:26 $ by $Author: civilis $
 */
public abstract class DefaultViewFactoryImpl implements ViewFactory, ApplicationContextAware, ApplicationListener {

	private ApplicationContext ctx;
	
	public void setApplicationContext(ApplicationContext applicationcontext)
			throws BeansException {
		ctx = applicationcontext;
	}

	public void onApplicationEvent(ApplicationEvent applicationevent) {
		
		if(applicationevent instanceof ContextRefreshedEvent) {
			
			//publish xforms factory registration
			ctx.publishEvent(getApplicationEvent());
		}
	}
	
	protected ViewFactoryPluggedInEvent getApplicationEvent() {
		
		return new ViewFactoryPluggedInEvent(this);
	}
}