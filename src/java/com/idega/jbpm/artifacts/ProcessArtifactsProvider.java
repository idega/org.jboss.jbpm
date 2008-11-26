package com.idega.jbpm.artifacts;

import java.util.Collection;
import java.util.List;

import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 *
 * Last modified: $Date: 2008/11/26 13:16:13 $ by $Author: civilis $
 */
public interface ProcessArtifactsProvider {
	
	public static final String CASE_IDENTIFIER = "string_caseIdentifier";
	
	public static final String CASE_DESCRIPTION = "string_caseDescription";

	public abstract Collection<TaskInstance> getAttachedEmailsTaskInstances(Long processInstanceId);

	public abstract List<BinaryVariable> getTaskAttachments(Long taskInstanceId);

	public abstract String getCaseIdentifier(Long processInstanceId);
	
	public abstract String getProcessDescription(Long processInstanceId);
}