package org.jboss.jbpm.tests.basic;


import java.util.Iterator;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.StartState;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

import junit.framework.TestCase;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/13 11:21:22 $ by $Author: civilis $
 *
 */
public class JbpmOneTaskTest extends TestCase {
	
	static ProcessDefinition process = null;
	
	static {
		try {
			process = 
			      ProcessDefinition.parseXmlInputStream(JbpmOneTaskTest.class.getResourceAsStream("oneTask.xml"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	  // get the nodes for easy asserting
	static StartState start = (StartState)process.getStartState();
	static TaskNode taskNode = (TaskNode)process.getNode("task");
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
		
		assertSame(taskNode, token.getNode());
		
		processInstance.getContextInstance().createVariable("somevar", "theval", token);
		
		System.out.println("fst time: "+processInstance.getContextInstance().getVariable("somevar", token));

		token.signal();

		// after the signal, the main path of execution has 
		// moved to the end state and the process has ended
	    assertSame(end, token.getNode());
	    assertTrue(processInstance.hasEnded());
	}
}