package com.idega.jbpm;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.job.Job;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.job.executor.JobExecutorThread;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.util.ListUtil;
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

	public JobExecutorThreadW(String name, JobExecutor jobExecutor, JbpmConfiguration jbpmConfiguration, int idleInterval, int maxIdleInterval,
			long maxLockTime, int maxHistory) {
		super(name, jobExecutor, jbpmConfiguration, idleInterval, maxIdleInterval, maxLockTime, maxHistory);
	}

	@Override
	protected Collection<Job> acquireJobs() {
		return getBpmContext().execute(new JbpmCallback<Collection<Job>>() {
			@Override
			public Collection<Job> doInJbpm(JbpmContext context) throws JbpmException {
				return acquireJobsSuper(context);
			}
		});
	}

	private Collection<Job> acquireJobsSuper(JbpmContext context) {
		@SuppressWarnings("unchecked")
		Collection<Job> jobs = super.acquireJobs();
		if (ListUtil.isEmpty(jobs)) {
			return Collections.emptyList();
		}
		return jobs;
	}

	protected Collection<Job> acquireJobsSuper() {
		return getBpmContext().execute(new JbpmCallback<Collection<Job>>() {

			@Override
			public Collection<Job> doInJbpm(JbpmContext context) throws JbpmException {
				return acquireJobsSuper(context);
			}
		});
	}

	@Override
	protected void executeJob(final Job job) {
		getBpmContext().execute(new JbpmCallback<Void>() {
			@Override
			public Void doInJbpm(JbpmContext context) throws JbpmException {
				executeJobSuper(context, job);
				return null;
			}
		});
	}

	private void executeJobSuper(JbpmContext context, Job job) {
		super.executeJob(job);
	}

	@Override
	protected Date getNextDueDate() {
		return getBpmContext().execute(new JbpmCallback<Date>() {
			@Override
			public Date doInJbpm(JbpmContext context) throws JbpmException {
				return getNextDueDateSuper();
			}
		});
	}

	protected Date getNextDueDateSuper() {
		return super.getNextDueDate();
	}

	BPMContext getBpmContext() {
		if (bpmContext == null)
			ELUtil.getInstance().autowire(this);
		return bpmContext;
	}
}