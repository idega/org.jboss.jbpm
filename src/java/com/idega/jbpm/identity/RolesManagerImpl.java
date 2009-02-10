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

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.HashMultimap;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.persistence.Param;
import com.idega.data.IDORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.identity.permission.RoleAccessPermissionsHandler;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * <p>
 * No synchornizations or constraints are put, so the entries might duplicate on race condition.
 * Yet, in all cases (afaik) extra entries, that could happen, don't do any real harm. TODO: but
 * noone said it would be nice to fix that.
 * </p>
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.60 $ Last modified: $Date: 2009/02/10 12:25:23 $ by $Author: juozas $
 */
@Scope("singleton")
@Service("bpmRolesManager")
@Transactional(readOnly = true, noRollbackFor = { AccessControlException.class,
        BPMAccessControlException.class })
public class RolesManagerImpl implements RolesManager {
	
	@Autowired
	private BPMDAO bpmDAO;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	AuthorizationService authorizationService;
	
	@Autowired
	private VariablesHandler variablesHandler;
	@Autowired
	private PermissionsFactory permissionsFactory;
	
	private static final Logger logger = Logger
	        .getLogger(RolesManagerImpl.class.getName());
	
	@Transactional(readOnly = false)
	public void createIdentitiesForRoles(Collection<Role> roles,
	        String identityId, IdentityType identityType, long processInstanceId) {
		
		modifyIdentitiesForRoles(roles, identityId, identityType,
		    processInstanceId, false);
	}
	
	@Transactional(readOnly = false)
	public void removeIdentitiesForRoles(Collection<Role> roles,
	        String identityId, IdentityType identityType, long processInstanceId) {
		
		modifyIdentitiesForRoles(roles, identityId, identityType,
		    processInstanceId, true);
	}
	
	@Transactional(readOnly = false)
	public void removeIdentitiesForActors(Collection<Actor> actors,
	        String identityId, IdentityType identityType, long processInstanceId) {
		modifyIdentitiesForActors(actors, identityId, identityType,
		    processInstanceId, true);
	}
	
	@Transactional(readOnly = false)
	public void createIdentitiesForActors(Collection<Actor> actors,
	        String identityId, IdentityType identityType, long processInstanceId) {
		modifyIdentitiesForActors(actors, identityId, identityType,
		    processInstanceId, false);
	}
	
	@Transactional(readOnly = false)
	void modifyIdentitiesForActors(Collection<Actor> actors, String identityId,
	        IdentityType identityType, long processInstanceId, boolean remove) {
		
		for (Actor actor : actors) {
			
			List<NativeIdentityBind> identities = actor.getNativeIdentities();
			
			boolean contains = false;
			
			if (identities != null) {
				
				for (Iterator<NativeIdentityBind> iterator = identities
				        .iterator(); iterator.hasNext();) {
					NativeIdentityBind nativeIdentityBind = iterator.next();
					
					if (identityType == nativeIdentityBind.getIdentityType()
					        && identityId.equals(nativeIdentityBind
					                .getIdentityId())) {
						
						if (remove) {
							getBpmDAO().remove(nativeIdentityBind);
							iterator.remove();
						}
						
						contains = true;
						break;
					}
				}
			}
			
			if (!remove && !contains) {
				
				NativeIdentityBind nidentity = new NativeIdentityBind();
				nidentity.setIdentityId(identityId);
				nidentity.setIdentityType(identityType);
				actor = getBpmDAO().merge(actor);
				nidentity.setActor(actor);
				getBpmDAO().persist(nidentity);
				
				if (identities == null) {
					
					identities = new ArrayList<NativeIdentityBind>(1);
					actor.setNativeIdentities(identities);
				}
				
				identities.add(nidentity);
				getBpmDAO().persist(actor);
			}
		}
	}
	
	@Transactional(readOnly = false)
	void modifyIdentitiesForRoles(Collection<Role> roles, String identityId,
	        IdentityType identityType, long processInstanceId, boolean remove) {
		
		if (roles.isEmpty())
			return;
		
		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			
			if (role.getScope() == RoleScope.PI)
				rolesNames.add(role.getRoleName());
		}
		
