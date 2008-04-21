package com.idega.jbpm.exe.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.exe.ProcessArtifactsProvider;
import com.idega.jbpm.exe.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/21 05:13:45 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class ProcessArtifactsProviderImpl implements ProcessArtifactsProvider {
	
	private IdegaJbpmContext idegaJbpmContext;
	private VariablesHandler variablesHandler;
	
	public static final String email_fetch_process_name = "fetchEmails";
	
	public Collection<TaskInstance> getAttachedEmailsTaskInstances(Long processInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			
			@SuppressWarnings("unchecked")
			List<Token> tkns = processInstance.findAllTokens();
			
			for (Token tkn : tkns) {
				
				ProcessInstance subPI = tkn.getSubProcessInstance();
				
				if(subPI != null && email_fetch_process_name.equals(subPI.getProcessDefinition().getName())) {
					
					@SuppressWarnings("unchecked")
					Collection<TaskInstance> taskInstances = subPI.getTaskMgmtInstance().getTaskInstances();

					for (Iterator<TaskInstance> iterator  = taskInstances.iterator(); iterator.hasNext();) {
						TaskInstance taskInstance = iterator.next();
						
						if(!taskInstance.hasEnded())
							iterator.remove();
					}
					
					return taskInstances;
				}
			}
			
			return null;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Collection<TaskInstance> getSubmittedTaskInstances(Long processInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
			
			for (Iterator<TaskInstance> iterator  = taskInstances.iterator(); iterator.hasNext();) {
				TaskInstance taskInstance = iterator.next();
				
				if(!taskInstance.hasEnded())
					iterator.remove();
			}
			
			return taskInstances;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public List<BinaryVariable> getTaskAttachments(Long taskInstanceId) {
		
		if(taskInstanceId == null)
			return null;
	
		List<BinaryVariable> binaryVariables = getVariablesHandler().resolveBinaryVariables(taskInstanceId);
		
		return binaryVariables;
	}
	
	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	@Autowired
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
}