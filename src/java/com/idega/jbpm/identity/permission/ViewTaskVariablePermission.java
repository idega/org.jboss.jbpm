package com.idega.jbpm.identity.permission;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/27 11:01:10 $ by $Author: civilis $
 */
public class ViewTaskVariablePermission extends org.jbpm.security.permission.ViewTaskParametersPermission implements BPMTaskVariableAccessPermission {

	private static final long serialVersionUID = 649569607062662766L;
	private List<Access> accesses;
	private TaskInstance taskInstance;
	private Boolean checkOnlyInActorsPool;
	private String variableIndentifier;
	
	public ViewTaskVariablePermission(String name, String actions) {
	    super(name, actions);
	    throw new IllegalArgumentException("Use constructor: constructor(String name, String actions, TaskInstance taskInstance)");
	}
	
	public ViewTaskVariablePermission(String name, String actions, TaskInstance taskInstance) {
	    super(name, actions);
	    
	    if(taskInstance == null)
	    	throw new IllegalArgumentException("taskInstance should not be null");
	    
	    accesses = new ArrayList<Access>(1);
	    accesses.add(Access.read);
	    this.taskInstance = taskInstance;
	}
	
	public List<Access> getAccesses() {
		return accesses;
	}
	
	@Override
	public boolean implies(Permission permission) {

		return false;
	}
	
	public TaskInstance getTaskInstance() {
		return taskInstance;
	}
	
	public boolean getCheckOnlyInActorsPool() {
		return checkOnlyInActorsPool == null ? false : checkOnlyInActorsPool;
	}

	public void setCheckOnlyInActorsPool(boolean checkOnlyInActorsPool) {
		this.checkOnlyInActorsPool = checkOnlyInActorsPool;
	}

	public String getVariableIndentifier() {
		return variableIndentifier;
	}

	public void setVariableIndentifier(String variableIndentifier) {
		this.variableIndentifier = variableIndentifier;
	}
}