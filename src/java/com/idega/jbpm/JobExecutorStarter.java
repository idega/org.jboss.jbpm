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
 * @version $Revision: 1.6 $, $Date: 2009/01/07 18:31:22 $ by $Author: civilis $
 * 
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JobExecutorStarter implements ApplicationListener {

	private static final Logger LOGGER = Logger
			.getLogger(JobExecutorStarter.class.getName());

	public void onApplicationEvent(ApplicationEvent applicationEvent) {

		if (applicationEvent instanceof IWMainApplicationStartedEvent) {
			LOGGER.info("Starting Job executor");
//			JbpmConfiguration.getInstance().getJobExecutor().setIdleInterval(
//					3000);
			JbpmConfiguration.getInstance().startJobExecutor();
		} else if (applicationEvent instanceof IWMainApplicationShutdownEvent) {
			LOGGER.info("Stopping Job executor");
			JbpmConfiguration.getInstance().getJobExecutor().stop();
		}
	}
}
