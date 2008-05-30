package com.idega.jbpm.artifacts;

import java.util.Collection;
import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.variables.BinaryVariable;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/05/30 08:44:09 $ by $Author: valdas $
 */
public interface ProcessArtifactsProvider {
	
	public static final String CASE_IDENTIFIER = "string:caseIdentifier";

	public abstract Collection<TaskInstance> getSubmittedTaskInstances(
			Long processInstanceId);

	public abstract Collection<TaskInstance> getAttachedEmailsTaskInstances(Long processInstanceId);

	public abstract List<BinaryVariable> getTaskAttachments(Long taskInstanceId);

	public abstract String getCaseIdentifier(Long processInstanceId);
}