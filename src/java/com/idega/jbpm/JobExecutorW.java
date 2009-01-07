package com.idega.jbpm;

import org.jbpm.job.executor.JobExecutor;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2009/01/07 18:31:22 $ by $Author: civilis $
 */
public class JobExecutorW extends JobExecutor {

	private static final long serialVersionUID = 8916905698572123599L;

	@Override
	protected Thread createThread(String threadName) {
		return new JobExecutorThreadW(threadName, this, getJbpmConfiguration(),
				getIdleInterval(), getMaxIdleInterval(), getMaxLockTime(),
				getHistoryMaxSize());
	}
}