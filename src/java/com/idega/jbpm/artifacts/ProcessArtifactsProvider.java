package com.idega.jbpm.artifacts;

import java.util.Collection;
import java.util.List;

import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/06/15 16:32:30 $ by $Author: civilis $
 */
public interface ProcessArtifactsProvider {
	
	public static final String CASE_IDENTIFIER = "string:caseIdentifier";

	public abstract Collection<TaskInstanceW> getSubmittedTaskInstances(Long processInstanceId);
	
	public abstract List<TaskInstanceW> getAllUnfinishedTaskInstances(long processInstanceId);
		
	public abstract Collection<TaskInstanceW> getUnfinishedTaskInstances(Token token);

	public abstract Collection<TaskInstance> getAttachedEmailsTaskInstances(Long processInstanceId);

	public abstract List<BinaryVariable> getTaskAttachments(Long taskInstanceId);

	public abstract String getCaseIdentifier(Long processInstanceId);
}