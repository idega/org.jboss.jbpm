package com.idega.jbpm.artifacts;

import java.util.Collection;
import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.variables.BinaryVariable;


/**
 * @deprecated all this stuff should be in any of the wrapper classes. E.g. ProcessInstaceW
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 *
 * Last modified: $Date: 2008/11/30 08:15:18 $ by $Author: civilis $
 */
@Deprecated
public interface ProcessArtifactsProvider {
	
	public static final String CASE_IDENTIFIER = "string_caseIdentifier";

	public abstract Collection<TaskInstance> getAttachedEmailsTaskInstances(Long processInstanceId);

	public abstract List<BinaryVariable> getTaskAttachments(Long taskInstanceId);

	public abstract String getCaseIdentifier(Long processInstanceId);
}