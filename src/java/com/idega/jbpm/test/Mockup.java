package com.idega.jbpm.test;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 *
 * Last modified: $Date: 2008/06/15 11:58:54 $ by $Author: civilis $
 *
 */
public class Mockup {
	
	public static final String plaintiffVarName = "string:plaintiffFirstName";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		try {
			
			JbpmConfiguration jbpmConfiguration = 
		           JbpmConfiguration.getInstance("com/idega/jbpm/test/jbpm.test.cfg.xml");
			
			JbpmContext ctx = jbpmConfiguration.createJbpmContext();
			
			ProcessDefinition pd = ProcessDefinition.parseXmlInputStream(new FileInputStream("/Users/civilis/dev/workspace/eplatform-4-bpm/org.jboss.jbpm/src/java/com/idega/jbpm/test/processdefinition.xml"));
			//ProcessDefinition invitationPD = ProcessDefinition.parseXmlInputStream(new FileInputStream("/Users/civilis/dev/workspace/eplatform-4-bpm/org.jboss.jbpm/src/java/com/idega/jbpm/test/participantInvitation/processdefinition.xml"));
			
			try {
				
				ctx.deployProcessDefinition(pd);
				System.out.println("__deployed");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			ProcessInstance pi = pd.createProcessInstance();
			
			printTokens(pi);
			printCompletedTaskInstances(pi);
			printIncompletedTaskInstances(pi);
			
			pi.signal();
			System.out.println(">Signaling");
			
			//pi.getTaskMgmtInstance().getTaskMgmtDefinition().addTask(task)

			printIncompletedTaskInstances(pi);
			
			if(true)
				return;

			System.out.println(">Submiting");
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> taskInstances = pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken());
			
			TaskInstance t1 = taskInstances.iterator().next();
			
			Map<String, Object> vars = new HashMap<String, Object>();
			vars.put(plaintiffVarName, "name1");
			submitInstances(taskInstances, vars);

			printTokens(pi);
			printCompletedTaskInstances(pi);
			printIncompletedTaskInstances(pi);
			
			System.out.println(">Submiting again .......");
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> taskInstances2 = pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken());
			TaskInstance t2 = taskInstances2.iterator().next();
			vars = new HashMap<String, Object>();
			vars.put(plaintiffVarName, "name2");
			
			if(false) {
			
				submitInstances(taskInstances2, vars);
				
				System.out.println("t1var:"+t1.getVariableLocally(plaintiffVarName));
				System.out.println("t2var:"+t2.getVariableLocally(plaintiffVarName));
				System.out.println("pi:"+pi.getContextInstance().getVariable(plaintiffVarName));
			}
			
			System.out.println("ending task instance on transition 'addComment'");
			t2.end("addComment");
			
			printCompletedTaskInstances(pi);
			printIncompletedTaskInstances(pi);
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> tis = pi.getTaskMgmtInstance().getTaskInstances();
			
			for (TaskInstance taskInstance : tis) {

				if(!taskInstance.hasEnded() && taskInstance.getName().equals("Submit comment")) {
					taskInstance.end("submitComment");
					System.out.println(">>>>ended: "+taskInstance.getToken());
					break;
				}
			}
			
			printCompletedTaskInstances(pi);
			printIncompletedTaskInstances(pi);
			
			tis = pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken());
			
			for (TaskInstance taskInstance : tis) {
				
				if(taskInstance.getName().equals("Case Overview")) {
					System.out.println("ending case overview last");
					//taskInstance.getTask().setas
					taskInstance.end("addComment");
//					taskInstance.getSwimlaneInstance().getSwimlane()
					break;
				}
			}
			
			printCompletedTaskInstances(pi);
			printIncompletedTaskInstances(pi);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printTokens(ProcessInstance pi) {
	
		System.out.println("----------");
		System.out.println("tokens: "+pi.findAllTokens());
		System.out.println("----------");
	}
	public static void printIncompletedTaskInstances(ProcessInstance pi) {
		
		System.out.println("----------");
		System.out.println(">>Incompleted task instances");
		
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = pi.getTaskMgmtInstance().getTaskInstances();
		
		if(taskInstances == null)
			System.out.println(">>>Empty");
		else
			for (TaskInstance taskInstance : taskInstances) {
				
				if(!taskInstance.hasEnded()) {
					System.out.println(">>>"+taskInstance+", token: "+taskInstance.getToken());
				}
			}
		System.out.println("----------");
	}
	
	public static void printCompletedTaskInstances(ProcessInstance pi) {
		
		System.out.println("----------");
		System.out.println(">>Completed task instances");
		
		System.out.println("tokens: "+pi.findAllTokens());
		
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = pi.getTaskMgmtInstance().getTaskInstances();
		
		if(taskInstances == null)
			System.out.println(">>>Empty");
		else
			for (TaskInstance taskInstance : taskInstances) {
				if(taskInstance.hasEnded()) {
					System.out.println(">>>"+taskInstance+", token: "+taskInstance.getToken());
				}
			}
		System.out.println("----------");
	}
	
	public static void submitInstances(Collection<TaskInstance> taskInstances, Map<String, Object> variables) {
		
		for (TaskInstance ti : taskInstances) {
			
			///ti.getTask().getTaskController().setTaskControllerDelegation(taskControllerDelegation)
			//ti.getTask().getTaskController().
			System.out.println("submiting: "+ti);
			ti.setVariables(variables);
			ti.getTask().getTaskController().submitParameters(ti);
			ti.end();
		}
	}
	
	public void testStuff() {
		
		System.out.println("testing stuff");
	}
}