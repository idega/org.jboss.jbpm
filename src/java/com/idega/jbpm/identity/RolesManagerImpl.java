package com.idega.jbpm.identity;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.persistence.Param;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.19 $
 * 
 * Last modified: $Date: 2008/05/23 08:19:55 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmRolesManager")
@Transactional(readOnly=true)
public class RolesManagerImpl implements RolesManager {
	
	private BPMDAO bpmDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private AuthorizationService authorizationService;
	
	private static final Logger logger = Logger.getLogger(RolesManagerImpl.class.getName());

	@Transactional(readOnly=false)
	public void createIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId) {
		
		if(roles.isEmpty())
			return;

		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			
			if(role.getScope() == RoleScope.PI)
				rolesNames.add(role.getRoleName());
		}
		
		//List<ProcessRole> processRoles = getBpmDAO().getProcessRolesByRolesNames(rolesNames, processInstanceId);
		
		List<ProcessRole> processRoles = 
			getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
					new Param(ProcessRole.processRoleNameProperty, rolesNames),
					new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
			);
		
		for (ProcessRole role : processRoles) {
			
			List<NativeIdentityBind> identities = role.getNativeIdentities();
			
			boolean contains = false;
			
			for (NativeIdentityBind nativeIdentityBind : identities) {
				
				if(identityType == nativeIdentityBind.getIdentityType() && identityId.equals(nativeIdentityBind.getIdentityId())) {
					contains = true;
					break;
				}
			}
			
			if(contains)
				continue;
			
			NativeIdentityBind nidentity = new NativeIdentityBind();
			nidentity.setIdentityId(identityId);
			nidentity.setIdentityType(identityType);
			nidentity.setProcessRole((ProcessRole)getBpmDAO().merge(role));
			getBpmDAO().persist(nidentity);
		}
	}
	
	public BPMDAO getBpmDAO() {
		return bpmDAO;
	}

	@Autowired
	public void setBpmDAO(BPMDAO bpmDAO) {
		this.bpmDAO = bpmDAO;
	}
	
	public void hasRightsToStartTask(long taskInstanceId, int userId) throws BPMAccessControlException {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			if(taskInstance.getStart() != null || taskInstance.hasEnded())
				throw new BPMAccessControlException("Task ("+taskInstanceId+") has already been started, or has already ended", "Task has already been started, or has already ended");
			
			if(taskInstance.getActorId() == null || !taskInstance.getActorId().equals(String.valueOf(userId)))
				throw new BPMAccessControlException("User ("+userId+") tried to start task, but not assigned to the user provided. Assigned: "+taskInstance.getActorId(), "User should be taken or assigned of the task first to start working on it");
			
			SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
			getAuthorizationService().checkPermission(permission);
		
		} catch (BPMAccessControlException e) {
			throw e;
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("User has no access to modify this task");
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public void checkPermission(Permission permission) throws BPMAccessControlException {
		
		try {
			getAuthorizationService().checkPermission(permission);
			
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("No access");
		}
	}
	
	public void hasRightsToAssignTask(long taskInstanceId, int userId) throws BPMAccessControlException {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			if(taskInstance.getStart() != null || taskInstance.hasEnded())
				throw new BPMAccessControlException("Task ("+taskInstanceId+") has already been started, or has already ended", "Task has been started by someone, or has ended.");
			
			if(taskInstance.getActorId() != null)
				throw new BPMAccessControlException("Task ("+taskInstanceId+") is already assigned to: "+taskInstance.getActorId(), "This task has been assigned already");
			
			SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
			getAuthorizationService().checkPermission(permission);
		
		} catch (BPMAccessControlException e) {
			throw e;
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("User has no access to modify this task");
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	@Transactional(readOnly=true)
	public Collection<User> getAllUsersForRoles(Collection<String> rolesNames, ProcessInstance pi) {

		List<ProcessRole> proles =
				getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class, 
						new Param(ProcessRole.processRoleNameProperty, rolesNames),
						new Param(ProcessRole.processInstanceIdProperty, pi.getId())
				);
		
		IWApplicationContext iwac = getIWMA().getIWApplicationContext();
		UserBusiness userBusiness = getUserBusiness(iwac);
		
		ArrayList<Group> allGroups = new ArrayList<Group>();
		HashMap<String, User> allUsers = new HashMap<String, User>();
		
		for (ProcessRole prole : proles) {
			
			if(prole.getNativeIdentities() == null || prole.getNativeIdentities().isEmpty()) {
				
				@SuppressWarnings("unchecked")
				Collection<Group> grps = getAccessController().getAllGroupsForRoleKey(prole.getProcessRoleName(), iwac);
				
				if(grps != null)
					allGroups.addAll(grps);
				
			} else {
				
				try {
					
					for (NativeIdentityBind identity : prole.getNativeIdentities()) {
						
						if(identity.getIdentityType() == IdentityType.USER) {
							
							User user = userBusiness.getUser(new Integer(identity.getIdentityId()));
							allUsers.put(user.getPrimaryKey().toString(), user);
							
						} else if(identity.getIdentityType() == IdentityType.GROUP) {
							
							@SuppressWarnings("unchecked")
							Collection<User> groupUsers = userBusiness.getUsersInGroup(new Integer(identity.getIdentityId()));
							
							for (User user : groupUsers)
								allUsers.put(user.getPrimaryKey().toString(), user);
						}
					}
					
				} catch (RemoteException e) {
					logger.log(Level.SEVERE, "Exception while loading users from nativeIdentities", e);
				}
			}
		}
		
		try {
			for (Group group : allGroups) {

				@SuppressWarnings("unchecked")
				Collection<User> users = userBusiness.getUsersInGroup(group);
				
				for (User user : users) {
					allUsers.put(user.getPrimaryKey().toString(), user);
				}
			}
			
		} catch (RemoteException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving users from roles assigned groups", e);
		}
		
		/*
		List<ProcessRole> piScopeRoles = 
			getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class, 
					new Param(ProcessRole.processRoleNameProperty, rolesNames),
					new Param(ProcessRole.processInstanceIdProperty, pi.getId())
			);
		
		try {
			for (ProcessRole processRole : piScopeRoles) {
				
				if(!processRole.getNativeIdentities().isEmpty()) {
					
					for (NativeIdentityBind identity : processRole.getNativeIdentities()) {
						
						if(identity.getIdentityType() == IdentityType.USER) {
							
							User user = userBusiness.getUser(new Integer(identity.getIdentityId()));
							allUsers.put(user.getPrimaryKey().toString(), user);
							
						} else if(identity.getIdentityType() == IdentityType.GROUP) {
							
							@SuppressWarnings("unchecked")
							Collection<User> groupUsers = userBusiness.getUsersInGroup(new Integer(identity.getIdentityId()));
							
							for (User user : groupUsers)
								allUsers.put(user.getPrimaryKey().toString(), user);
						}
					}
				}
			}
			
		} catch (RemoteException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving users in native identities", e);
		}
		*/
		
		return allUsers.values();
	}
	
	public void createNativeRolesFromProcessRoles(String processName, Collection<Role> roles) {
		
		AccessController ac = getAccessController();

		for (Role role : roles) {
			
			if(role.getScope() == RoleScope.PD) {
				ac.checkIfRoleExistsInDataBaseAndCreateIfMissing(role.getRoleName());
			}
		}
	}
	
//	public List<ProcessRole> createProcessRolesForPDScope(String processName, Collection<Role> roles, Long processInstanceId) {
//		
//		return createProcessRoles(processName, roles, processInstanceId, RoleScope.PD);
//	}
//	
//	public List<ProcessRole> createProcessRolesForPIScope(String processName, Collection<Role> roles, Long processInstanceId) {
//		
//		return createProcessRoles(processName, roles, processInstanceId, RoleScope.PI);
//	}
	
	public List<ProcessRole> createProcessRoles(String processName, Collection<Role> roles, Long processInstanceId) {
		
		HashSet<String> rolesNamesToCreate = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			
			rolesNamesToCreate.add(role.getRoleName());
//			if(role.getScope() == scope) {
//				rolesNamesToCreate.add(role.getRoleName());
//			}
		}
		
		ArrayList<ProcessRole> processRoles = new ArrayList<ProcessRole>();
		
		if(!rolesNamesToCreate.isEmpty()) {
		
			List<ProcessRole> proles = 
				getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
						new Param(ProcessRole.processRoleNameProperty, rolesNamesToCreate),
						new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
				);
			
			for (ProcessRole processRole : proles) {
				
				if(rolesNamesToCreate.contains(processRole.getProcessRoleName()))
					rolesNamesToCreate.remove(processRole.getProcessRoleName());
				
				processRoles.add(processRole);
			}
			
			for (String roleNameToCreate : rolesNamesToCreate) {
				
				ProcessRole processRole = new ProcessRole();
				processRole.setProcessName(processName);
				processRole.setProcessRoleName(roleNameToCreate);
				processRole.setProcessInstanceId(processInstanceId);
				
				getBpmDAO().persist(processRole);
				processRoles.add(processRole);
			}
		}
		
		return processRoles;
	}
	
	/*
	public List<ProcessRole> createProcessRoles(String processName, Collection<Role> roles, Long processInstanceId) {
		
		HashSet<String> pdScopeRolesNamesToCreate = new HashSet<String>();
		HashSet<String> piScopeRolesNamesToCreate;
		
		if(processInstanceId != null)
			piScopeRolesNamesToCreate = new HashSet<String>();
		else
			piScopeRolesNamesToCreate = null;
		
		for (Role role : roles) {
			
			if(role.getScope() == RoleScope.PD)
				pdScopeRolesNamesToCreate.add(role.getRoleName());
			else if(role.getScope() == RoleScope.PI && piScopeRolesNamesToCreate != null)
				piScopeRolesNamesToCreate.add(role.getRoleName());
		}
		
		List<ProcessRole> PDScopeProcRoles;
		
		if(!pdScopeRolesNamesToCreate.isEmpty()) {
		
			PDScopeProcRoles = 
				getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndProcessNameAndPIIdIsNull, ProcessRole.class,
						new Param(ProcessRole.processRoleNameProperty, pdScopeRolesNamesToCreate),
						new Param(ProcessRole.processNameProperty, processName)
				);
			
		} else {
			PDScopeProcRoles = new ArrayList<ProcessRole>(0);
		}
		
		ArrayList<ProcessRole> processRoles = new ArrayList<ProcessRole>();
		
		for (ProcessRole processRole : PDScopeProcRoles) {
			
			if(pdScopeRolesNamesToCreate.contains(processRole.getProcessRoleName()))
				pdScopeRolesNamesToCreate.remove(processRole.getProcessRoleName());
			
			processRoles.add(processRole);
		}
		
		AccessController ac = getAccessController();
		
		for (String roleNameToCreate : pdScopeRolesNamesToCreate) {
			
			ProcessRole processRole = new ProcessRole();
			processRole.setProcessName(processName);
			processRole.setProcessRoleName(roleNameToCreate);
			
			getBpmDAO().persist(processRole);
			processRoles.add(processRole);
			ac.checkIfRoleExistsInDataBaseAndCreateIfMissing(roleNameToCreate);
		}
		
		if(piScopeRolesNamesToCreate != null && !piScopeRolesNamesToCreate.isEmpty()) {
		
			List<ProcessRole> PIScopeProcRoles = 
				getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
						new Param(ProcessRole.processRoleNameProperty, piScopeRolesNamesToCreate),
						new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
				);
				
			for (ProcessRole processRole : PIScopeProcRoles) {
				
				if(piScopeRolesNamesToCreate.contains(processRole.getProcessRoleName()))
					piScopeRolesNamesToCreate.remove(processRole.getProcessRoleName());
				
				processRoles.add(processRole);
			}
			
			for (String roleNameToCreate : piScopeRolesNamesToCreate) {
				
				ProcessRole processRole = new ProcessRole();
				processRole.setProcessName(processName);
				processRole.setProcessRoleName(roleNameToCreate);
				processRole.setProcessInstanceId(processInstanceId);
				
				getBpmDAO().persist(processRole);
				processRoles.add(processRole);
			}
		}
		
		return processRoles;
	}*/
	
