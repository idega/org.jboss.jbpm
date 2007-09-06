package org.jboss.jbpm.tests.generic;


import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.StartState;

import junit.framework.TestCase;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/06 11:17:13 $ by $Author: civilis $
 *
 */
public class JbpmHelloWorldTest extends TestCase {
	
	static ProcessDefinition process = null;
	
	static {
		try {
			process = 
			      ProcessDefinition.parseXmlInputStream(JbpmHelloWorldTest.class.getResourceAsStream("hello-world.xml"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	  // get the nodes for easy asserting
	static StartState start = (StartState)process.getStartState();
	static Node sNode = process.getNode("s");
	static EndState end = (EndState) process.getNode("end");

	ProcessInstance processInstance;

	// the main path of execution
	Token token;

	public void setUp() {
		// create a new process instance for the given process definition
		processInstance = new ProcessInstance(process);

		// the main path of execution is the root token
		token = processInstance.getRootToken();
	}
  
	public void testMainScenario() {
		// after process instance creation, the main path of 
		// execution is positioned in the start state.
		assertSame(start, token.getNode());

		token.signal();

		// after the signal, the main path of execution has 
		// moved to the auction state
		assertSame(sNode, token.getNode());

		token.signal();

		// after the signal, the main path of execution has 
		// moved to the end state and the process has ended
	    assertSame(end, token.getNode());
	    assertTrue(processInstance.hasEnded());
	}
}