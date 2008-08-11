package com.idega.jbpm.identity;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
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
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMRoleAccessPermission;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.45 $
 * 
 * Last modified: $Date: 2008/08/11 13:33:10 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmRolesManager")
@Transactional(readOnly=true)
public class RolesManagerImpl implements RolesManager {
	
	private BPMDAO bpmDAO;
	private BPMContext idegaJbpmContext;
	private AuthorizationService authorizationService;
	private VariablesHandler variablesHandler;
	
	private static final Logger logger = Logger.getLogger(RolesManagerImpl.class.getName());

	@Transactional(readOnly=false)
	public void createIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId) {
		
		modifyIdentitiesForRoles(roles, identityId, identityType, processInstanceId, false);
	}
	
	@Transactional(readOnly=false)
	public void removeIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId) {
		
		modifyIdentitiesForRoles(roles, identityId, identityType, processInstanceId, true);
	}
	
	@Transactional(readOnly=false)
	void modifyIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId, boolean remove) {
		
		if(roles.isEmpty())
			return;

		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			
			if(role.getScope() == RoleScope.PI)
				rolesNames.add(role.getRoleName());
		}
		
		if(!rolesNames.isEmpty()) {
			
			List<ProcessRole> processRoles = 
				getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
						new Param(ProcessRole.processRoleNameProperty, rolesNames),
						new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
				);
			
			for (ProcessRole role : processRoles) {
				
				List<NativeIdentityBind> identities = role.getNativeIdentities();
				
				boolean contains = false;
				
				for (Iterator<NativeIdentityBind> iterator = identities.iterator(); iterator.hasNext();) {
					NativeIdentityBind nativeIdentityBind = iterator.next();
				
					if(identityType == nativeIdentityBind.getIdentityType() && identityId.equals(nativeIdentityBind.getIdentityId())) {
						
						if(remove) {
							getBpmDAO().remove(nativeIdentityBind);
							iterator.remove();
						}
						
						contains = true;
						break;
					}
				}
				
				if(!remove && !contains) {
				
					NativeIdentityBind nidentity = new NativeIdentityBind();
					nidentity.setIdentityId(identityId);
					nidentity.setIdentityType(identityType);
					nidentity.setProcessRole(getBpmDAO().merge(role));
					getBpmDAO().persist(nidentity);
				}
			}
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
	
	public Collection<User> getAllUsersForRoles(Collection<String> rolesNames, long piId) {
		
		return getAllUsersForRoles(rolesNames, piId, null);
	}
	
	public List<String> getRolesForAccess(long processInstanceId, Access access) {
		
		final String queryName;
		
		switch (access) {
			case caseHandler:
				
				queryName = ProcessRole.getRolesNamesHavingCaseHandlerRights;
				break;
				
			case modifyPermissions:
				queryName = ProcessRole.getRoleNameHavingRightsModifyPermissionByPIId;
	
			default:
				throw new UnsupportedOperationException("Not supported for access="+access+", just add another query");
		}
		
		List<String> rolesNames =
			getBpmDAO().getResultList(queryName, String.class, 
					new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
			);
		
		return rolesNames;
	}
	
	public Collection<User> getAllUsersForRoles(Collection<String> rolesNames, long piId, BPMRoleAccessPermission perm) {

		List<ProcessRole> proles;
		
//		get all roles of contancts
		if(rolesNames == null || rolesNames.isEmpty()) {
		
			proles =
				getBpmDAO().getResultList(ProcessRole.getSetByPIId, ProcessRole.class, 
						new Param(ProcessRole.processInstanceIdProperty, piId)
				);
		} else
			proles =
				getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class, 
						new Param(ProcessRole.processRoleNameProperty, rolesNames),
						new Param(ProcessRole.processInstanceIdProperty, piId)
				);
		
		if(perm != null && proles != null && !proles.isEmpty()) {
//			filtering roles, that the permission doesn't let to see
			
			AuthorizationService authServ = getAuthorizationService();
			
			ArrayList<ProcessRole> filteredProles = new ArrayList<ProcessRole>(proles.size());
			
			for (Iterator<ProcessRole> iterator = proles.iterator(); iterator.hasNext();) {
				ProcessRole processRole = iterator.next();
				perm.setCheckContactsForRole(true);
				perm.setRoleName(processRole.getProcessRoleName());
				perm.setProcessInstanceId(processRole.getProcessInstanceId());
				
				try {
//					check, if current user can see contacts of the role (set to the permission)
					authServ.checkPermission((Permission)perm);
					filteredProles.add(processRole);
				} catch (AccessControlException e) {
				}
			}
			
			proles = filteredProles;
		}
		
		if(proles != null && !proles.isEmpty()) {
			
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
			
			return allUsers.values();
		} else {
			
			@SuppressWarnings("unchecked")
			List<User> empty = Collections.EMPTY_LIST;
			return empty;
		}
	}
	
	public void createNativeRolesFromProcessRoles(String processName, Collection<Role> roles) {
		
		AccessController ac = getAccessController();

		for (Role role : roles) {
			
			if(role.getScope() == RoleScope.PD) {
				ac.checkIfRoleExistsInDataBaseAndCreateIfMissing(role.getRoleName());
			}
		}
	}
	
	@Transactional(readOnly=false)
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
	
	@Transactional(readOnly=false)
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
					perm.setModifyRightsPermission(role.getAccesses().contains(Access.modifyPermissions));
					perm.setCaseHandlerPermission(role.getAccesses().contains(Access.caseHandler));
					
					getBpmDAO().persist(perm);
				}
			}
		}
	}
	
	@Transactional(readOnly=false)
	public List<ActorPermissions> createRolesPermissions(List<Role> roles, long processInstanceId) {
		List<ActorPermissions> createdPermissions = null;
		if(!roles.isEmpty()) {
		
			HashSet<String> rolesNames = new HashSet<String>(roles.size());
			boolean hasContactsPermissions = false;
			
			for (Role role : roles) {
				rolesNames.add(role.getRoleName());
				
				if(!hasContactsPermissions && role.getRolesContacts() != null && !role.getRolesContacts().isEmpty())
					hasContactsPermissions = true;
			}
						
			
			List<ActorPermissions> perms = getBpmDAO().getResultList(ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceIdPureRoles, ActorPermissions.class,
					new Param(ActorPermissions.roleNameProperty, rolesNames),
					new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
			);
			
//			if eq, then all perms are created
			if(perms.size() < rolesNames.size()) {
				
				for (ActorPermissions actorPermissions : perms) {
					roles.remove(actorPermissions.getRoleName());
					rolesNames.remove(actorPermissions.getRoleName());
				}
			
				for (Role role : roles) {
					
					ActorPermissions perm = new ActorPermissions();
					perm.setTaskId(null);
					perm.setRoleName(role.getRoleName());
					perm.setReadPermission(role.getAccesses() != null && role.getAccesses().contains(Access.read));
					perm.setWritePermission(role.getAccesses() != null && role.getAccesses().contains(Access.write));
					perm.setModifyRightsPermission(role.getAccesses() != null && role.getAccesses().contains(Access.modifyPermissions));
					perm.setCaseHandlerPermission(role.getAccesses() != null && role.getAccesses().contains(Access.caseHandler));
					
					getBpmDAO().persist(perm);
					
					if(createdPermissions == null) {
						createdPermissions = new ArrayList<ActorPermissions>();
					}
					createdPermissions.add(perm);
				}
			}
			
			if(hasContactsPermissions) {
				
				List<ActorPermissions> createdContactsPermissions = createRolesContactsPermissions(roles, processInstanceId);
				
				if(createdContactsPermissions != null) {
				
					if(createdPermissions == null)
						createdPermissions = createdContactsPermissions;
					else
						createdPermissions.addAll(createdContactsPermissions);
				}
			}
		}
		return createdPermissions;
	}
	
