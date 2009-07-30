package com.idega.jbpm.exe;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import com.idega.jbpm.identity.Role;
import com.idega.jbpm.rights.Right;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.33 $ Last modified: $Date: 2009/07/03 08:56:49 $ by $Author: valdas $
 */
public interface ProcessInstanceW {
	
	/**
	 * @return all task instances, including the ended ones
	 */
	public abstract List<TaskInstanceW> getAllTaskInstances();
	
	/**
	 * @param processInstanceId
	 * @return all task instances, that were submitted and won't ever change. Can be treated as
	 *         documents.
	 */
	public abstract List<TaskInstanceW> getSubmittedTaskInstances();
	
	public abstract List<BPMDocument> getSubmittedDocumentsForUser(User user,
	        Locale locale);
	
	public abstract List<BPMDocument> getTaskDocumentsForUser(User user,
	        Locale locale);
	
	/**
	 * @param rootToken
	 * @return not ended task instances for the token provided
	 */
	public abstract Collection<TaskInstanceW> getUnfinishedTaskInstances(
	        Token rootToken);
	
	/**
	 * @return all process instance task instances, that are has not ended yet (end == null) and not
	 *         hidden
	 */
	public abstract List<TaskInstanceW> getAllUnfinishedTaskInstances();
	
	/**
	 * @param taskName
	 * @return see javadoc for getAllUnfinishedTaskInstances(), but also filtered by taskName
	 */
	public abstract List<TaskInstanceW> getUnfinishedTaskInstancesForTask(
	        String taskName);
	
	/**
	 * @param taskName
	 * @return see javadoc for getUnfinishedTaskInstancesForTask( String taskName), only single
	 *         unfinished task instance is taken
	 */
	public abstract TaskInstanceW getSingleUnfinishedTaskInstanceForTask(
	        String taskName);
	
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
	 * if handlerUserId not null - assigns handler to the process, unassigns otherwise
	 * 
	 * @param handlerUserId
	 */
	public abstract void assignHandler(Integer handlerUserId);
	
	/**
	 * @return process instance related watcher (see javadoc of ProcessWatch)
	 */
	public abstract ProcessWatch getProcessWatcher();
	
	/**
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
	 * @return human readable identifier, used for distinguishing processes more easily, as well, as
	 *         using in search etc
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
	 * checks if the current logged in user has right against this process instance
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
	
	public abstract List<BPMEmailDocument> getAttachedEmails(User user);
	
	public abstract boolean hasEnded();
	
	public abstract TaskInstanceW getStartTaskInstance();
	
	public abstract Collection<Role> getRolesContactsPermissions(Integer userId);
	
	/**
	 * @return a list of documents that where added with addAttachment subprocess
	 */
	public List<BinaryVariable> getAttachments();
	
	/**
	 * @param variableName
	 *            - full variable name
	 * @param token
	 *            optional, if null is passed, process intance root token is used
	 * @return retrieves a variable in the scope of the token. If the given token does not have a
	 *         variable for the given name, the variable is searched for up the token hierarchy.
	 */
	public abstract Object getVariableLocally(String variableName, Token token);
	
	public abstract TaskMgmtInstanceW getTaskMgmtInstance();
	
	public abstract User getOwner();
}