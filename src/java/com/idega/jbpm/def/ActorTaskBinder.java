package com.idega.jbpm.def;

import java.rmi.RemoteException;

import javax.ejb.FinderException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.data.ICRole;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;

public class ActorTaskBinder {
	
	private JbpmConfiguration cfg;
	private SessionFactory sessionFactory;
	
	public String getBindingType(long taskId) {
		Session session = getSessionFactory().openSession();
		try {
			ActorTaskBind atb = ActorTaskBind.getBinding(session, taskId);
			if(atb == null)
				return null;
			return atb.getActorType();
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public void bindActor(String processId, String taskId, String actorId, String actorType) {
		if(actorType == null || processId == null || taskId == null || actorType == null) {
			throw new IllegalArgumentException("Parameters cannot be null");
		}
		JbpmContext ctx = cfg.createJbpmContext();
		ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(new Long(processId).longValue());
		TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
		Task task = mgmt.getTask(taskId);
		Session session = null;
		try {
			session = getSessionFactory().openSession();
			ActorTaskBind bind = ActorTaskBind.getBinding(session, task.getId());
			if(bind == null) {
				bind = new ActorTaskBind();
				bind.setTaskId(task.getId());
				bind.setActorId(actorId);
				bind.setActorType(actorType);
				session.save(bind);
			} else {
				bind.setActorId(actorId);
				bind.setActorType(actorType);
				session.flush();
			}
		} finally {
			ctx.close();
			if(session != null)
				session.close();
		}
	}
	
	public void bindUser(String processId, String taskId, String userId) {
		if(userId == null || processId == null || taskId == null) {
			throw new IllegalArgumentException("Parameters cannot be null");
		}
		JbpmContext ctx = cfg.createJbpmContext();
		ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(new Long(processId).longValue());
		TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
		Task task = mgmt.getTask(taskId);
		Session session = null;
		try {
			session = getSessionFactory().openSession();
			ActorTaskBind bind = ActorTaskBind.getBinding(session, task.getId());
			if(bind == null) {
				bind = new ActorTaskBind();
			}
			bind.setTaskId(task.getId());
			bind.setActorId(userId);
			bind.setActorType(ActorTaskBind.USER);
			session.save(bind);
		} finally {
			ctx.close();
			if(session != null)
				session.close();
		}
	}
	
	public void bindGroup(String processId, String taskId, String groupId) {
		if(groupId == null || processId == null || taskId == null) {
			throw new IllegalArgumentException("Parameters cannot be null");
		}
		JbpmContext ctx = cfg.createJbpmContext();
		ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(new Long(processId).longValue());
		TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
		Task task = mgmt.getTask(taskId);
		Session session = null;
		try {
			session = getSessionFactory().openSession();
			ActorTaskBind bind = ActorTaskBind.getBinding(session, task.getId());
			if(bind == null) {
				bind = new ActorTaskBind();
				bind.setTaskId(task.getId());
				bind.setActorId(groupId);
				bind.setActorType(ActorTaskBind.GROUP);
				session.save(bind);
			} else {
				bind.setActorId(groupId);
				bind.setActorType(ActorTaskBind.GROUP);
				session.flush();
			}
		} finally {
			ctx.close();
			if(session != null)
				session.close();
		}
	}
	
	public void bindRole(String processId, String taskId, String roleId) {
		if(roleId == null || processId == null || taskId == null) {
			throw new IllegalArgumentException("Parameters cannot be null");
		}
		JbpmContext ctx = cfg.createJbpmContext();
		ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(new Long(processId).longValue());
		TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
		Task task = mgmt.getTask(taskId);
		Session session = null;
		try {
			session = getSessionFactory().openSession();
			ActorTaskBind bind = ActorTaskBind.getBinding(session, task.getId());
			if(bind == null) {
				bind = new ActorTaskBind();
			}
			bind.setTaskId(task.getId());
			bind.setActorId(roleId);
			bind.setActorType(ActorTaskBind.ROLE);
			session.save(bind);
		} finally {
			ctx.close();
			if(session != null)
				session.close();
		}
	}
	
	public ActorTaskBind getActor(long taskId) {
		Session session = getSessionFactory().openSession();
		try {
			return ActorTaskBind.getBinding(session, taskId);
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public String getActorName(String actorId, String actorType) {
		if(actorId == null || actorType == null) {
			return null;
		}
		if(actorType.equals(ActorTaskBind.USER)) {
			try {
				User user = getUserBusiness().getUser(new Integer(actorId));
				return user.getName();
			} catch(RemoteException re) {
				re.printStackTrace();
			}
		} else if(actorType.equals(ActorTaskBind.GROUP)) {
			try {
				Group group = getGroupBusiness().getGroupByGroupID(new Integer(actorId).intValue());
				return group.getName();
			} catch(FinderException fe) {
				fe.printStackTrace();
			} catch(RemoteException re) {
				re.printStackTrace();
			}
		} else if(actorType.equals(ActorTaskBind.ROLE)) {
			try {
				AccessController accessController = CoreUtil.getIWContext().getAccessController();
				ICRole role = accessController.getRoleByRoleKey(actorId);
				return role.getRoleKey();
			} catch(FinderException fe) {
				fe.printStackTrace();
			}
		}
		return null;
	}
	
	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
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
	
	public void setJbpmConfiguration(JbpmConfiguration cfg) {
		this.cfg = cfg;
	}
	
	public JbpmConfiguration getJbpmConfiguration() {
		return cfg;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

}
