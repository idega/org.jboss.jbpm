package com.idega.jbpm.exe;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.identity.Role;
import com.idega.jbpm.rights.Right;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.18 $
 * 
 *          Last modified: $Date: 2008/12/28 12:08:04 $ by $Author: civilis $
 */
public interface ProcessInstanceW {

	/**
	 * 
	 * @return all task instances, including the ended ones
	 */
	public abstract List<TaskInstanceW> getAllTaskInstances();

	/**
	 * 
	 * @param processInstanceId
	 * @return all task instances, that were submitted and won't ever change.
	 *         Can be treated as documents.
	 */
	public abstract List<TaskInstanceW> getSubmittedTaskInstances();

	public abstract List<BPMDocumentImpl> getSubmittedDocumentsForUser(
			User user, Locale locale);

	public abstract List<BPMDocumentImpl> getTaskDocumentsForUser(User user,
			Locale locale);

	/**
	 * 
	 * @param rootToken
	 * @return not ended task instances for the token provided
	 */
	public abstract Collection<TaskInstanceW> getUnfinishedTaskInstances(
			Token rootToken);

	/**
	 * 
	 * @return all process instance task instances, that are has not ended yet
	 *         (end == null)
	 */
	public abstract List<TaskInstanceW> getAllUnfinishedTaskInstances();

	/**
	 * the same as calling getProcessInstance().getId()
	 * 
	 * @return
	 */
	public abstract Long getProcessInstanceId();

	/**
	 * should be used only in factory methods
	 * 
	 * @param processInstanceId
	 */
	public abstract void setProcessInstanceId(Long processInstanceId);

	/**
	 * assigned handler user id (see assignHandler method)
	 * 
	 * @return
	 */
	public abstract Integer getHandlerId();

	/**
	 * if handlerUserId not null - assigns handler to the process, unassigns
	 * otherwise
	 * 
	 * @param handlerUserId
	 */
	public abstract void assignHandler(Integer handlerUserId);

	/**
	 * 
	 * @return process instance related watcher (see javadoc of ProcessWatch)
	 */
	public abstract ProcessWatch getProcessWatcher();

	/**
	 * 
	 * @return if the process can be assigned to the case handler
	 */
	public abstract boolean hasHandlerAssignmentSupport();

	public abstract List<User> getUsersConnectedToProcess();

	public abstract void setContactsPermission(Role role, Integer userId);

	/**
	 * get jbpm process instance this wrapper wraps
	 * 
	 * @return
	 */
	public abstract ProcessInstance getProcessInstance();

	/**
	 * 
	 * @return human readable identifier, used for distinguishing processes more
	 *         easily, as well, as using in search etc
	 */
	public abstract String getProcessIdentifier();

	public abstract String getProcessDescription();

	/**
	 * @return definition wrapper this process instance relates to
	 */
	public abstract ProcessDefinitionW getProcessDefinitionW();

	/**
	 * gets process name - usually the name of the start task view
	 * 
	 * @param locale
	 * @return
	 */
	// public abstract String getName(Locale locale);
	/**
	 * checks if the current logged in user has right against this process
	 * instance
	 * 
	 * @param right
	 * @return
	 */
	public abstract boolean hasRight(Right right);

	/**
	 * checks if the user provided has right against this process instance
	 * 
	 * @param right
	 * @param user
	 *            to check right against
	 * @return
	 */
	public abstract boolean hasRight(Right right, User user);

	/**
	 * @return task instances for submitted email attachments
	 */
	public abstract Collection<TaskInstance> getAttachedEmailsTaskInstances();
}