package com.idega.jbpm;

import java.util.Collection;
import java.util.Date;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.job.executor.JobExecutorThread;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.util.expression.ELUtil;

/**
 * Jbpm JobExecutor implementation wrapper which adds transaction handling to
 * transactional method calls
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2009/01/07 18:31:22 $ by $Author: civilis $
 */
public class JobExecutorThreadW extends JobExecutorThread {

	@Autowired
	private BPMContext bpmContext;

	public JobExecutorThreadW(String name, JobExecutor jobExecutor,
			JbpmConfiguration jbpmConfiguration, int idleInterval,
			int maxIdleInterval, long maxLockTime, int maxHistory) {
		super(name, jobExecutor, jbpmConfiguration, idleInterval,
				maxIdleInterval, maxLockTime, maxHistory);

		ELUtil.getInstance().autowire(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Collection acquireJobs() {

		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				return acquireJobsSuper();
			}
		});
	}

	@SuppressWarnings("unchecked")
	protected Collection acquireJobsSuper() {

		return super.acquireJobs();
	}

	@Override
	protected void executeJob(final Job job) {

		getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				executeJobSuper(job);
				return null;
			}
		});
	}

	public void executeJobSuper(Job job) {

		super.executeJob(job);
	}

	@Override
	protected Date getNextDueDate() {

		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				return getNextDueDateSuper();
			}
		});
	}

	protected Date getNextDueDateSuper() {
		return super.getNextDueDate();
	}

	BPMContext getBpmContext() {
		return bpmContext;
	}
}