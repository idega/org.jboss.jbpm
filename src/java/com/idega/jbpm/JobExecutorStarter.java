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
 * @version $Revision: 1.7 $, $Date: 2009/01/29 16:10:33 $ by $Author: anton $
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JobExecutorStarter implements ApplicationListener<ApplicationEvent> {

	private static final Logger LOGGER = Logger.getLogger(JobExecutorStarter.class.getName());

	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if (applicationEvent instanceof IWMainApplicationStartedEvent) {
			JbpmConfiguration jbpmConfig = JbpmConfiguration.getInstance();
			LOGGER.info("Obtained JBPM configuration: " + jbpmConfig);

			LOGGER.info("Starting Job executor");
			jbpmConfig.getJobExecutor().setIdleInterval(3000);
			jbpmConfig.startJobExecutor();
		} else if (applicationEvent instanceof IWMainApplicationShutdownEvent) {
			LOGGER.info("Stopping Job executor");
			JbpmConfiguration.getInstance().getJobExecutor().stop();
		}
	}
}