		if (!rolesNames.isEmpty()) {
			
			List<Actor> actors = getBpmDAO().getResultList(
			    Actor.getSetByRoleNamesAndPIId, Actor.class,
			    new Param(Actor.processRoleNameProperty, rolesNames),
			    new Param(Actor.processInstanceIdProperty, processInstanceId));
			
			modifyIdentitiesForActors(actors, identityId,
		        identityType, processInstanceId, remove);
		}
	}
	
	public void hasRightsToStartTask(final long taskInstanceId, final int userId)
	        throws BPMAccessControlException {
		
		try {
			getBpmContext().execute(new JbpmCallback() {
				
				public Object doInJbpm(JbpmContext context)
				        throws JbpmException {
					TaskInstance taskInstance = context
					        .getTaskInstance(taskInstanceId);
					
					if (taskInstance.getStart() != null
					        || taskInstance.hasEnded())
						throw new BPMAccessControlException(
						        "Task ("
						                + taskInstanceId
						                + ") has already been started, or has already ended",
						        "Task has already been started, or has already ended");
					
					if (taskInstance.getActorId() == null
					        || !taskInstance.getActorId().equals(
					            String.valueOf(userId)))
						throw new BPMAccessControlException(
						        "User ("
						                + userId
						                + ") tried to start task, but not assigned to the user provided. Assigned: "
						                + taskInstance.getActorId(),
						        "User should be taken or assigned of the task first to start working on it");
					
					Permission permission = getPermissionsFactory()
					        .getTaskInstanceSubmitPermission(false,
					            taskInstance);
					getAuthorizationService().checkPermission(permission);
					return null;
				}
			});
			
		} catch (BPMAccessControlException e) {
			throw e;
		} catch (AccessControlException e) {
			throw new BPMAccessControlException(
			        "User has no access to modify this task");
		}
	}
	
	public void checkPermission(Permission permission)
	        throws BPMAccessControlException {
		
		try {
			getAuthorizationService().checkPermission(permission);
			
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("No access");
		}
	}
	
	public void hasRightsToAssignTask(final long taskInstanceId,
	        final int userId) throws BPMAccessControlException {
		
		try {
			getBpmContext().execute(new JbpmCallback() {
				
				public Object doInJbpm(JbpmContext context)
				        throws JbpmException {
					TaskInstance taskInstance = context
					        .getTaskInstance(taskInstanceId);
					
					if (taskInstance.getStart() != null
					        || taskInstance.hasEnded())
						throw new BPMAccessControlException(
						        "Task ("
						                + taskInstanceId
						                + ") has already been started, or has already ended",
						        "Task has been started by someone, or has ended.");
					
					if (taskInstance.getActorId() != null)
						throw new BPMAccessControlException("Task ("
						        + taskInstanceId + ") is already assigned to: "
						        + taskInstance.getActorId(),
						        "This task has been assigned already");
					
					Permission permission = getPermissionsFactory()
					        .getTaskInstanceSubmitPermission(false,
					            taskInstance);
					getAuthorizationService().checkPermission(permission);
					return null;
				}
			});
			
		} catch (BPMAccessControlException e) {
			throw e;
		} catch (AccessControlException e) {
			throw new BPMAccessControlException(
			        "User has no access to modify this task");
		}
	}
	
	public Collection<User> getAllUsersForRoles(Collection<String> rolesNames,
	        long piId) {
		
		return getAllUsersForRoles(rolesNames, piId, null);
	}
	
	public List<String> getRolesForAccess(long processInstanceId, Access access) {
		
		final String queryName;
		
		switch (access) {
			case caseHandler:

				queryName = Actor.getRolesNamesHavingCaseHandlerRights;
				break;
			
			case modifyPermissions:
				queryName = Actor.getRoleNameHavingRightsModifyPermissionByPIId;
				
			default:
				throw new UnsupportedOperationException(
				        "Not supported for access=" + access
				                + ", just add another query");
		}
		
		List<String> rolesNames = getBpmDAO().getResultList(queryName,
		    String.class,
		    new Param(Actor.processInstanceIdProperty, processInstanceId));
		
		return rolesNames;
	}
	
	public List<Role> getUserRoles(long processInstanceId, User user) {
		
		List<Actor> actors = getBpmDAO().getResultList(
		    Actor.getActorsByUserIdentityAndProcessInstanceId,
		    Actor.class,
		    new Param(Actor.processInstanceIdProperty, processInstanceId),
		    new Param(NativeIdentityBind.identityTypeProperty,
		            IdentityType.USER),
		    new Param(NativeIdentityBind.identityIdProperty, user
		            .getPrimaryKey().toString()));
		
		if (actors != null) {
			
			ArrayList<Role> roles = new ArrayList<Role>(actors.size());
			
			for (Actor actor : actors) {
				
				Role role = new Role(actor.getProcessRoleName());
				role.setScope(RoleScope.PI);
				roles.add(role);
			}
			
			return roles;
		}
		
		return null;
	}
	
	public Collection<User> getAllUsersForRoles(Collection<String> rolesNames,
	        long piId, BPMTypedPermission perm) {
		
		List<Actor> actors;
		
		// get all roles of contancts
		if (rolesNames == null || rolesNames.isEmpty()) {
			
			actors = getBpmDAO().getResultList(
			    Actor.getSetByPIIdHavingRoleName, Actor.class,
			    new Param(Actor.processInstanceIdProperty, piId));
		} else {
			
			actors = getBpmDAO().getResultList(Actor.getSetByRoleNamesAndPIId,
			    Actor.class,
			    new Param(Actor.processRoleNameProperty, rolesNames),
			    new Param(Actor.processInstanceIdProperty, piId));
		}
		
		if (perm != null && actors != null && !actors.isEmpty()) {
			// filtering roles, that the permission doesn't let to see
			
			AuthorizationService authServ = getAuthorizationService();
			
			ArrayList<Actor> filteredActors = new ArrayList<Actor>(actors
			        .size());
			
			for (Iterator<Actor> iterator = actors.iterator(); iterator
			        .hasNext();) {
				Actor processRole = iterator.next();
				perm.setAttribute(
				    RoleAccessPermissionsHandler.processInstanceIdAtt,
				    processRole.getProcessInstanceId());
				perm.setAttribute(RoleAccessPermissionsHandler.roleNameAtt,
				    processRole.getProcessRoleName());
				perm.setAttribute(
				    RoleAccessPermissionsHandler.checkContactsForRoleAtt, true);
				
				try {
					// check the permission provided, for instance, if current
					// user can see contacts of the role
					authServ.checkPermission((Permission) perm);
					filteredActors.add(processRole);
				} catch (AccessControlException e) {
				}
			}
			
			actors = filteredActors;
		}
		
		if (actors != null && !actors.isEmpty()) {
			
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			UserBusiness userBusiness = getUserBusiness();
			
			ArrayList<Group> allGroups = new ArrayList<Group>();
			HashMap<String, User> allUsers = new HashMap<String, User>();
			
			for (Actor actor : actors) {
				
				if (actor.getNativeIdentities() == null
				        || actor.getNativeIdentities().isEmpty()) {
					
					@SuppressWarnings("unchecked")
					Collection<Group> grps = getAccessController()
					        .getAllGroupsForRoleKey(actor.getProcessRoleName(),
					            iwac);
					
					if (grps != null)
						allGroups.addAll(grps);
					
				} else {
					
					try {
						
						for (NativeIdentityBind identity : actor
						        .getNativeIdentities()) {
							
							if (identity.getIdentityType() == IdentityType.USER) {
								
								User user = userBusiness.getUser(new Integer(
								        identity.getIdentityId()));
								allUsers.put(user.getPrimaryKey().toString(),
								    user);
								
							} else if (identity.getIdentityType() == IdentityType.GROUP) {
								
								@SuppressWarnings("unchecked")
								Collection<User> groupUsers = userBusiness
								        .getUsersInGroup(new Integer(identity
								                .getIdentityId()));
								
								for (User user : groupUsers)
									allUsers.put(user.getPrimaryKey()
									        .toString(), user);
							}
						}
						
					} catch (RemoteException e) {
						logger
						        .log(
						            Level.SEVERE,
						            "Exception while loading users from nativeIdentities",
						            e);
					}
				}
			}
			
			try {
				for (Group group : allGroups) {
					
					@SuppressWarnings("unchecked")
					Collection<User> users = userBusiness
					        .getUsersInGroup(group);
					
					for (User user : users) {
						allUsers.put(user.getPrimaryKey().toString(), user);
					}
				}
				
			} catch (RemoteException e) {
				Logger
				        .getLogger(getClass().getName())
				        .log(
				            Level.SEVERE,
				            "Exception while resolving users from roles assigned groups",
				            e);
			}
			
			return allUsers.values();
		} else {
			
			return Collections.emptyList();
		}
	}
	
	public void createNativeRolesFromProcessRoles(String processName,
	        Collection<Role> roles) {
		
		AccessController ac = getAccessController();
		
		for (Role role : roles) {
			
			if (role.getScope() == RoleScope.PD) {
				ac.checkIfRoleExistsInDataBaseAndCreateIfMissing(role
				        .getRoleName());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.idega.jbpm.identity.RolesManager#createProcessActors(java.util.Collection, org.jbpm.graph.exe.ProcessInstance)
	 */
	@Transactional(readOnly = false)
	public List<Actor> createProcessActors(Collection<Role> roles,
	        ProcessInstance processInstance) {
		
		HashSet<String> rolesNamesToCreate = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			
			rolesNamesToCreate.add(role.getRoleName());
		}
		
		final List<Actor> processActors;
		
		if (!rolesNamesToCreate.isEmpty()) {
			
			processActors = new ArrayList<Actor>();
			
			List<Actor> existingActors = getBpmDAO().getResultList(
			    Actor.getSetByRoleNamesAndPIId,
			    Actor.class,
			    new Param(Actor.processRoleNameProperty, rolesNamesToCreate),
			    new Param(Actor.processInstanceIdProperty, processInstance
			            .getId()));
			
			// removing from "to create" list roles, that already have actors
			for (Actor existingActor : existingActors) {
				
				if (rolesNamesToCreate.contains(existingActor
				        .getProcessRoleName()))
					rolesNamesToCreate.remove(existingActor
					        .getProcessRoleName());
				
				// processActors.add(existingActor);
			}
			
			if (!rolesNamesToCreate.isEmpty()) {
				
				// creating actors for roles
				
				String processName = processInstance.getProcessDefinition()
				        .getName();
				
				Long processInstanceId = processInstance.getId();
				
				for (String actorRole : rolesNamesToCreate) {
					
					Actor actor = new Actor();
					actor.setProcessName(processName);
					actor.setProcessRoleName(actorRole);
					actor.setProcessInstanceId(processInstanceId);
					
					getBpmDAO().persist(actor);
					processActors.add(actor);
				}
			}
			
		} else
			processActors = Collections.emptyList();
		
		return processActors;
	}
	
	@Transactional(readOnly = false)
	public void createTaskRolesPermissions(Task task, List<Role> roles) {
		
		if (!roles.isEmpty()) {
			
			HashSet<String> rolesNames = new HashSet<String>(roles.size());
			
			for (Role role : roles) {
				rolesNames.add(role.getRoleName());
			}
			
			List<ActorPermissions> perms = getBpmDAO().getResultList(
			    ActorPermissions.getSetByTaskIdAndProcessRoleNames,
			    ActorPermissions.class,
			    new Param(ActorPermissions.taskIdProperty, task.getId()),
			    new Param(ActorPermissions.roleNameProperty, rolesNames));
			
			// if eq, then all perms are created
			if (perms.size() < rolesNames.size()) {
				
				for (ActorPermissions actorPermissions : perms) {
					roles.remove(actorPermissions.getRoleName());
					rolesNames.remove(actorPermissions.getRoleName());
				}
				
				logger.log(Level.INFO, "Creating permissions for task: "
				        + task.getId() + ", for roles: " + rolesNames);
				
				for (Role role : roles) {
					
					ActorPermissions perm = new ActorPermissions();
					perm.setTaskId(task.getId());
					perm.setRoleName(role.getRoleName());
					perm.setReadPermission(role.getAccesses().contains(
					    Access.read));
					perm.setWritePermission(role.getAccesses().contains(
					    Access.write));
					perm.setModifyRightsPermission(role.getAccesses().contains(
					    Access.modifyPermissions));
					perm.setCaseHandlerPermission(role.getAccesses().contains(
					    Access.caseHandler));
					
					getBpmDAO().persist(perm);
				}
			}
		}
	}
	
	@Transactional(readOnly = false)
	public List<ActorPermissions> createRolesPermissions(List<Role> roles,
	        ProcessInstance processInstance) {
		
		List<ActorPermissions> createdPermissions = null;
		
		if (!roles.isEmpty()) {
			
			HashSet<String> rolesNames = new HashSet<String>(roles.size());
			boolean hasContactsPermissions = false;
			
			for (Role role : roles) {
				rolesNames.add(role.getRoleName());
				
				if (!hasContactsPermissions && role.getRolesContacts() != null
				        && !role.getRolesContacts().isEmpty())
					hasContactsPermissions = true;
			}
			
			for (Role role : roles) {
				
				ActorPermissions perm = new ActorPermissions();
				perm.setRoleName(role.getRoleName());
				perm.setReadPermission(role.getAccesses() != null
				        && role.getAccesses().contains(Access.read));
				perm.setWritePermission(role.getAccesses() != null
				        && role.getAccesses().contains(Access.write));
				perm.setModifyRightsPermission(role.getAccesses() != null
				        && role.getAccesses()
				                .contains(Access.modifyPermissions));
				perm.setCaseHandlerPermission(role.getAccesses() != null
				        && role.getAccesses().contains(Access.caseHandler));
				
				getBpmDAO().persist(perm);
				
				if (createdPermissions == null) {
					createdPermissions = new ArrayList<ActorPermissions>();
				}
				createdPermissions.add(perm);
			}
			
			if (hasContactsPermissions) {
				
				List<ActorPermissions> createdContactsPermissions = createRolesContactsPermissions(
				    roles, processInstance);
				
				if (createdContactsPermissions != null) {
					
					if (createdPermissions == null)
						createdPermissions = createdContactsPermissions;
					else
						createdPermissions.addAll(createdContactsPermissions);
				}
			}
		}
		return createdPermissions;
	}
	
	// TODO: createRolesContactsPermissions and createRolesPermissions are very
	// similar, DO REFACTORING
	@Transactional(readOnly = false)
	public List<ActorPermissions> createRolesContactsPermissions(
	        List<Role> roles, ProcessInstance processInstance) {
		
		List<ActorPermissions> createdPermissions = null;
		
		if (!roles.isEmpty()) {
			
			HashSet<String> rolesNames = new HashSet<String>(roles.size());
			
			for (Role role : roles) {
				rolesNames.add(role.getRoleName());
			}
			
			for (Role role : roles) {
				
				if (role.getRolesContacts() != null
				        && !role.getRolesContacts().isEmpty()) {
					
					ArrayList<Role> contactsRolesToCreate = new ArrayList<Role>(
					        role.getRolesContacts().size());
					
					for (String roleContact : role.getRolesContacts()) {
						
						ActorPermissions perm = new ActorPermissions();
						perm.setRoleName(role.getRoleName());
						perm.setCanSeeContacts(role.getAccesses() != null
						        && role.getAccesses().contains(
						            Access.seeContacts));
						perm.setCanSeeContactsOfRoleName(roleContact);
						
						getBpmDAO().persist(perm);
						
						if (createdPermissions == null) {
							createdPermissions = new ArrayList<ActorPermissions>();
						}
						createdPermissions.add(perm);
						
						if (!"all".equals(roleContact))
							contactsRolesToCreate.add(new Role(roleContact));
					}
					
					if (!contactsRolesToCreate.isEmpty())
						createProcessActors(contactsRolesToCreate,
						    processInstance);
				}
			}
		}
		return createdPermissions;
	}
	
	@Transactional(readOnly = false)
	public void assignTaskRolesPermissions(Task task, List<Role> roles,
	        Long processInstanceId) {
		
		if (roles.isEmpty())
			return;
		
		createTaskRolesPermissions(task, roles);
		
		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			rolesNames.add(role.getRoleName());
		}
		
		List<Actor> actorsForRoles = getProcessRoles(rolesNames,
		    processInstanceId);
		
		if (actorsForRoles != null && !actorsForRoles.isEmpty()) {
			
			for (Iterator<Actor> iterator = actorsForRoles.iterator(); iterator
			        .hasNext();) {
				
				Actor actorForRole = iterator.next();
				
				List<ActorPermissions> actorPerms = actorForRole
				        .getActorPermissions();
				
				if (actorPerms != null && !actorPerms.isEmpty()) {
					
					// look, if this actor has permission for the task (and it is not a task
					// instance permission). If true, then we remove the actor from actors list of
					// further processing
					
					Long taskId = task.getId();
					
					for (ActorPermissions actorPerm : actorPerms) {
						
						if (taskId.equals(actorPerm.getTaskId())
						        && actorPerm.getTaskInstanceId() == null) {
							
							iterator.remove();
						}
					}
				}
			}
			
			if (!actorsForRoles.isEmpty()) {
				
				// actors, that don't have task permissions, will be assigned with them here
				
				List<ActorPermissions> perms = getBpmDAO().getResultList(
				    ActorPermissions.getSetByTaskIdAndProcessRoleNames,
				    ActorPermissions.class,
				    new Param(ActorPermissions.taskIdProperty, task.getId()),
				    new Param(ActorPermissions.roleNameProperty, rolesNames));
				
				for (Actor actorToAssignPermissionTo : actorsForRoles) {
					
					for (Iterator<ActorPermissions> iterator = perms.iterator(); iterator
					        .hasNext();) {
						
						ActorPermissions perm = iterator.next();
						
						if (actorToAssignPermissionTo.getProcessRoleName()
						        .equals(perm.getRoleName())) {
							List<ActorPermissions> rolePerms = actorToAssignPermissionTo
							        .getActorPermissions();
							
							if (rolePerms == null) {
								rolePerms = new ArrayList<ActorPermissions>();
								actorToAssignPermissionTo
								        .setActorPermissions(rolePerms);
							}
							
							rolePerms.add(perm);
							
							// TODO: I commented this out 0117, does this break something? Looked
							// like bug
							// iterator.remove();
						}
					}
				}
			}
			
		} else
			logger.log(Level.WARNING, "No process roles found by roles: "
			        + rolesNames + ", processInstanceId: " + processInstanceId);
	}
	
	@Transactional(readOnly = false)
	public void assignRolesPermissions(List<Role> roles,
	        ProcessInstance processInstance) {
		
		if (roles.isEmpty())
			return;
		
		List<ActorPermissions> rolesPermissions = createRolesPermissions(roles,
		    processInstance);
		
		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			rolesNames.add(role.getRoleName());
		}
		
		List<Actor> rolesActors = getBpmDAO().getProcessRoles(rolesNames,
		    processInstance.getId());
		
		if (rolesActors != null && !rolesActors.isEmpty()) {
			
			for (Actor roleActor : rolesActors) {
				
				for (Iterator<ActorPermissions> iterator = rolesPermissions
				        .iterator(); iterator.hasNext();) {
					
					ActorPermissions perm = iterator.next();
					
					if (roleActor.getProcessRoleName().equals(
					    perm.getRoleName())) {
						List<ActorPermissions> rolePerms = roleActor
						        .getActorPermissions();
						
						if (rolePerms == null) {
							rolePerms = new ArrayList<ActorPermissions>();
							roleActor.setActorPermissions(rolePerms);
						}
						
						rolePerms.add(perm);
						
						// TODO: I commented this out 0117, does this break something? Looked like
						// bug
						// iterator.remove();
					}
				}
			}
			
		} else
			logger.log(Level.WARNING, "No process roles found by roles: "
			        + rolesNames + ", processInstanceId: "
			        + processInstance.getId());
	}
	
	public List<Long> getProcessInstancesIdsForUser(IWContext iwc, User user,
	        boolean checkIfSuperAdmin) {
		
		List<Long> processInstancesIds;
		
		if (checkIfSuperAdmin && iwc.isSuperAdmin()) {
			
			processInstancesIds = getBpmDAO().getResultList(
			    Actor.getAllProcessInstancesIdsHavingRoleName, Long.class);
			
		} else {
			
			final String userId;
			try {
				userId = user.getPrimaryKey().toString();
			} catch (NumberFormatException e) {
				throw new RuntimeException(e);
			}
			
			Set<String> userRoles = getAccessController().getAllRolesForUser(
			    user);
			
			if (userRoles != null && !userRoles.isEmpty()) {
				
				processInstancesIds = getBpmDAO().getResultList(
				    Actor.getProcessInstanceIdsByUserRolesAndUserIdentity,
				    Long.class,
				    new Param(Actor.processRoleNameProperty, userRoles),
				    new Param(NativeIdentityBind.identityIdProperty, userId),
				    new Param(NativeIdentityBind.identityTypeProperty,
				            NativeIdentityBind.IdentityType.USER.toString()));
				
			} else {
				
				processInstancesIds = getBpmDAO().getResultList(
				    Actor.getProcessInstanceIdsByUserIdentity,
				    Long.class,
				    new Param(NativeIdentityBind.identityIdProperty, userId),
				    new Param(NativeIdentityBind.identityTypeProperty,
				            NativeIdentityBind.IdentityType.USER));
			}
		}
		
		if (processInstancesIds == null)
			processInstancesIds = Collections.emptyList();
		
		return processInstancesIds;
	}
	
	public List<Actor> getProcessRolesForProcessInstanceByTaskInstance(
	        Long processInstanceId, final Long taskInstanceId,
	        String processRoleName) {
		
		// taskInstanceProcessInstanceId might not match with processInstanceId
		// because the task instance could be of the subprocess, taking
		// priorities here
		Long processInstanceIdFromTaskInstance = getBpmContext().execute(
		    new JbpmCallback() {
			    
			    public Object doInJbpm(JbpmContext context)
			            throws JbpmException {
				    return context.getTaskInstance(taskInstanceId)
				            .getProcessInstance().getId();
			    }
		    });
		
		List<Actor> proles;
		
		if (processInstanceId == null
		        || processInstanceId.equals(processInstanceIdFromTaskInstance)) {
			
			if (processRoleName == null) {
				
				proles = getBpmDAO().getResultList(
				    Actor.getSetByPIIdHavingRoleName,
				    Actor.class,
				    new Param(Actor.processInstanceIdProperty,
				            processInstanceIdFromTaskInstance));
			} else {
				
				proles = getBpmDAO().getResultList(
				    Actor.getSetByRoleNamesAndPIId,
				    Actor.class,
				    new Param(Actor.processInstanceIdProperty,
				            processInstanceIdFromTaskInstance),
				    new Param(Actor.processRoleNameProperty, Arrays
				            .asList(new String[] { processRoleName })));
			}
			
		} else {
			
			if (processRoleName == null) {
				
				proles = getBpmDAO().getResultList(
				    Actor.getSetByPIIdsHavingRoleName,
				    Actor.class,
				    new Param(Actor.processInstanceIdProperty, Arrays
				            .asList(new Long[] {
				                    processInstanceIdFromTaskInstance,
				                    processInstanceId })));
				
			} else {
				
				proles = getBpmDAO().getResultList(
				    Actor.getSetByPIIdsAndRoleNames,
				    Actor.class,
				    new Param(Actor.processRoleNameProperty, Arrays
				            .asList(new String[] { processRoleName })),
				    new Param(Actor.processInstanceIdProperty, Arrays
				            .asList(new Long[] {
				                    processInstanceIdFromTaskInstance,
				                    processInstanceId })));
			}
			
			if (proles != null) {
				
				HashMap<String, Actor> prolesMap = new HashMap<String, Actor>(
				        proles.size());
				
				for (Actor prole : proles) {
					
					// checking, if role is with taskinstance processinstanceid
					// - that's the preferred. keeping only one prole for role
					// name
					if (processInstanceIdFromTaskInstance.equals(prole
					        .getProcessInstanceId())
					        || !prolesMap.containsKey(prole
					                .getProcessRoleName())) {
						
						prolesMap.put(prole.getProcessRoleName(), prole);
					}
				}
				
				proles = new ArrayList<Actor>(prolesMap.values());
			}
		}
		
		return proles;
	}
	
	/**
	 * creates or updates task instance scope permissions for role.
	 * 
	 * @param role
	 *            - role object, containing role name, and accesses to set
	 * @param taskInstanceId
	 * @param setSameForAttachments
	 *            - set the same access rights for binary variables of the task instance
	 * @param variableIdentifier
	 *            - if provided, set rights for variable for that task instance. This is usually
	 *            used for task attachments.
	 */
	@Transactional(readOnly = false)
	public void setTaskRolePermissionsTIScope(final Role role,
	        final Long taskInstanceId, final boolean setSameForAttachments,
	        final String variableIdentifier) {
		
		// TODO: check permissions if rights can be changed
		final String roleName = role.getRoleName();
		
		if (roleName != null &&taskInstanceId != null
				&& roleName.length() != 0) {
			
			List<Actor> actorsToSetPermissionsTo = getBpmContext().execute(new JbpmCallback() {
				
				public Object doInJbpm(JbpmContext context)
				        throws JbpmException {
					
					TaskInstance ti = context.getTaskInstance(taskInstanceId);
					long processInstanceId = ti.getProcessInstance().getId();
					
					final List<Actor> actorsToSetPermissionsTo = getProcessRolesForProcessInstanceByTaskInstance(
					    processInstanceId, taskInstanceId, roleName);
					return actorsToSetPermissionsTo;
				}
			});
			
			setTaskPermissionsTIScopeForActors(actorsToSetPermissionsTo, role.getAccesses(), 
				taskInstanceId, setSameForAttachments, variableIdentifier);
		} else
			logger
			        .log(Level.WARNING,
			            "-RolesManagerImpl.setTaskRolePermissionsTIScope- Insufficient info provided");
	}
	
	/**
	 * creates or updates task instance scope permissions for Actors.
	 * 
	 * @param actorsToSetPermissionsTo- Actor objects
	 * @param accesses - accesses to set for actors
	 *       
	 * @param taskInstanceId
	 * @param setSameForAttachments
	 *            - set the same access rights for binary variables of the task instance
	 * @param variableIdentifier
	 *            - if provided, set rights for variable for that task instance. This is usually
	 *            used for task attachments.
	 */
	@Transactional(readOnly = false)
	public void setTaskPermissionsTIScopeForActors(
	        final List<Actor> actorsToSetPermissionsTo,
	        final List<Access> accesses, final Long taskInstanceId,
	        final boolean setSameForAttachments, final String variableIdentifier) {
		
		// TODO: check permissions if rights can be changed
		
		if (taskInstanceId != null) {
			
			getBpmContext().execute(new JbpmCallback() {
				
				public Object doInJbpm(JbpmContext context)
				        throws JbpmException {
					
					TaskInstance ti = context.getTaskInstance(taskInstanceId);
					long processInstanceId = ti.getProcessInstance().getId();
					
					if (!ListUtil.isEmpty(actorsToSetPermissionsTo)) {
						for( Actor actorToSetPermissionTo:actorsToSetPermissionsTo){
						/*Actor actorToSetPermissionTo = actorsToSetPermissionsTo
						        .iterator().next();*/
						List<ActorPermissions> perms = actorToSetPermissionTo
						        .getActorPermissions();
						if(ListUtil.isEmpty(perms)){
							perms = new ArrayList<ActorPermissions>(1);
							actorToSetPermissionTo.setActorPermissions(perms);
						}
						boolean setReadPermission = accesses != null
						        && accesses.contains(Access.read);
						boolean setWritePermission = accesses != null
						        && accesses.contains(Access.write);
						
						ActorPermissions tiPerm = null;
						
						if (!StringUtil.isEmpty(variableIdentifier)) {
							
							ActorPermissions aperm = null;
							
							for (ActorPermissions perm : perms) {
								
								if (taskInstanceId.equals(perm
								        .getTaskInstanceId())
								        && perm.getVariableIdentifier() == null) {
									tiPerm = perm;
								} else if (taskInstanceId.equals(perm
								        .getTaskInstanceId())
								        && variableIdentifier.equals(perm
								                .getVariableIdentifier())) {
									aperm = perm;
									break;
								}
							}
							
							if (aperm != null) {
								
								aperm.setWritePermission(setWritePermission);
								aperm.setReadPermission(setReadPermission);
								aperm = getBpmDAO().merge(aperm);
								
							} else {
								
								// creating only if there's any permissive
								// permission to be set
								
								aperm = new ActorPermissions();
								aperm.setRoleName(actorToSetPermissionTo
								        .getProcessRoleName());
								aperm.setTaskId(ti.getTask().getId());
								aperm.setTaskInstanceId(taskInstanceId);
								aperm.setVariableIdentifier(variableIdentifier);
								aperm.setReadPermission(setReadPermission);
								aperm.setWritePermission(setWritePermission);
								aperm.setActors(actorsToSetPermissionsTo);
								
								getBpmDAO().persist(aperm);
								perms.add(aperm);
								
								// TODO: is this needed? merge at the end perhaps doess the thing
								// for (Actor permActor : proles) {
								//									
								// List<ActorPermissions> permActorPerms = permActor
								// .getActorPermissions();
								//									
								// if (permActorPerms == null) {
								// permActorPerms = new ArrayList<ActorPermissions>(
								// 1);
								// permActor
								// .setActorPermissions(permActorPerms);
								// }
								//									
								// permActorPerms.add(aperm);
								// }
							}
						}
						
						if (tiPerm == null) {
							for (ActorPermissions perm : perms) {
								
								if (taskInstanceId.equals(perm
								        .getTaskInstanceId())
								        && perm.getVariableIdentifier() == null) {
									tiPerm = perm;
									break;
								}
							}
						}
						
						if (variableIdentifier != null) {
							
							setWritePermission = setWritePermission
							        || (tiPerm != null && tiPerm
							                .getWritePermission());
							setReadPermission = setReadPermission
							        || (tiPerm != null && tiPerm
							                .getReadPermission());
						}
						
						if (tiPerm != null) {
							
							tiPerm.setWritePermission(setWritePermission);
							tiPerm.setReadPermission(setReadPermission);
							
							tiPerm = getBpmDAO().merge(tiPerm);
							
						} else {
							
							// creating only if there's any permissive
							// permission to be set
							
							tiPerm = new ActorPermissions();
							tiPerm.setRoleName(actorToSetPermissionTo
							        .getProcessRoleName());
							tiPerm.setTaskId(ti.getTask().getId());
							tiPerm.setTaskInstanceId(taskInstanceId);
							tiPerm.setVariableIdentifier(null);
							tiPerm.setWritePermission(setWritePermission);
							tiPerm.setReadPermission(setReadPermission);
							tiPerm.setActors(actorsToSetPermissionsTo);
							
							getBpmDAO().persist(tiPerm);
							perms.add(tiPerm);
						}
						
						// set for task instance
						
						if (setSameForAttachments && variableIdentifier == null) {
							
							List<BinaryVariable> binaryVariables = getVariablesHandler()
							        .resolveBinaryVariables(taskInstanceId);
							
							if (!binaryVariables.isEmpty()) {
								
								for (ActorPermissions perm : perms) {
									
									if (taskInstanceId.equals(perm
									        .getTaskInstanceId())
									        && perm.getVariableIdentifier() != null) {
										
										for (Iterator<BinaryVariable> iterator = binaryVariables
										        .iterator(); iterator.hasNext();) {
											BinaryVariable binVar = iterator
											        .next();
											
											if (perm.getVariableIdentifier()
											        .equals(
											            binVar.getHash()
											                    .toString())) {
												
												perm
												        .setReadPermission(setReadPermission);
												perm
												        .setWritePermission(setWritePermission);
												perm = getBpmDAO().merge(perm);
												iterator.remove();
											}
										}
									}
								}
								
								// creating non existent permissions for vars
								for (BinaryVariable binVar : binaryVariables) {
									
									tiPerm = new ActorPermissions();
									tiPerm.setRoleName(actorToSetPermissionTo
									        .getProcessRoleName());
									tiPerm.setTaskId(ti.getTask().getId());
									tiPerm.setTaskInstanceId(taskInstanceId);
									tiPerm.setVariableIdentifier(binVar
									        .getHash().toString());
									tiPerm
									        .setWritePermission(setWritePermission);
									tiPerm.setReadPermission(setReadPermission);
									tiPerm.setActors(actorsToSetPermissionsTo);
									
									getBpmDAO().persist(tiPerm);
									perms.add(tiPerm);
								}
							}
						}
						
						getBpmDAO().merge(actorToSetPermissionTo);
						}
					} else {
						logger
						        .log(
						            Level.WARNING,
						            "-RolesManagerImpl.setTaskPermissionsTIScopeForActors- No task instance found with ID="
						                    + taskInstanceId
						                    + ", processInstanceId="
						                    + processInstanceId);
					}
					
					return null;
				}
			});
			
		} else
			logger
			        .log(Level.WARNING,
			            "-RolesManagerImpl.setTaskRolePermissionsTIScope- Insufficient info provided");
	}
	
	@Transactional(readOnly = false)
	public void setContactsPermission(Role role, final Long processInstanceId,
	        Integer userId) {
		
		if (userId == null)
			throw new IllegalArgumentException("User id not provided");
		
		// TODO: move to named query
		List<Actor> actors = getBpmDAO()
		        .getResultListByInlineQuery(
		            "select act from Actor act inner join act."
		                    + Actor.nativeIdentitiesProperty + " ni where act."
		                    + Actor.processInstanceIdProperty + " = :"
		                    + Actor.processInstanceIdProperty + " and act."
		                    + Actor.processRoleNameProperty
		                    + " is null and ni."
		                    + NativeIdentityBind.identityTypeProperty + " = :"
		                    + NativeIdentityBind.identityTypeProperty
		                    + " and ni."
		                    + NativeIdentityBind.identityIdProperty + " = :"
		                    + NativeIdentityBind.identityIdProperty,
		            Actor.class,
		            new Param(Actor.processInstanceIdProperty,
		                    processInstanceId),
		            new Param(NativeIdentityBind.identityTypeProperty,
		                    NativeIdentityBind.IdentityType.USER),
		            new Param(NativeIdentityBind.identityIdProperty, userId
		                    .toString()));
		
		boolean setDefault = "default".equals(role.getRoleName());
		
		final Actor actor;
		
		if ((actors == null || actors.isEmpty()) && !setDefault) {
			
			// creating new actor for the user
			
			String processName = getBpmContext().execute(new JbpmCallback() {
				
				public Object doInJbpm(JbpmContext context)
				        throws JbpmException {
					return context.getProcessInstance(processInstanceId)
					        .getProcessDefinition().getName();
				}
			});
			
			actor = new Actor();
			actor.setProcessName(processName);
			actor.setProcessInstanceId(processInstanceId);
			
			// and native identity
			
			getBpmDAO().persist(actor);
			
			NativeIdentityBind ni = new NativeIdentityBind();
			ni.setIdentityId(userId.toString());
			ni.setIdentityType(NativeIdentityBind.IdentityType.USER);
			ni.setActor(actor);
			
			getBpmDAO().persist(ni);
			
			actor.addNativeIdentity(ni);
			
		} else {
			
			actor = actors == null || actors.isEmpty() ? null : actors
			        .iterator().next();
		}
		
		if (setDefault) {
			
			if (actor != null) {
				
				// only caring if there's an specific actor for the user
				
				List<ActorPermissions> perms = actor.getActorPermissions();
				
				if (perms != null) {
					
					for (ActorPermissions perm : perms) {
						
						getBpmDAO().remove(perm);
					}
					
					actor.setActorPermissions(null);
				}
			}
			
		} else if ("all".equals(role.getRoleName())) {
			
		} else {
			
			List<ActorPermissions> perms = actor.getActorPermissions();
			ActorPermissions canSeeRolePerm = null;
			
			if (perms != null) {
				
				for (ActorPermissions perm : perms) {
					
					// find the permission, if exist first
					
					if (role.getRoleName().equals(
					    perm.getCanSeeContactsOfRoleName())) {
						canSeeRolePerm = perm;
						break;
					}
				}
			}
			
			if (canSeeRolePerm == null) {
				
				// create permission
				
				canSeeRolePerm = new ActorPermissions();
				canSeeRolePerm.setCanSeeContactsOfRoleName(role.getRoleName());
				canSeeRolePerm.addActor(actor);
				getBpmDAO().persist(canSeeRolePerm);
				
				actor.addActorPermission(canSeeRolePerm);
			}
			
			if (role.getAccesses() != null
			        && role.getAccesses().contains(Access.contactsCanBeSeen)) {
				
				// has rights to see role
				
				// setting access to see role name contacts
				canSeeRolePerm.setCanSeeContacts(true);
				
			} else {
				
				// doesn't have rights to see role
				
				// removing see contact permission
				canSeeRolePerm.setCanSeeContacts(false);
			}
		}
	}
	
	public List<Role> getRolesPermissionsForTaskInstance(
	        Long processInstanceId, final Long taskInstanceId,
	        String variableIdentifier) {
		
		// TODO: check permissions if rights can be seen
		if (taskInstanceId == null)
			throw new IllegalArgumentException("TaskInstanceId not provided");
		
		Long taskId = getBpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				return context.getTaskInstance(taskInstanceId).getTask()
				        .getId();
			}
		});
		
		List<Actor> proles = getProcessRolesForProcessInstanceByTaskInstance(
		    processInstanceId, taskInstanceId, null);
		
		ArrayList<Role> roles = new ArrayList<Role>(proles.size());
		
		for (Actor prole : proles) {
			
			Role role = new Role();
			role.setRoleName(prole.getProcessRoleName());
			
			List<ActorPermissions> perms = prole.getActorPermissions();
			
			ArrayList<Access> taskInstanceScopeANDVar = null;
			ArrayList<Access> taskInstanceScopeNoVar = null;
			ArrayList<Access> taskScopeANDVar = null;
			ArrayList<Access> taskScopeNoVar = null;
			
			for (ActorPermissions perm : perms) {
				
				if (taskInstanceId.equals(perm.getTaskInstanceId())
				        && variableIdentifier != null
				        && variableIdentifier.equals(perm
				                .getVariableIdentifier())) {
					
					taskInstanceScopeANDVar = new ArrayList<Access>(2);
					
					if (perm.getWritePermission() != null
					        && perm.getWritePermission()) {
						
						taskInstanceScopeANDVar.add(Access.write);
					}
					if (perm.getReadPermission() != null
					        && perm.getReadPermission()) {
						
						taskInstanceScopeANDVar.add(Access.read);
					}
					
					break;
					
				} else if (taskInstanceId.equals(perm.getTaskInstanceId())
				        && perm.getVariableIdentifier() == null) {
					
					taskInstanceScopeNoVar = new ArrayList<Access>(2);
					
					if (perm.getWritePermission() != null
					        && perm.getWritePermission()) {
						
						taskInstanceScopeNoVar.add(Access.write);
					}
					if (perm.getReadPermission() != null
					        && perm.getReadPermission()) {
						
						taskInstanceScopeNoVar.add(Access.read);
					}
					
					if (variableIdentifier == null)
						break;
					
				} else if (taskId.equals(perm.getTaskId())
				        && perm.getTaskInstanceId() == null
				        && variableIdentifier != null
				        && variableIdentifier.equals(perm
				                .getVariableIdentifier())) {
					
					taskScopeANDVar = new ArrayList<Access>(2);
					
					if (perm.getWritePermission() != null
					        && perm.getWritePermission()) {
						
						taskScopeANDVar.add(Access.write);
					}
					if (perm.getReadPermission() != null
					        && perm.getReadPermission()) {
						
						taskScopeANDVar.add(Access.read);
					}
					
				} else if (taskId.equals(perm.getTaskId())
				        && perm.getTaskInstanceId() == null
				        && perm.getVariableIdentifier() == null) {
					
					taskScopeNoVar = new ArrayList<Access>(2);
					
					if (perm.getWritePermission() != null
					        && perm.getWritePermission()) {
						
						taskScopeNoVar.add(Access.write);
					}
					if (perm.getReadPermission() != null
					        && perm.getReadPermission()) {
						
						taskScopeNoVar.add(Access.read);
					}
				}
			}
			
			if (variableIdentifier != null) {
				
				if (taskInstanceScopeANDVar != null) {
					role.setAccesses(taskInstanceScopeANDVar);
				} else if (taskScopeANDVar != null) {
					role.setAccesses(taskScopeANDVar);
				} else if (taskInstanceScopeNoVar != null) {
					role.setAccesses(taskInstanceScopeNoVar);
				} else if (taskScopeNoVar != null) {
					role.setAccesses(taskScopeNoVar);
				}
				
			} else {
				
				if (taskInstanceScopeNoVar != null) {
					role.setAccesses(taskInstanceScopeNoVar);
				} else if (taskScopeNoVar != null) {
					role.setAccesses(taskScopeNoVar);
				}
			}
			
			roles.add(role);
		}
		
		return roles;
	}
	
	public Collection<Role> getUserPermissionsForRolesContacts(
	        Long processInstanceId, Integer userId) {
		
		// TODO: check permissions if rights can be seen
		if (userId == null)
			throw new IllegalArgumentException("userId not provided");
		
		List<ActorPermissions> perms = getBpmDAO().getResultList(
		    ActorPermissions.getSetByProcessInstanceIdAndCanSeeContacts,
		    ActorPermissions.class,
		    new Param(Actor.processInstanceIdProperty, processInstanceId));
		
		if (perms != null) {
			
			HashMultimap<String, ActorActorPerm> candidates = new HashMultimap<String, ActorActorPerm>();
			
			for (ActorPermissions perm : perms) {
				
				for (Actor act : perm.getActors()) {
					
					// resolving all candidate actors, for each _can see role_
					candidates.put(perm.getCanSeeContactsOfRoleName(),
					    new ActorActorPerm(act, perm));
				}
			}
			
			AccessController ac = getAccessController();
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			
			boolean canSeeAll = false;
			boolean canSeeAllPriority = false;
			Set<String> candidatesKeyset = candidates.keySet();
			
			String canSeeAllRoleName = "all";
			
			if (candidatesKeyset.contains(canSeeAllRoleName)) {
				
				// resolving, if can see all contacts, and the level of
				// "can see".
				// i.e. if it's explicitly set for the user, or just ordinary
				// rights from the role actor
				
				Set<ActorActorPerm> actorsNPerms = candidates
				        .get(canSeeAllRoleName);
				
				for (ActorActorPerm actNPerm : actorsNPerms) {
					
					if (checkFallsInActor(actNPerm.actor, userId, ac, iwac)) {
						
						if (actNPerm.actor.getProcessRoleName() == null)
							canSeeAllPriority = true;
						else
							canSeeAll = true;
					}
				}
				
				candidates.removeAll(canSeeAllRoleName);
			}
			
			HashSet<Role> explicitPermRoles = new HashSet<Role>();
			HashSet<Role> implicitPermRoles = new HashSet<Role>();
			
			if (!canSeeAllPriority) {
				
				for (String contactRoleName : candidatesKeyset) {
					
					Set<ActorActorPerm> actorsNPerms = candidates
					        .get(contactRoleName);
					
					// putting roles, that have or doesn't access
					// the priority here is -> if the actor is without role,
					// then we use it (highest priority)
					
					for (ActorActorPerm actNPerm : actorsNPerms) {
						
						boolean directlySet = actNPerm.actor
						        .getProcessRoleName() == null;
						
						if (checkFallsInActor(actNPerm.actor, userId, ac, iwac)) {
							
							// separating two, so we can merge later regarding
							// priorities: explicit > implicit
							HashSet<Role> roles = directlySet ? explicitPermRoles
							        : implicitPermRoles;
							
							// putting role if it's no there yet, or actor role
							// name == null (meaning it's explicitly set for the
							// user, therefore higher priority)
							
							boolean canSee1 = (contactRoleName
							        .equals(actNPerm.actorPerm
							                .getCanSeeContactsOfRoleName()) && actNPerm.actorPerm
							        .getCanSeeContacts());
							boolean canSee = directlySet ? canSee1
							        : canSeeAll ? true : canSee1;
							
							Role role = new Role(contactRoleName,
							        canSee ? Access.contactsCanBeSeen : null);
							
							// can see always overrides cannot see, and of
							// course putting, if nothing there yet
							if (canSee || !roles.contains(role)) {
								
								if (roles.contains(role))
									roles.remove(role);
								
								roles.add(role);
							}
						}
					}
				}
			}
			
			HashSet<Role> mergedRoles = new HashSet<Role>(explicitPermRoles
			        .size()
			        + implicitPermRoles.size());
			mergedRoles.addAll(explicitPermRoles);
			
			for (Role implicitRole : implicitPermRoles) {
				
				if (!mergedRoles.contains(implicitRole))
					mergedRoles.add(implicitRole);
			}
			
			final List<Actor> notIncludedActorsRoles;
			
			if (mergedRoles != null && !mergedRoles.isEmpty()) {
				
				HashSet<String> rolesWithAccessNames = new HashSet<String>();
				
				for (Role role : mergedRoles) {
					rolesWithAccessNames.add(role.getRoleName());
				}
				
				notIncludedActorsRoles = getBpmDAO().getResultList(
				    Actor.getSetByPIIdAndNotContainingRoleNames,
				    Actor.class,
				    new Param(Actor.processInstanceIdProperty,
				            processInstanceId),
				    new Param(Actor.processRoleNameProperty,
				            rolesWithAccessNames));
				
			} else {
				
				notIncludedActorsRoles = getBpmDAO().getResultList(
				    Actor.getSetByPIIdHavingRoleName,
				    Actor.class,
				    new Param(Actor.processInstanceIdProperty,
				            processInstanceId));
			}
			
			if (notIncludedActorsRoles != null) {
				
				for (Actor actor : notIncludedActorsRoles) {
					
					Role role = canSeeAll || canSeeAllPriority ? new Role(actor
					        .getProcessRoleName(), Access.contactsCanBeSeen)
					        : new Role(actor.getProcessRoleName());
					mergedRoles.add(role);
				}
			}
			
			return mergedRoles;
		}
		
		return Collections.emptyList();
	}
	
	class ActorActorPerm {
		
		Actor actor;
		ActorPermissions actorPerm;
		
		ActorActorPerm(Actor actor, ActorPermissions actorPerm) {
			this.actor = actor;
			this.actorPerm = actorPerm;
		}
	}
	
	public boolean checkFallsInActor(Actor actor, int userId,
	        AccessController ac, IWApplicationContext iwac) {
		
		if (actor.getProcessRoleName() == null) {
			
			List<NativeIdentityBind> nis = actor.getNativeIdentities();
			
			if (nis != null) {
				
				for (NativeIdentityBind ni : nis) {
					
					if (ni.getIdentityType() == IdentityType.USER
					        && String.valueOf(userId)
					                .equals(ni.getIdentityId()))
						return true;
				}
			}
			
		} else {
			
			return checkFallsInRole(actor.getProcessRoleName(), actor
			        .getNativeIdentities(), userId, ac, iwac);
		}
		
		return false;
	}
	
	public Set<String> getUserNativeRoles(User user) {
		
		return getAccessController().getAllRolesForUser(user);
	}
	
	public User getUser(int userId) {
		
		try {
			return getUserBusiness().getUser(userId);
			
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	public boolean checkFallsInRole(String roleName,
	        List<NativeIdentityBind> nativeIdentities, /*
	                                                                                                                                                                                                                                                        													 * Collection<Group>
	                                                                                                                                                                                                                                                        													 * usrGrps,
	                                                                                                                                                                                                                                                        													 */
	        int userId, AccessController ac, IWApplicationContext iwac) {
		
		if (nativeIdentities != null && !nativeIdentities.isEmpty()) {
			if (fallsInUsers(userId, nativeIdentities)
			        || fallsInGroups(userId, nativeIdentities))
				return true;
		} else {
			
			try {
				User usr = getUserBusiness().getUser(userId);
				Set<String> roles = ac.getAllRolesForUser(usr);
				
				if (roles.contains(roleName))
					return true;
				
			} catch (RemoteException e) {
				throw new IBORuntimeException(e);
			}
		}
		
		return false;
	}
	
	protected boolean fallsInGroups(int userId,
	        List<NativeIdentityBind> nativeIdentities) {
		
		try {
			UserBusiness ub = getUserBusiness();
			@SuppressWarnings("unchecked")
			Collection<Group> userGroups = ub.getUserGroups(userId);
			
			if (userGroups != null) {
				
				for (Group group : userGroups) {
					
					String groupId = group.getPrimaryKey().toString();
					
					for (NativeIdentityBind nativeIdentity : nativeIdentities) {
						
						if (nativeIdentity.getIdentityType() == IdentityType.GROUP
						        && nativeIdentity.getIdentityId().equals(
						            groupId))
							return true;
					}
				}
			}
			
			return false;
			
		} catch (RemoteException e) {
			throw new IDORuntimeException(e);
		}
	}
	
	protected boolean fallsInUsers(int userId,
	        List<NativeIdentityBind> nativeIdentities) {
		
		for (NativeIdentityBind nativeIdentity : nativeIdentities) {
			
			if (nativeIdentity.getIdentityType() == IdentityType.USER
			        && nativeIdentity.getIdentityId().equals(
			            String.valueOf(userId)))
				return true;
		}
		
		return false;
	}
	
	private IWMainApplication getIWMA() {
		
		return IWMainApplication.getDefaultIWMainApplication();
	}
	
	private AccessController getAccessController() {
		
		return getIWMA().getAccessController();
	}
	
	public BPMContext getBpmContext() {
		return bpmContext;
	}
	
	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}
	
	public AuthorizationService getAuthorizationService() {
		return authorizationService;
	}
	
	public void setAuthorizationService(
	        AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}
	
	private UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(
			    IWMainApplication.getDefaultIWApplicationContext(),
			    UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}
	
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
	
	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}
	
	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}
	
	public List<Actor> getProcessRoles(Collection<String> rolesNames,
	        Long processInstanceId) {
		return getBpmDAO().getProcessRoles(rolesNames, processInstanceId);
	}
	
	public BPMDAO getBpmDAO() {
		return bpmDAO;
	}
	
	public void setBpmDAO(BPMDAO bpmDAO) {
		this.bpmDAO = bpmDAO;
	}
}