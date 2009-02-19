package com.idega.jbpm.events;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.instantiation.Delegation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.BaseBPMTest;
import com.idega.jbpm.JbpmCallback;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2009/02/19 13:06:40 $ by $Author: civilis $
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public final class TaskInstanceCreateEventTest extends BaseBPMTest {

	void deployProcessDefinitions() throws Exception {

		getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				try {
					ProcessDefinition taskInstanceCreateEventTestProcess = ProcessDefinition
							.parseXmlString("<process-definition name='TaskInstanceCreateEventTest'>"
									+ "<event type='task-create'>"
									+ "<script>System.out.println(\"____Task instance created event retrieved in the process definition\");</script>"
									+ "</event>"
									+ "<event type='task-create'>"
									+ "<script>System.out.println(\"____oOOOOOOOOOOOOOOOOO\");</script>"
									+ "</event>"
									+ "  <start-state>"
									+ "    <transition to='TaskNode1' />"
									+ "  </start-state>"
									+ "  <task-node name='TaskNode1'>"
									+ "<task name='Task1' />"
									+ "<event type='task-create'>"
									+ "<script>System.out.println(\"____Task instance created\");</script>"
									+ "</event>"
									+ "    	 <transition to='end' />"
									+ "  </task-node>"
									+ "  <end-state name='end'/>"
									+ "</process-definition>");

					Event event;
					String eventType = Event.EVENTTYPE_TASK_CREATE;

					if (!taskInstanceCreateEventTestProcess.hasEvent(eventType)) {

						System.out.println("hasn't event");

						event = taskInstanceCreateEventTestProcess
								.addEvent(new Event(eventType));
					} else {
						System.out.println("has event");
						event = taskInstanceCreateEventTestProcess
								.getEvent(eventType);
					}

					Delegation del = new Delegation(
							TaskCreateEventHandler.class.getName());
					Action act = new Action(del);
					act.setName("TaskCreateEventHandler");
					// taskInstanceCreateEventTestProcess.addEvent(new
					// Event(Event.EVENTTYPE_TASK_START));
					// act.setEvent(event);
					event.addAction(act);
					// taskInstanceCreateEventTestProcess.addAction(act);

					context
							.deployProcessDefinition(taskInstanceCreateEventTestProcess);

				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}

		});
	}

	@Test
	public void testTaskInstanceCreateHandler() throws Exception {

		deployProcessDefinitions();

		getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				try {
					ProcessInstance pi = context
							.newProcessInstance("TaskInstanceCreateEventTest");
					pi.signal();

				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}
		});
	}
}