//	public void createTaskRolesPermissionsPIScope(Task task, List<Role> roles, Long processInstanceId) {
//		
//		assignTaskRolesPermissions(task, roles, processInstanceId);
//	}
//	
//	public void createTaskRolesPermissionsPDScope(Task task, List<Role> roles) {
//		
//		assignTaskRolesPermissions(task, roles, null);
//	}
	
	@Transactional(readOnly=true)
	public void createTaskRolesPermissions(Task task, List<Role> roles) {
		
		if(!roles.isEmpty()) {
		
			HashSet<String> rolesNames = new HashSet<String>(roles.size());
			
			for (Role role : roles) {
				rolesNames.add(role.getRoleName());
			}
			
			List<ActorPermissions> perms = getBpmDAO().getResultList(ActorPermissions.getSetByTaskIdAndProcessRoleNames, ActorPermissions.class,
					new Param(ActorPermissions.taskIdProperty, task.getId()),
					new Param(ActorPermissions.roleNameProperty, rolesNames)
			);
			
//			if eq, then all perms are created
			if(perms.size() < rolesNames.size()) {
				
				for (ActorPermissions actorPermissions : perms) {
					roles.remove(actorPermissions.getRoleName());
					rolesNames.remove(actorPermissions.getRoleName());
				}
			
				logger.log(Level.INFO, "Creating permissions for task: "+task.getId()+", for roles: "+rolesNames);
			
				for (Role role : roles) {
					
					ActorPermissions perm = new ActorPermissions();
					perm.setTaskId(task.getId());
					perm.setRoleName(role.getRoleName());
					perm.setReadPermission(role.getAccesses().contains(Access.read));
					perm.setWritePermission(role.getAccesses().contains(Access.write));
					
					getBpmDAO().persist(perm);
				}
			}
		}
	}
	
	@Transactional(readOnly=true)
	public void assignTaskRolesPermissions(Task task, List<Role> roles, Long processInstanceId) {
		
		if(roles.isEmpty())
			return;
		
		createTaskRolesPermissions(task, roles);
		
		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			rolesNames.add(role.getRoleName());
		}
		
		List<ProcessRole> proles = 
			getBpmDAO().getResultList(
					ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
					new Param(ProcessRole.processRoleNameProperty, rolesNames),
					new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
			);
		
		if(proles != null && !proles.isEmpty()) {
		
			for (Iterator<ProcessRole> iterator = proles.iterator(); iterator.hasNext();) {
				
				ProcessRole prole = iterator.next();
				
				List<ActorPermissions> perms = prole.getActorPermissions();
				
				boolean hasPermForTask = false;
				
				if(!perms.isEmpty()) {
					
					for (ActorPermissions actorPermissions : perms) {
						
						if(actorPermissions.getTaskId().equals(task.getId()) && actorPermissions.getTaskInstanceId() == null)
							hasPermForTask = true;
					}
				}
				
				if(hasPermForTask)
					iterator.remove();
			}

			if(!proles.isEmpty()) {
				
				List<ActorPermissions> perms = getBpmDAO().getResultList(ActorPermissions.getSetByTaskIdAndProcessRoleNames, ActorPermissions.class,
						new Param(ActorPermissions.taskIdProperty, task.getId()),
						new Param(ActorPermissions.roleNameProperty, rolesNames)
				);
				
				for (ProcessRole prole : proles) {
					
					for (Iterator<ActorPermissions> iterator = perms.iterator(); iterator.hasNext();) {
						
						ActorPermissions perm = iterator.next();
						
						if(prole.getProcessRoleName().equals(perm.getRoleName())) {
							List<ActorPermissions> rolePerms = prole.getActorPermissions();
							
							if(rolePerms == null) {
								rolePerms = new ArrayList<ActorPermissions>();
							}
							
							rolePerms.add(perm);
							iterator.remove();
							getBpmDAO().merge(prole);
						}
					}
				}
			}
			
		} else
			logger.log(Level.WARNING, "No process roles found by roles: "+rolesNames+", processInstanceId: "+processInstanceId);
		
		
		
