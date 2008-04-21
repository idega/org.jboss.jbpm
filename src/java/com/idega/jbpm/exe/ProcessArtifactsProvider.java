package com.idega.jbpm.exe;

import java.util.Collection;
import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.exe.impl.BinaryVariable;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/21 05:13:45 $ by $Author: civilis $
 */
public interface ProcessArtifactsProvider {

	public abstract Collection<TaskInstance> getSubmittedTaskInstances(
			Long processInstanceId);

	public abstract Collection<TaskInstance> getAttachedEmailsTaskInstances(Long processInstanceId);

	public abstract List<BinaryVariable> getTaskAttachments(Long taskInstanceId);

}