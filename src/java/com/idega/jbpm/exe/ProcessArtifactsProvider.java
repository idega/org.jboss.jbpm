package com.idega.jbpm.exe;

import java.util.Collection;
import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/05/10 18:08:07 $ by $Author: civilis $
 */
public interface ProcessArtifactsProvider {

	public abstract Collection<TaskInstance> getSubmittedTaskInstances(
			Long processInstanceId);

	public abstract Collection<TaskInstance> getAttachedEmailsTaskInstances(Long processInstanceId);

	public abstract List<BinaryVariable> getTaskAttachments(Long taskInstanceId);

}