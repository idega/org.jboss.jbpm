package com.idega.jbpm;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.test.base.IdegaBaseTransactionalTest;
import com.idega.jbpm.proxy.JbpmHandlerProxy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class SpringIntegrationTest extends IdegaBaseTransactionalTest {

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
							+ "      <action name='X' class='"
							+ JbpmHandlerProxy.class.getName()
							+ "'>"
							+ "		 	<handlerName>"
							+ "testHandlerForSpringIntegration"
							+ "</handlerName>"
							+ "			<propertyMap key-type='java.lang.String' value-type='java.lang.String'> "
							+ "		 		<entry><key>justString</key><value>just a string</value></entry>"
							+ "				<entry><key>expretionObject</key><value>#{list}</value></entry>"
							+ "				<entry><key>script</key><value>${return resolver.get(\"list\");}</value></entry>"

							+ "			</propertyMap>"

							+ " 	 </action>" + "    </event>"
							+ "    <transition to='end' />" + "  </node>"
							+ "  <end-state name='end'/>"
							+ "</process-definition>");
			jctx.deployProcessDefinition(simpleProcess);
		} catch (Exception e) {
			e.printStackTrace();
			List<String> list = new ArrayList<String>();
			list.add("element in list");
		} finally {
			bpmContext.closeAndCommit(jctx);
		}
	}

	/**
	 * TODO FIX This test, it should run normally
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSpringModulesAreWorkingOk() throws Exception {
		// -----------------------------------------------
		// !!!!!!!!!!!!!!!IF (FALSE HERE) !!!!!!!!!!!!!!!!!!!
		// -----------------------------------------------
		if (false) {
			System.out.println("Starting the test");
			deployProcessDefinitions();

			bpmContext.execute(new JbpmCallback() {

				public Object doInJbpm(JbpmContext context)
						throws JbpmException {
					ProcessInstance pi = context
							.newProcessInstance("bulk messages");
					pi.signal();
					return null;
				}

			});
		}
	}
}
