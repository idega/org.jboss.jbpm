package com.idega.jbpm.identity.permission;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/16 19:00:30 $ by $Author: civilis $
 */
public class SubmitTaskParametersPermission extends org.jbpm.security.permission.SubmitTaskParametersPermission implements BPMTaskAccessPermission {

	private static final long serialVersionUID = -8197508250166066993L;
	private List<Access> accesses;
	private TaskInstance taskInstance;
	private Boolean checkOnlyInActorsPool;
	
	public SubmitTaskParametersPermission(String name, String actions) {
		super(name, actions);
		throw new IllegalArgumentException("Use constructor: constructor(String name, String actions, TaskInstance taskInstance)");
	}
	
	public SubmitTaskParametersPermission(String name, String actions, TaskInstance taskInstance) {
	    super(name == null ? "taskInstance" : name, actions);
	    
	    if(taskInstance == null)
	    	throw new IllegalArgumentException("taskInstance should not be null");
	    
	    accesses = new ArrayList<Access>(2);
	    accesses.add(Access.read);
	    accesses.add(Access.write);
	    this.taskInstance = taskInstance;
	}
	
	public List<Access> getAccesses() {
		return accesses;
	}
	
	@Override
	public boolean implies(Permission permission) {

		if(permission instanceof BPMTaskAccessPermission) {
			
			BPMTaskAccessPermission p = (BPMTaskAccessPermission)permission;
			
			for (Access access : p.getAccesses()) {

				if(!accesses.contains(access))
					return false;
			}
			
			return true;
		}
		
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
}