//	TODO: createRolesContactsPermissions and createRolesPermissions are very similar, DO REFACTORING
	@Transactional(readOnly=false)
	public List<ActorPermissions> createRolesContactsPermissions(List<Role> roles, long processInstanceId) {
		
		List<ActorPermissions> createdPermissions = null;
		if(!roles.isEmpty()) {
		
			HashSet<String> rolesNames = new HashSet<String>(roles.size());
			
			for (Role role : roles) {
				rolesNames.add(role.getRoleName());
			}
						
			//"from ActorPermissions ap, in (ap." + ActorPermissions.processRolesProperty + ") roles where ap."+ActorPermissions.roleNameProperty+" in (:"+ActorPermissions.roleNameProperty+") and roles." + ProcessRole.processInstanceIdProperty + " = :" + ProcessRole.processInstanceIdProperty +" and "+ActorPermissions.taskIdProperty+" is null and "+ActorPermissions.taskInstanceIdProperty+" is null and "+ActorPermissions.canSeeContactsOfRoleNameProperty+" is not null"
//			ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceIdForContacts
			List<ActorPermissions> perms = getBpmDAO().getResultList(ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceIdForContacts,
					ActorPermissions.class,
					new Param(ActorPermissions.roleNameProperty, rolesNames),
					new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
			);
			
//			if eq, then all perms are created
			if(perms.size() < rolesNames.size()) {
				
				for (ActorPermissions actorPermissions : perms) {
					roles.remove(actorPermissions.getRoleName());
					rolesNames.remove(actorPermissions.getRoleName());
				}
			
				for (Role role : roles) {
					
					if(role.getRolesContacts() != null && !role.getRolesContacts().isEmpty()) {
					
						for (String roleContact : role.getRolesContacts()) {
					
							ActorPermissions perm = new ActorPermissions();
							perm.setTaskId(null);
							perm.setRoleName(role.getRoleName());
							perm.setReadPermission(role.getAccesses() != null && role.getAccesses().contains(Access.read));
							perm.setWritePermission(role.getAccesses() != null && role.getAccesses().contains(Access.write));
							perm.setModifyRightsPermission(role.getAccesses() != null && role.getAccesses().contains(Access.modifyPermissions));
							perm.setCaseHandlerPermission(role.getAccesses() != null && role.getAccesses().contains(Access.caseHandler));
							perm.setCanSeeContactsOfRoleName(roleContact);
							
							getBpmDAO().persist(perm);
							
							if(createdPermissions == null) {
								createdPermissions = new ArrayList<ActorPermissions>();
							}
							createdPermissions.add(perm);
						}
					}
				}
			}
		}
		return createdPermissions;
	}
	
	@Transactional(readOnly=true)
	public List<ProcessRole> getProcessRoles(Collection<String> rolesNames, Long processInstanceId) {
		
		List<ProcessRole> proles = 
			getBpmDAO().getResultList(
					ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
					new Param(ProcessRole.processRoleNameProperty, rolesNames),
					new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
			);
		
		return proles;
	}
	
	@Transactional(readOnly=false)
	public void assignTaskRolesPermissions(Task task, List<Role> roles, Long processInstanceId) {
		
		if(roles.isEmpty())
			return;
		
		createTaskRolesPermissions(task, roles);
		
		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			rolesNames.add(role.getRoleName());
		}
		
		List<ProcessRole> proles = getProcessRoles(rolesNames, processInstanceId);
		
		if(proles != null && !proles.isEmpty()) {
		
			for (Iterator<ProcessRole> iterator = proles.iterator(); iterator.hasNext();) {
				
				ProcessRole prole = iterator.next();
				
				List<ActorPermissions> perms = prole.getActorPermissions();
				
				boolean hasPermForTask = false;
				
				if(!perms.isEmpty()) {
					
					for (ActorPermissions actorPermissions : perms) {
						try {
						if(actorPermissions.getTaskId().equals(task.getId()) && actorPermissions.getTaskInstanceId() == null)
							hasPermForTask = true;
						} catch(NullPointerException e) {
							continue;
						}						
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
	}
	
	@Transactional(readOnly=false)
	public void assignRolesPermissions(List<Role> roles, Long processInstanceId) {
		
		if(roles.isEmpty())
			return;
		
		List<ActorPermissions> perms = createRolesPermissions(roles, processInstanceId);
		
		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			rolesNames.add(role.getRoleName());
		}
		
		List<ProcessRole> proles = getProcessRoles(rolesNames, processInstanceId);
		
		if(proles != null && !proles.isEmpty()) {
				
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
			
		} else
			logger.log(Level.WARNING, "No process roles found by roles: "+rolesNames+", processInstanceId: "+processInstanceId);
	}
	
	public List<Long> getProcessInstancesIdsForCurrentUser() {
		IWContext iwc = CoreUtil.getIWContext(); 
		return getProcessInstancesIdsForUser(iwc, iwc.getCurrentUser(), true);
	}
	
	public List<Long> getProcessInstancesIdsForUser(IWContext iwc, User user, boolean checkIfSuperAdmin) {
		final List<Long> processInstancesIds;
		
		if(checkIfSuperAdmin && iwc.isSuperAdmin()) {
			
			processInstancesIds = getBpmDAO().getResultList(ProcessRole.getAllProcessInstancesIds, Long.class);
			
		} else {
			
			final Integer userId;
			try {
				userId = Integer.valueOf(user.getPrimaryKey().toString());
			} catch(NumberFormatException e) {
				throw new RuntimeException(e);
			}
		
			@SuppressWarnings("unchecked")
			Set<String> userRoles = getAccessController().getAllRolesForUser(user);
			
			if(userRoles != null && !userRoles.isEmpty()) {
				
				processInstancesIds = 
					getBpmDAO().getResultList(ProcessRole.getProcessInstanceIdsByUserRolesAndUserIdentity, Long.class,
							new Param(ProcessRole.processRoleNameProperty, userRoles),
							new Param(NativeIdentityBind.identityIdProperty, userId),
							new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER.toString())
					);
				
			} else {
				
				processInstancesIds = 
					getBpmDAO().getResultList(ProcessRole.getProcessInstanceIdsByUserIdentity, Long.class,
							new Param(NativeIdentityBind.identityIdProperty, userId),
							new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER.toString())
					);
			}
		}
		
		if (ListUtil.isEmpty(processInstancesIds)) {
			return null;
		}
		
		return ListUtil.isEmpty(processInstancesIds) ? null : processInstancesIds;
	}
	
	public List<ProcessRole> getProcessRolesForProcessInstanceByTaskInstance(Long processInstanceId, Long taskInstanceId, String processRoleName) {
		
		JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskInstanceProcessInstanceId = jctx.getTaskInstance(taskInstanceId).getProcessInstance().getId();
			
			List<ProcessRole> proles;
			
			if(processInstanceId == null || processInstanceId.equals(taskInstanceProcessInstanceId)) {
				
				if(processRoleName == null) {
				
					proles = 
						getBpmDAO().getResultList(ProcessRole.getSetByPIId, ProcessRole.class,
								new Param(ProcessRole.processInstanceIdProperty, taskInstanceProcessInstanceId)
						);
				} else {

					proles = getBpmDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
							new Param(ProcessRole.processInstanceIdProperty, taskInstanceProcessInstanceId),
							new Param(ProcessRole.processRoleNameProperty, Arrays.asList(new String[] {processRoleName}))
					);
				}
				
			} else {
				
				if(processRoleName == null) {
					
					proles = getBpmDAO().getResultList(ProcessRole.getSetByPIIds, ProcessRole.class,
							new Param(ProcessRole.processInstanceIdProperty, Arrays.asList(new Long[] {taskInstanceProcessInstanceId, processInstanceId}))
					);
					
				} else {
					
					proles = getBpmDAO().getResultList(ProcessRole.getSetByPIIdsAndRoleNames, ProcessRole.class,
							new Param(ProcessRole.processRoleNameProperty, Arrays.asList(new String[] {processRoleName})),
							new Param(ProcessRole.processInstanceIdProperty, Arrays.asList(new Long[] {taskInstanceProcessInstanceId, processInstanceId}))
					);
				}
				
				if(proles != null) {
					
					HashMap<String, ProcessRole> prolesMap = new HashMap<String, ProcessRole>(proles.size());
				
					for (ProcessRole prole : proles) {

//						checking, if role is with taskinstance processinstanceid - that's the preferred. keeping only one prole for role name
						if(taskInstanceProcessInstanceId.equals(prole.getProcessInstanceId()) || !prolesMap.containsKey(prole.getProcessRoleName())) {
						
							prolesMap.put(prole.getProcessRoleName(), prole);
						}
					}
					
					proles = new ArrayList<ProcessRole>(prolesMap.values());
				}
			}
			
			
			return proles;
		} finally {
			getIdegaJbpmContext().closeAndCommit(jctx);
		}
	}
	
	/**
	 * creates or updates task instance scope permissions for role.
	 * @param role - role object, containing role name, and accesses to set
	 * @param taskInstanceId
	 * @param setSameForAttachments - set the same access rights for binary variables of the task instance 
	 * @param variableIdentifier - if provided, set rights for variable for that task instance. This is usually used for task attachments.
	 */
	@Transactional(readOnly = false)
	public void setTaskRolePermissionsTIScope(Role role, Long processInstanceId, Long taskInstanceId, boolean setSameForAttachments, String variableIdentifier) {
	
//		TODO: check permissions if rights can be changed
		String roleName = role.getRoleName();
		
		if(roleName != null && taskInstanceId != null && !CoreConstants.EMPTY.equals(roleName)) {
			
			final JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
			
			try {
				
				TaskInstance ti = jctx.getTaskInstance(taskInstanceId);
				long piId = ti.getProcessInstance().getId();
			
				final List<ProcessRole> proles = getProcessRolesForProcessInstanceByTaskInstance(processInstanceId, taskInstanceId, roleName);
				
				if(proles != null && !proles.isEmpty()) {
					
					ProcessRole prole = proles.iterator().next();
					List<ActorPermissions> perms = prole.getActorPermissions();
					
					List<Access> accesses = role.getAccesses();
					boolean setReadPermission = accesses != null && accesses.contains(Access.read);
					boolean setWritePermission = accesses != null && accesses.contains(Access.write);
					
					ActorPermissions tiPerm = null;
					
					if(variableIdentifier != null && !CoreConstants.EMPTY.equals(variableIdentifier)) {
						
						ActorPermissions aperm = null;
						
						for (ActorPermissions perm : perms) {
							
							if(taskInstanceId.equals(perm.getTaskInstanceId()) && perm.getVariableIdentifier() == null) {
								tiPerm = perm;
							} else if(taskInstanceId.equals(perm.getTaskInstanceId()) && variableIdentifier.equals(perm.getVariableIdentifier())) {
								aperm = perm;
								break;
							}
						}
						
						if(aperm != null) {
							
							aperm.setWritePermission(setWritePermission);
							aperm.setReadPermission(setReadPermission);
							aperm = getBpmDAO().merge(aperm);
							
						} else {
							
//							creating only if there's any permissive permission to be set
							
							aperm = new ActorPermissions();
							aperm.setRoleName(prole.getProcessRoleName());
							aperm.setTaskId(ti.getTask().getId());
							aperm.setTaskInstanceId(taskInstanceId);
							aperm.setVariableIdentifier(variableIdentifier);
							aperm.setReadPermission(setReadPermission);
							aperm.setWritePermission(setWritePermission);
							aperm.setProcessRoles(proles);
							
							getBpmDAO().persist(aperm);
							perms.add(aperm);
						}
					}
					
					if(tiPerm == null) {
						for (ActorPermissions perm : perms) {
							
							if(taskInstanceId.equals(perm.getTaskInstanceId()) && perm.getVariableIdentifier() == null) {
								tiPerm = perm;
								break;
							}
						}
					}
					
					if(variableIdentifier != null) {
						
						setWritePermission = setWritePermission || (tiPerm != null && tiPerm.getWritePermission());
						setReadPermission = setReadPermission || (tiPerm != null && tiPerm.getReadPermission());
					}
					
					if(tiPerm != null) {

						tiPerm.setWritePermission(setWritePermission);
						tiPerm.setReadPermission(setReadPermission);
						
						tiPerm = getBpmDAO().merge(tiPerm);
						
					} else {
						
//						creating only if there's any permissive permission to be set
						
						tiPerm = new ActorPermissions();
						tiPerm.setRoleName(prole.getProcessRoleName());
						tiPerm.setTaskId(ti.getTask().getId());
						tiPerm.setTaskInstanceId(taskInstanceId);
						tiPerm.setVariableIdentifier(null);
						tiPerm.setWritePermission(setWritePermission);
						tiPerm.setReadPermission(setReadPermission);
						tiPerm.setProcessRoles(proles);
						
						getBpmDAO().persist(tiPerm);
						perms.add(tiPerm);
					}
					
//					set for task instance
					
					if(setSameForAttachments && variableIdentifier == null) {

						List<BinaryVariable> binaryVariables = getVariablesHandler().resolveBinaryVariables(taskInstanceId);
						
						if(!binaryVariables.isEmpty()) {
						
							for (ActorPermissions perm : perms) {
								
								if(taskInstanceId.equals(perm.getTaskInstanceId()) && perm.getVariableIdentifier() != null) {

									for (Iterator<BinaryVariable> iterator = binaryVariables.iterator(); iterator.hasNext();) {
										BinaryVariable binVar = iterator.next();
										
										if(perm.getVariableIdentifier().equals(binVar.getHash().toString())) {
											
											perm.setReadPermission(setReadPermission);
											perm.setWritePermission(setWritePermission);
											perm = getBpmDAO().merge(perm);
											iterator.remove();
										}
									}
								}
							}
							
//							creating non existent permissions for vars
							for (BinaryVariable binVar : binaryVariables) {
								
								tiPerm = new ActorPermissions();
								tiPerm.setRoleName(prole.getProcessRoleName());
								tiPerm.setTaskId(ti.getTask().getId());
								tiPerm.setTaskInstanceId(taskInstanceId);
								tiPerm.setVariableIdentifier(binVar.getHash().toString());
								tiPerm.setWritePermission(setWritePermission);
								tiPerm.setReadPermission(setReadPermission);
								tiPerm.setProcessRoles(proles);
								
								getBpmDAO().persist(tiPerm);
								perms.add(tiPerm);
							}
						}
					}
					
					getBpmDAO().merge(prole);
					
				} else {
					logger.log(Level.WARNING, "-RolesManagerImpl.setTaskRolePermissionsTIScope- No process roles found by roleName="+roleName+", processInstanceId="+piId);
				}
				
			} finally {
				getIdegaJbpmContext().closeAndCommit(jctx);
			}
		} else
			logger.log(Level.WARNING, "-RolesManagerImpl.setTaskRolePermissionsTIScope- Insufficient info provided");
	}
	
	public List<Role> getRolesPermissionsForTaskInstance(Long processInstanceId, Long taskInstanceId, String variableIdentifier) {
		
//		TODO: check permissions if rights can be seen
		if(taskInstanceId == null)
			throw new IllegalArgumentException("TaskInstanceId not provided");
		
		JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskId = jctx.getTaskInstance(taskInstanceId).getTask().getId();
			
			List<ProcessRole> proles = getProcessRolesForProcessInstanceByTaskInstance(processInstanceId, taskInstanceId, null);
			
			ArrayList<Role> roles = new ArrayList<Role>(proles.size());
			
			for (ProcessRole prole : proles) {
				
				Role role = new Role();
				role.setRoleName(prole.getProcessRoleName());
				
				List<ActorPermissions> perms = prole.getActorPermissions();
				
				ArrayList<Access> taskInstanceScopeANDVar = null;
				ArrayList<Access> taskInstanceScopeNoVar = null;
				ArrayList<Access> taskScopeANDVar = null;
				ArrayList<Access> taskScopeNoVar = null;
				
				for (ActorPermissions perm : perms) {

					if (taskInstanceId.equals(perm.getTaskInstanceId()) && variableIdentifier != null && variableIdentifier.equals(perm.getVariableIdentifier())) {
						
						taskInstanceScopeANDVar = new ArrayList<Access>(2);
					
						if(perm.getWritePermission() != null && perm.getWritePermission()) {
						
							taskInstanceScopeANDVar.add(Access.write);
						}
						if(perm.getReadPermission() != null && perm.getReadPermission()) {
							
							taskInstanceScopeANDVar.add(Access.read);
						}
						
						break;
						
					} else if (taskInstanceId.equals(perm.getTaskInstanceId()) && perm.getVariableIdentifier() == null) {
						
						taskInstanceScopeNoVar = new ArrayList<Access>(2);

						if(perm.getWritePermission() != null && perm.getWritePermission()) {
							
							taskInstanceScopeNoVar.add(Access.write);
						}
						if(perm.getReadPermission() != null && perm.getReadPermission()) {
							
							taskInstanceScopeNoVar.add(Access.read);
						}
						
						if(variableIdentifier == null)
							break;
							
					} else if (taskId.equals(perm.getTaskId()) && perm.getTaskInstanceId() == null && variableIdentifier != null && variableIdentifier.equals(perm.getVariableIdentifier())) {
						
						taskScopeANDVar = new ArrayList<Access>(2);
						
						if(perm.getWritePermission() != null && perm.getWritePermission()) {
							
							taskScopeANDVar.add(Access.write);
						}
						if(perm.getReadPermission() != null && perm.getReadPermission()) {
							
							taskScopeANDVar.add(Access.read);
						}
						
					} else if (taskId.equals(perm.getTaskId()) && perm.getTaskInstanceId() == null && perm.getVariableIdentifier() == null) {
						
						taskScopeNoVar = new ArrayList<Access>(2);
						
						if(perm.getWritePermission() != null && perm.getWritePermission()) {
							
							taskScopeNoVar.add(Access.write);
						}
						if(perm.getReadPermission() != null && perm.getReadPermission()) {
							
							taskScopeNoVar.add(Access.read);
						}
					}
				}
				
				if(variableIdentifier != null) {
					
					if(taskInstanceScopeANDVar != null) {
						role.setAccesses(taskInstanceScopeANDVar);
					} else if(taskScopeANDVar != null) {
						role.setAccesses(taskScopeANDVar);
					} else if(taskInstanceScopeNoVar != null) {
						role.setAccesses(taskInstanceScopeNoVar);
					} else if(taskScopeNoVar != null) {
						role.setAccesses(taskScopeNoVar);
					}
					
				} else {
					
					if(taskInstanceScopeNoVar != null) {
						role.setAccesses(taskInstanceScopeNoVar);
					} else if(taskScopeNoVar != null) {
						role.setAccesses(taskScopeNoVar);
					}
				}
				
				roles.add(role);
			}
			
			return roles;
			
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
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
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

	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	@Autowired
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
}