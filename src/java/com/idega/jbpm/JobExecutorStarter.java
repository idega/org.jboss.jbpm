package com.idega.jbpm;

import java.util.logging.Logger;

import org.jbpm.JbpmConfiguration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWMainApplicationShutdownEvent;
import com.idega.idegaweb.IWMainApplicationStartedEvent;

/**
 *
 * 
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version $Revision: 1.3 $, $Date: 2008/12/19 07:45:23 $ by $Author: anton $
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JobExecutorStarter implements ApplicationListener {

	private static final Logger LOGGER = Logger.getLogger(JobExecutorStarter.class.getName());
	
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if(applicationEvent instanceof IWMainApplicationStartedEvent) {
			LOGGER.info("Starting Job executor");
			JbpmConfiguration.getInstance().startJobExecutor();
			JbpmConfiguration.getInstance().getJobExecutor().setIdleInterval(18000);
		} else if(applicationEvent instanceof IWMainApplicationShutdownEvent) {
			LOGGER.info("Stopping Job executor");
			JbpmConfiguration.getInstance().getJobExecutor().stop();
		}
	}
	
	public JbpmConfiguration getJbpmConfiguration() {
		return JbpmConfiguration.getInstance();
	}

}
