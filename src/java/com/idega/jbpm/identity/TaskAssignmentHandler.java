package com.idega.jbpm.identity;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
import com.idega.user.business.GroupBusiness;
import com.idega.user.data.Group;
import com.idega.util.CoreUtil;
import com.idega.webface.WFUtil;

public class TaskAssignmentHandler implements AssignmentHandler {

	public void assign(Assignable assignable, ExecutionContext executionContext)
			throws Exception {
		TaskInstance taskInstance = (TaskInstance) assignable;
		Task task = taskInstance.getTask();
		
		long taskId = task.getId();
		
		SessionFactory sessionFactory = (SessionFactory) WFUtil.getBeanInstance("idega_jbpmDSHibernateSessionFactory");
		Session session = sessionFactory.getCurrentSession();
		try {
			ActorTaskBind atb = ActorTaskBind.getBinding(session, taskId);
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
		} finally {
			if(session != null)
				session.close();
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
	
	public SessionFactory sessionFactory;

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

}
