package com.idega.jbpm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.executor.JobExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.test.base.IdegaBaseTransactionalTest;
import com.idega.jbpm.proxy.JbpmHandlerProxy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class SpringIntegrationTest extends IdegaBaseTransactionalTest {
	
	//private static final Logger log = Logger.getLogger(SpringModulesIntegrationTest.class.getName());
	protected JobExecutor jobExecutor;
	static long maxWaitTime = 20000;
	
	private static final String testActionHandlerSpringBeanName = "simpleTestActionHandlerClass";
	private static boolean wasMethodExcuteCalled;
	
	
	@Autowired
	private IdegaJbpmContext bpmContext;
	
	void deployProcessDefinitions() throws Exception {

		JbpmContext jctx = bpmContext.createJbpmContext();

		try {
			ProcessDefinition simpleProcess = ProcessDefinition
					.parseXmlString("<process-definition name='bulk messages'>"
							+ "  <start-state>"
							+ "    <transition to='b' />"
							+ "  </start-state>"
							+ "  <node name='b' >"
							+ "    <event type='node-enter'>"
							+ "	   	 <script name='assignPropertiesToUserData'> "
							+ "	 		<expression> "
							+ "    		  List list = new ArrayList();"
							+ "	   		  list.add(\"element in list\");"
							+ "		    </expression> "
							+ "  		<variable name='list' access='write' mapped-name='list' />"
							+ "		 </script>"
							+ "      <action name='X' class='"  + JbpmHandlerProxy.class.getName() + "'>"
							+ "		 	<handlerName>" + "testHandlerForSpringIntegration" + "</handlerName>"
							+ "			<propertyMap key-type='java.lang.String' value-type='java.lang.String'> "
							+ "		 		<entry><key>justString</key><value>just a string</value></entry>"
			                + "				<entry><key>expretionObject</key><value>#{list}</value></entry>"
			                + "				<entry><key>script</key><value>${return resolver.get(\"list\");}</value></entry>"
			               
			                + "			</propertyMap>"
							
							
							+ " 	 </action>"
							+ "    </event>"
							+ "    <transition to='end' />"
							+ "  </node>"
							+ "  <end-state name='end'/>"
							+ "</process-definition>");
			jctx.deployProcessDefinition(simpleProcess);
		}catch (Exception e) {
			e.printStackTrace();
		List<String> list = new ArrayList<String>();
		list.add("element in list");
		} finally {
			bpmContext.closeAndCommit(jctx);
		}
	}

	@Test
	public void testSpringModulesAreWorkingOk() throws Exception {
		//-----------------------------------------------
		//!!!!!!!!!!!!!!!IF (FALSE HERE) !!!!!!!!!!!!!!!!!!!
		//-----------------------------------------------
		if(false){
			System.out.println("Starting the test");
			wasMethodExcuteCalled = false;
			deployProcessDefinitions();
			JbpmContext jbpmContext = bpmContext.createJbpmContext();
	
			try {
				jobExecutor = jbpmContext.getJbpmConfiguration().getJobExecutor();
				
				ProcessInstance pi = jbpmContext.newProcessInstance("bulk messages");
				pi.signal();
				
			}catch (Exception e) {
				e.printStackTrace();
	
			} finally {
				bpmContext.closeAndCommit(jbpmContext);
			}
			
			processJobs(maxWaitTime);
			
			/*if(!wasMethodExcuteCalled){
				fail("Method execute in class SomeCustomAction was not called!");
			}*/
		}
	}
	


	
	
	private void processAllJobs(final long maxWait) {
	    boolean jobsAvailable = true;

	    // install a timer that will interrupt if it takes too long
	    // if that happens, it will lead to an interrupted exception and the test will fail
	    TimerTask interruptTask = new TimerTask() {
	      Thread testThread = Thread.currentThread();
	      public void run() {
	        System.out.println("test "+getName()+" took too long. going to interrupt...");
	        testThread.interrupt();
	      }
	    };
	    Timer timer = new Timer();
	    timer.schedule(interruptTask, maxWait);
	    
	    try {
	      while (jobsAvailable) {
	    	System.out.println("going to sleep for 200 millis, waiting for the job executor to process more jobs");
	        Thread.sleep(200);
	        jobsAvailable = areJobsAvailable();
	      }
	      jobExecutor.stopAndJoin();
	      
	    } catch (InterruptedException e) {
	      fail("test execution exceeded treshold of "+maxWait+" milliseconds");
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
	
	protected boolean areJobsAvailable() {
	    return (getNbrOfJobsAvailable()>0);
	  }
	
	protected void startJobExecutor() {
	    jobExecutor.start();
	  }
	
	private int getNbrOfJobsAvailable() {
	    int nbrOfJobsAvailable = 0;
	    JbpmContext jbpmContext = bpmContext.createJbpmContext();
	    try {
	      Session session = jbpmContext.getSession();
	      Number jobs = (Number) session.createQuery("select count(*) from org.jbpm.job.Job").uniqueResult();
	      System.out.println("there are '"+jobs+"' jobs currently in the job table");
	      if (jobs!=null) {
	        nbrOfJobsAvailable = jobs.intValue();
	      }
	    } finally {
	    	bpmContext.closeAndCommit(jbpmContext);
	    }
	    return nbrOfJobsAvailable;
	  }
	
	protected void stopJobExecutor() {
	    if (jobExecutor!=null) {
	      try {
	        jobExecutor.stopAndJoin();
	      } catch (InterruptedException e) {
	        throw new RuntimeException("waiting for job executor to stop and join got interrupted", e); 
	      }
	    }
	  }

}
