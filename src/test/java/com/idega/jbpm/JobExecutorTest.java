package com.idega.jbpm;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.test.base.IdegaBaseTransactionalTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JobExecutorTest extends IdegaBaseTransactionalTest {

	static long maxWaitTime = 20000;

	@Autowired
	private IdegaJbpmContext bpmContext;

	void deployProcessDefinitions() throws Exception {

		bpmContext.execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				try {
					ProcessDefinition simpleProcess = ProcessDefinition
							.parseXmlString("<process-definition name='bulkMessages'>"
									+ "  <start-state>"
									+ "    <transition to='b' />"
									+ "  </start-state>"
									+ "  <state name='b' >"
									+ "<event type=\"node-leave\">"
									+ "<script>System.out.println(\"____LEAVING\");</script>"
									+ "</event>"
									+ "<timer duedate='5 seconds' transition='toEnd'></timer>"
									+ "    <transition name='toEnd' to='end' />"
									+ "  </state>"
									+ "  <end-state name='end'/>"
									+ "</process-definition>");
					context.deployProcessDefinition(simpleProcess);
				} catch (Exception e) {
					e.printStackTrace();
					List<String> list = new ArrayList<String>();
					list.add("element in list");
				}

				return null;
			}
		});
	}

	protected void createProcessInstance() {

		bpmContext.execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance pi = context.newProcessInstance("bulkMessages");
				pi.signal();
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testSpringModulesAreWorkingOk() throws Exception {

		deployProcessDefinitions();

		createProcessInstance();

		processJobs(maxWaitTime);
	}

	private void processAllJobs(final long maxWait) {

		boolean jobsAvailable = true;

		// install a timer that will interrupt if it takes too long
		// if that happens, it will lead to an interrupted exception and the
		// test will fail
		TimerTask interruptTask = new TimerTask() {
			Thread testThread = Thread.currentThread();

			public void run() {
				System.out.println("test " + getName()
						+ " took too long. going to interrupt...");
				testThread.interrupt();
			}
		};
		Timer timer = new Timer();
		timer.schedule(interruptTask, maxWait);

		try {
			while (jobsAvailable) {
				System.out
						.println("going to sleep for 200 millis, waiting for the job executor to process more jobs");
				Thread.sleep(200);
				jobsAvailable = areJobsAvailable();
			}
			JbpmConfiguration.getInstance().getJobExecutor().stopAndJoin();

		} catch (InterruptedException e) {
			fail("test execution exceeded treshold of " + maxWait
					+ " milliseconds");
		} finally {
			timer.cancel();
		}
	}

	protected void processJobs(long maxWait) {
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		startJobExecutor();
		try {
			processAllJobs(maxWait);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			stopJobExecutor();
		}
	}

	@Transactional
	protected boolean areJobsAvailable() {
		return (getNbrOfJobsAvailable() > 0);
	}

	protected void startJobExecutor() {
		JbpmConfiguration.getInstance().getJobExecutor().start();
	}

	private int getNbrOfJobsAvailable() {
		Integer nbrOfJobsAvailable = bpmContext.execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				Session session = context.getSession();
				Number jobs = (Number) session.createQuery(
						"select count(*) from org.jbpm.job.Job").uniqueResult();
				System.out.println("there are '" + jobs
						+ "' jobs currently in the job table");
				Integer nbrOfJobsAvailable = 0;
				if (jobs != null) {
					nbrOfJobsAvailable = jobs.intValue();
				}
				return nbrOfJobsAvailable;
			}
		});

		return nbrOfJobsAvailable;
	}

	protected void stopJobExecutor() {
		if (JbpmConfiguration.getInstance().getJobExecutor() != null) {
			try {
				JbpmConfiguration.getInstance().getJobExecutor().stopAndJoin();
			} catch (InterruptedException e) {
				throw new RuntimeException(
						"waiting for job executor to stop and join got interrupted",
						e);
			}
		}
	}

}
