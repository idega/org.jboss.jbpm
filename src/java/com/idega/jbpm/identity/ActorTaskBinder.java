package com.idega.jbpm.identity;

import java.rmi.RemoteException;

import javax.ejb.FinderException;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.data.ICRole;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.data.dao.BpmBindsDAO;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;

public class ActorTaskBinder {
	
	private BpmBindsDAO jbpmBindsDao;
	private IdegaJbpmContext idegaJbpmContext;
	
	public String getBindingType(long taskId) {
		
		ActorTaskBind atb = getJbpmBindsDao().getActorTaskBind(taskId);
		return atb == null ? null : atb.getActorType(); 
	}
	
	public void bindActor(String processId, String taskId, String actorId, String actorType) {
		
		if(actorType == null || processId == null || taskId == null || actorType == null)
			throw new IllegalArgumentException("Any of parameters cannot be null");
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(Long.parseLong(processId));
		Task task = pd.getTaskMgmtDefinition().getTask(taskId);
		
		try {
			ActorTaskBind bind = getJbpmBindsDao().getActorTaskBind(task.getId());
			
			if(bind == null) {
				bind = new ActorTaskBind();
				bind.setTaskId(task.getId());
				bind.setActorId(actorId);
				bind.setActorType(actorType);
				getJbpmBindsDao().persist(bind);
			} else {
				bind.setActorId(actorId);
				bind.setActorType(actorType);
			}
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public void bindUser(String processId, String taskId, String userId) {
		
		if(userId == null || processId == null || taskId == null)
			throw new IllegalArgumentException("Any of parameters cannot be null");
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(Long.parseLong(processId));
		Task task = pd.getTaskMgmtDefinition().getTask(taskId);
		
		try {
			ActorTaskBind bind = getJbpmBindsDao().getActorTaskBind(task.getId());
			
			if(bind == null)
				bind = new ActorTaskBind();
			
			bind.setTaskId(task.getId());
			bind.setActorId(userId);
			bind.setActorType(ActorTaskBind.USER);
			getJbpmBindsDao().persist(bind);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public void bindGroup(String processId, String taskId, String groupId) {
		
		if(groupId == null || processId == null || taskId == null)
			throw new IllegalArgumentException("Any of parameters cannot be null");
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(Long.parseLong(processId));
		Task task = pd.getTaskMgmtDefinition().getTask(taskId);
		
		try {
			ActorTaskBind bind = getJbpmBindsDao().getActorTaskBind(task.getId());
			
			if(bind == null) {
				bind = new ActorTaskBind();
				bind.setTaskId(task.getId());
				bind.setActorId(groupId);
				bind.setActorType(ActorTaskBind.GROUP);
				getJbpmBindsDao().persist(bind);
			} else {
				bind.setActorId(groupId);
				bind.setActorType(ActorTaskBind.GROUP);
				//TODO: check if this is needed getJbpmBindsDao().flush();
			}
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public void bindRole(String processId, String taskId, String roleId) {
		
		if(roleId == null || processId == null || taskId == null)
			throw new IllegalArgumentException("Any of parameters cannot be null");
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(Long.parseLong(processId));
		Task task = pd.getTaskMgmtDefinition().getTask(taskId);
		
		try {
			ActorTaskBind bind = getJbpmBindsDao().getActorTaskBind(task.getId());
			
			if(bind == null)
				bind = new ActorTaskBind();
			
			bind.setTaskId(task.getId());
			bind.setActorId(roleId);
			bind.setActorType(ActorTaskBind.ROLE);
			getJbpmBindsDao().persist(bind);
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public ActorTaskBind getActor(long taskId) {
		return getJbpmBindsDao().getActorTaskBind(taskId);
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

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BpmBindsDAO getJbpmBindsDao() {
		return jbpmBindsDao;
	}

	public void setJbpmBindsDao(BpmBindsDAO jbpmBindsDao) {
		this.jbpmBindsDao = jbpmBindsDao;
	}
}