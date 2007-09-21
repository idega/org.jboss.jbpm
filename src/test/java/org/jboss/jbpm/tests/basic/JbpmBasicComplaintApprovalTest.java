package org.jboss.jbpm.tests.basic;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jbpm.context.def.VariableAccess;
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
 * Last modified: $Date: 2007/09/21 11:29:06 $ by $Author: civilis $
 *
 */
public class JbpmBasicComplaintApprovalTest extends TestCase {
	
	static ProcessDefinition process = null;
	
	static {
		try {
			process = 
			      ProcessDefinition.parseXmlInputStream(JbpmBasicComplaintApprovalTest.class.getResourceAsStream("basicComplaintApproval.xml"));
			
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

		
		
		
		
		
		
		
		
		assertSame(start, token.getNode());

		System.out.println("unfinished tasks: "+processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken()));
		
		processInstance.signal();
		
		if(true)
			return;
		
		if(processInstance.getRootToken().getNode() instanceof TaskNode) {
			
			System.out.println("unfinished tasks: "+processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken()));
			System.out.println("tinstances: "+processInstance.getTaskMgmtInstance().getTaskInstances());
			TaskInstance ti = (TaskInstance)processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken()).iterator().next();
			ti.start();
			System.out.println("started task");
			ti.end();
			
			System.out.println("unfinished tasks after finishing task1: "+processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken()));
			System.out.println("tinstances: "+processInstance.getTaskMgmtInstance().getTaskInstances());
			System.out.println("and now the node: "+processInstance.getRootToken().getNode());
			
			ti = (TaskInstance)processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken()).iterator().next();
			System.out.println("variables locally: "+ti.getVariablesLocally());
			//ti.getTask().getTaskController().
			System.out.println("variable access: "+((VariableAccess)ti.getTask().getTaskController().getVariableAccesses().iterator().next()).isRequired());
			
			
			ti.start();
			System.out.println("variables instances: "+ti.getVariableInstances());
			
			Map<String, Object> variables = new HashMap<String, Object>();
			variables.put("lala", "1");
			ti.setVariables(variables);
			//ti.deleteVariable("xx");
			System.out.println("variables after deletion: "+ti.getVariables());
			System.out.println("variables instances: "+ti.getVariableInstances());
			ti.end();
		}
			
//		processInstance.getRootToken().signal();

	    assertSame(end, token.getNode());
	    assertTrue(processInstance.hasEnded());
	}
}