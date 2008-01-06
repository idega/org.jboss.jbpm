package com.idega.jbpm.identity;

import java.util.Collection;
import java.util.Iterator;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.data.dao.JbpmBindsDao;
import com.idega.user.business.GroupBusiness;
import com.idega.user.data.Group;
import com.idega.util.CoreUtil;
import com.idega.webface.WFUtil;

public class TaskAssignmentHandler implements AssignmentHandler {

	private static final long serialVersionUID = 5029739971157745012L;
	private JbpmBindsDao jbpmBindsDao;

	public void assign(Assignable assignable, ExecutionContext executionContext)
			throws Exception {
		TaskInstance taskInstance = (TaskInstance) assignable;
		Task task = taskInstance.getTask();
		
		long taskId = task.getId();
		
		ActorTaskBind atb = getJbpmBindsDao().getActorTaskBind(taskId);
		
		if(atb != null) {
			String type = atb.getActorType();
			String actorId = atb.getActorId();
			if(type.equals(ActorTaskBind.USER)) {
				assignable.setActorId(actorId);
			} else if(type.equals(ActorTaskBind.ROLE)) {
				AccessController accessController = CoreUtil.getIWContext().getAccessController();
				Collection groups = accessController.getAllGroupsForRoleKey(actorId, CoreUtil.getIWContext());
				for(Iterator it = groups.iterator(); it.hasNext(); ) {
					Group group = (Group) it.next();
					Collection users = getGroupBusiness().getUsers(group);
					String[] pooledIds = {};
					pooledIds = (String[]) users.toArray(pooledIds);
					assignable.setPooledActors(pooledIds);
				}
			} else if(type.equals(ActorTaskBind.GROUP)) {
				Collection users = getGroupBusiness().getUsers(new Integer(actorId).intValue());
				String[] pooledIds = {};
				pooledIds = (String[]) users.toArray(pooledIds);
				assignable.setPooledActors(pooledIds);
			}
		}
	}
	
	protected GroupBusiness getGroupBusiness() {
		try {
			return (GroupBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), GroupBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public JbpmBindsDao getJbpmBindsDao() {
		
		if(jbpmBindsDao == null)
			jbpmBindsDao = (JbpmBindsDao)WFUtil.getBeanInstance("jbpmBindsDao");
		
		return jbpmBindsDao;
	}

	public void setJbpmBindsDao(JbpmBindsDao jbpmBindsDao) {
		this.jbpmBindsDao = jbpmBindsDao;
	}
}