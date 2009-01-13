package com.idega.jbpm;

import java.util.Calendar;
import java.util.Date;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class CalendarBeanTest extends BaseBPMTest {

	void deployProcessDefinitions() throws Exception {

		try {
			ProcessDefinition simpleProcess = ProcessDefinition
					.parseXmlString("<process-definition name='bulk messages'>"
							+ "  <start-state>"
							+ "    <transition to='b' />"
							+ "  </start-state>"
							+ "  <node name='b' >"
							+ "    <event type='node-enter'>"
							+ "	   	 <script name='assignVariableUsingCalendarBean'> "
							+ "	 		<expression> "
							+ "				Date currentDate = new Date();"
							+ "				Date currentPlusOne = com.idega.jbpm.proxy.JbpmHandlerProxy.bean(\"calendarOps\").add(currentDate, \"1 hour\");"
							+ "		    </expression> "
							+ "  		<variable name='currentPlusOne' access='write' />"
							+ "  		<variable name='currentDate' access='write' />"
							+ "		 </script>" + "    </event>"
							+ "    <transition to='end' />" + "  </node>"
							+ "  <end-state name='end'/>"
							+ "</process-definition>");

			deployProcessDefinition(simpleProcess);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSpringModulesAreWorkingOk() throws Exception {

		deployProcessDefinitions();

		ProcessInstance pi = getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				ProcessInstance pi = context
						.newProcessInstance("bulk messages");
				pi.signal();
				return pi;
			}
		});

		Date currentDate = (Date) pi.getContextInstance().getVariable(
				"currentDate");
		Date currentPlusOne = (Date) pi.getContextInstance().getVariable(
				"currentPlusOne");

		Calendar cal = Calendar.getInstance();
		cal.setTime(currentDate);
		cal.add(Calendar.HOUR, 1);
		Date addedHour = cal.getTime();

		assertEquals(currentPlusOne, addedHour);
	}
}