//		List<String> assignedRolesNames  = 
//			getBpmDAO().getResultList(ProcessRole.getSetHavingPermissionsByTaskIdAndRoleName, String.class,
//					new Param(ProcessRole.processInstanceIdProperty, processInstanceId),
//					new Param(ProcessRole.processRoleNameProperty, rolesNames),
//					new Param(ActorPermissions.taskIdProperty, task.getId())
//			);
//		
//		if(assignedRolesNames.size() < roles.size()) {
//			
//			
//			
//		}
		
		
		/*

		HashMap<String, Role> mappedRoles = new HashMap<String, Role>(roles.size());
		
		for (Role role : roles) {
			
			if((processInstanceId == null && role.getScope() == RoleScope.PD) || (processInstanceId != null && role.getScope() == RoleScope.PI)) {
			
				mappedRoles.put(role.getRoleName(), role);
			}
		}
		
		
		
		List<ProcessRole> proles;
		
		if(!mappedRoles.isEmpty()) {
		
			if(processInstanceId == null) {
				
				proles = 
					getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndProcessNameAndPIIdIsNull, ProcessRole.class,
							new Param(ProcessRole.processRoleNameProperty, mappedRoles.keySet()),
							new Param(ProcessRole.processNameProperty, task.getProcessDefinition().getName())
					);
			} else {
				
				proles = 
					getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
							new Param(ProcessRole.processRoleNameProperty, mappedRoles.keySet()),
							new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
					);
			}
		} else {
			proles = new ArrayList<ProcessRole>(1);
		}
		*/
		
		/* TODO: remove getSetByTaskIdAndProcessRole query
		List<ActorPermissions> perms = 
			getBpmDAO().getResultList(ActorPermissions.getSetByTaskIdAndProcessRole, new Param[] {
						new Param(ActorPermissions.taskIdProperty, task.getId()),
						new Param(ActorPermissions.processRoleProperty, proles)
					}, ActorPermissions.class);
		 
		
		for (ProcessRole prole : proles) {
			
			List<ActorPermissions> perms = prole.getActorPermissions();
			
			boolean hasPermForTask = false;
			
			if(!perms.isEmpty()) {
				
				for (ActorPermissions actorPermissions : perms) {
					
					if(actorPermissions.getTaskId().equals(task.getId()) && actorPermissions.getTaskInstanceId() == null)
						hasPermForTask = true;
				}
			}
			
			if(!hasPermForTask) {
				
				Role rolePermsToAssign = mappedRoles.get(prole.getProcessRoleName());
				
				ActorPermissions perm = new ActorPermissions();
				perm.setTaskId(task.getId());
				perm.setProcessRole(prole);
				perm.setReadPermission(rolePermsToAssign.getAccesses().contains(Access.read));
				perm.setWritePermission(rolePermsToAssign.getAccesses().contains(Access.write));
				
				getBpmDAO().persist(perm);
			}
		}
		*/
	}
	
	public List<Long> getProcessInstancesIdsForCurrentUser() {
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		final List<Long> prolesIds;
		
		if(iwc.isSuperAdmin()) {
			
			prolesIds = getBpmDAO().getResultList(ProcessRole.getAllProcessInstancesIds, Long.class);
			
		} else {
			
			Integer currentUserId = iwc.getCurrentUserId();
		
			@SuppressWarnings("unchecked")
			Set<String> userRoles = getAccessController().getAllRolesForCurrentUser(iwc);
			
			
			if(userRoles != null && !userRoles.isEmpty()) {
				
				prolesIds = 
					getBpmDAO().getResultList(ProcessRole.getProcessInstanceIdsByUserRolesAndUserIdentity, Long.class,
							new Param(ProcessRole.processRoleNameProperty, userRoles),
							new Param(NativeIdentityBind.identityIdProperty, currentUserId),
							new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER.toString())
					);
				
			} else {
				
				prolesIds = 
					getBpmDAO().getResultList(ProcessRole.getProcessInstanceIdsByUserIdentity, Long.class,
							new Param(NativeIdentityBind.identityIdProperty, currentUserId),
							new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER.toString())
					);
				
				
			}
		}
		
		return prolesIds;
	}
	
	public List<ProcessRole> getProcessRolesForProcessInstanceByTaskInstance(Long taskInstanceId) {
		
		JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			long processInstanceId = jctx.getTaskInstance(taskInstanceId).getProcessInstance().getId();
			
			final List<ProcessRole> proles = 
				getBpmDAO().getResultList(ProcessRole.getSetByPIId, ProcessRole.class,
						new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
				);
			
			return proles;
		} finally {
			getIdegaJbpmContext().closeAndCommit(jctx);
		}
	}
	
	protected IWMainApplication getIWMA() {
		
		IWMainApplication iwma;
		FacesContext fctx = FacesContext.getCurrentInstance();
		
		if(fctx == null)
			iwma = IWMainApplication.getDefaultIWMainApplication();
		else
			iwma = IWMainApplication.getIWMainApplication(fctx);
		
		return iwma;
	}
	
	protected AccessController getAccessController() {
		
		return getIWMA().getAccessController();
	}
	
	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	public AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	@Autowired
	public void setAuthorizationService(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	protected UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}