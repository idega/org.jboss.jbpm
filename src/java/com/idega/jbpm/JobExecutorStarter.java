package com.idega.jbpm;

import org.jbpm.JbpmConfiguration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWMainApplicationStartedEvent;

/**
 *
 * 
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0 
 *
 * Last modified: Dec 4, 2008 by Author: Anton 
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JobExecutorStarter implements ApplicationListener {

	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if(applicationEvent instanceof IWMainApplicationStartedEvent) {
			JbpmConfiguration.getInstance().startJobExecutor();
		}
	}
	
	public JbpmConfiguration getJbpmConfiguration() {
		return JbpmConfiguration.getInstance();
	}

}
