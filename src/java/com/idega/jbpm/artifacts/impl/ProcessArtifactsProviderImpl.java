package com.idega.jbpm.artifacts.impl;

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

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.artifacts.ProcessArtifactsProvider;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;

/**
 * @deprecated all this stuff should be in any of the wrapper classes. E.g. ProcessInstaceW
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 *
 * Last modified: $Date: 2008/11/26 13:16:13 $ by $Author: civilis $
 */
@Deprecated
@Scope("singleton")
@Service
public class ProcessArtifactsProviderImpl implements ProcessArtifactsProvider {
	
	private BPMContext idegaJbpmContext;
	private VariablesHandler variablesHandler;
	private BPMFactory bpmFactory;
	
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
	
	public List<BinaryVariable> getTaskAttachments(Long taskInstanceId) {
		
		if(taskInstanceId == null)
			return null;
	
		List<BinaryVariable> binaryVariables = getVariablesHandler().resolveBinaryVariables(taskInstanceId);
		
		return binaryVariables;
	}
	
	public String getCaseIdentifier(Long processInstanceId) {
		if (processInstanceId == null) {
			return null;
		}
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		try {
			ProcessInstance pi = ctx.getProcessInstance(processInstanceId);
			Object o = pi.getContextInstance().getVariable(ProcessArtifactsProvider.CASE_IDENTIFIER);
			return o == null ? null : String.valueOf(o);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
		
		return null;
	}
	
	public String getProcessDescription(Long processInstanceId) {
		if (processInstanceId == null) {
			return null;
		}
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		try {
			ProcessInstance pi = ctx.getProcessInstance(processInstanceId);
			Object o = pi.getContextInstance().getVariable(ProcessArtifactsProvider.CASE_DESCRIPTION);
			return o == null ? null : String.valueOf(o);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
		
		return null;
	}
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	@Autowired
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}