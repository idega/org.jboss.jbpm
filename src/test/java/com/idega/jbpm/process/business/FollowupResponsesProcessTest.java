package com.idega.jbpm.process.business;

import java.io.InputStream;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.test.base.IdegaBaseTransactionalTest;
import com.idega.jbpm.IdegaJbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/07/01 19:39:40 $ by $Author: civilis $
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public final class FollowupResponsesProcessTest extends IdegaBaseTransactionalTest {

	@Autowired
	private IdegaJbpmContext bpmContext;
	
	void deployProcessDefinitions() throws Exception {

		JbpmContext jctx = bpmContext.createJbpmContext();
		InputStream is = null;
		
		try {
			ClassPathResource cpr = new ClassPathResource("/com/idega/jbpm/process/business/definition/followupresponse/processdefinition.xml", getClass());
			is = cpr.getInputStream();
			
			ProcessDefinition followupProcess = ProcessDefinition.parseXmlInputStream(is);
			jctx.deployProcessDefinition(followupProcess);
			
			ProcessDefinition superProcess = ProcessDefinition.parseXmlString(
						      "<process-definition name='super'>" +
						      "		<task name='Task for subprocess'>" +
						      "			<controller>" +
						      "				<variable name='string:ownerKennitala' access='read,write,required'></variable>"+
						      "			</controller>" +
						      /*
						      "<assignment class='com.idega.jbpm.identity.JSONAssignmentHandler'>"+
						      "<expression>" +
						      "{taskAssignment: {roles: {role: [" +
						      "{roleName: \"bpm_handler\", accesses: {access: [read]}}," +
						      "{roleName: \"bpm_owner\", accesses: {access: [read, write]}, scope: PI, assignIdentities: {string: [\"current_user\"]}}," +
						      "{roleName: \"bpm_invited\", accesses: {access: [read]}, scope: PI}" +
						      "]} }}" +
						      "</expression>" +
						      "</assignment>" +
						      */
						      "		</task>" +
						      "		<task name='Task for subprocess 2'>" +
						      "			<controller>" +
						      "				<variable name='string:ownerKennitala' access='read,write,required'></variable>"+
						      "			</controller>" +
						      "		</task>" +
						      "  <start-state>" +
						      "    <transition name='with subprocess' to='subprocess' />" +
						      "  </start-state>" +
						      "  <process-state name='subprocess'>" +
						      "    <sub-process name='followupResponses' binding='late' />" +
						      
						      "<variable name='tasksToSubprocess' access='read' mapped-name='followupTasks' />" +
						      "	<event type='node-enter'>" +
						      "		<script>"+
						      "			<expression>" +
						      "				tasks = new ArrayList(2);" +
						      "				task = executionContext.getTaskMgmtInstance().getTaskMgmtDefinition().getTask(\"Task for subprocess\");" +
						      "				bean = new com.idega.jbpm.invitation.AssignTasksForRolesUsersBean();" +
						      "				bean.setTask(task);" +
						      "				bean.setToken(token);" +
						      "				bean.setRoles(new String[] {\"bpm_handler\"});" +
						      "				tasks.add(bean);" +
						      "				task = executionContext.getTaskMgmtInstance().getTaskMgmtDefinition().getTask(\"Task for subprocess 2\");" +
						      "				bean = new com.idega.jbpm.invitation.AssignTasksForRolesUsersBean();" +
						      "				bean.setTask(task);" +
						      "				bean.setRoles(new String[] {\"bpm_owner\"});" +
						      "				bean.setToken(token);" +
						      "				tasks.add(bean);" +
						      "			</expression>" +
						      "			<variable name='tasksToSubprocess' access='write' mapped-name='tasks' />" +
						      "		</script>"+
						      "	</event>" +
						      "    <transition name='toEnd' to='end' />" +
						      "  </process-state>" +
						      "<end-state name='end'></end-state>" +
						      "</process-definition>"
						    );
			jctx.deployProcessDefinition(superProcess);
			
			
		} finally {
			bpmContext.closeAndCommit(jctx);
			
			if(is != null)
				is.close();
		}
	}
	
	@Test
	public void testFollowup() throws Exception {

		deployProcessDefinitions();
		
		JbpmContext jbpmContext = bpmContext.createJbpmContext();
		
		try {
			ProcessInstance pi = jbpmContext.newProcessInstanceForUpdate("super");
			System.out.println("superprocess ID="+pi.getId());
		    pi.signal("with subprocess");

		    
			
		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}
	    
	    		
	}
}