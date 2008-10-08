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
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.49 $
 * 
 * Last modified: $Date: 2008/10/08 11:55:23 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmRolesManager")
@Transactional(readOnly=true)
public class RolesManagerImpl implements RolesManager {
	
	private BPMDAO bpmDAO;
	private BPMContext idegaJbpmContext;
	private AuthorizationService authorizationService;
	private VariablesHandler variablesHandler;
	@Autowired
	private PermissionsFactory permissionsFactory;
	
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
			
			List<Actor> processRoles = 
				getBpmDAO().getResultList(Actor.getSetByRoleNamesAndPIId, Actor.class,
						new Param(Actor.processRoleNameProperty, rolesNames),
						new Param(Actor.processInstanceIdProperty, processInstanceId)
				);
			
			for (Actor role : processRoles) {
				
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
					nidentity.setActor(getBpmDAO().merge(role));
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
	
			Permission permission = getPermissionsFactory().getTaskSubmitPermission(false, taskInstance);
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
			
			Permission permission = getPermissionsFactory().getTaskSubmitPermission(false, taskInstance);
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
				
				queryName = Actor.getRolesNamesHavingCaseHandlerRights;
				break;
				
			case modifyPermissions:
				queryName = Actor.getRoleNameHavingRightsModifyPermissionByPIId;
	
			default:
				throw new UnsupportedOperationException("Not supported for access="+access+", just add another query");
		}
		
		List<String> rolesNames =
			getBpmDAO().getResultList(queryName, String.class, 
					new Param(Actor.processInstanceIdProperty, processInstanceId)
			);
		
		return rolesNames;
	}
	
	public Collection<User> getAllUsersForRoles(Collection<String> rolesNames, long piId, BPMTypedPermission perm) {

		List<Actor> actors;
		
//		get all roles of contancts
		if(rolesNames == null || rolesNames.isEmpty()) {
		
			actors =
				getBpmDAO().getResultList(Actor.getSetByPIIdHavingRoleName, Actor.class, 
						new Param(Actor.processInstanceIdProperty, piId)
				);
		} else
			actors =
				getBpmDAO().getResultList(Actor.getSetByRoleNamesAndPIId, Actor.class, 
						new Param(Actor.processRoleNameProperty, rolesNames),
						new Param(Actor.processInstanceIdProperty, piId)
				);
		
		if(perm != null && actors != null && !actors.isEmpty()) {
//			filtering roles, that the permission doesn't let to see
			
			AuthorizationService authServ = getAuthorizationService();
			
			ArrayList<Actor> filteredProles = new ArrayList<Actor>(actors.size());
			
			for (Iterator<Actor> iterator = actors.iterator(); iterator.hasNext();) {
				Actor processRole = iterator.next();
				perm.setAttribute(RoleAccessPermissionsHandler.processInstanceIdAtt, processRole.getProcessInstanceId());
				perm.setAttribute(RoleAccessPermissionsHandler.roleNameAtt, processRole.getProcessRoleName());
				perm.setAttribute(RoleAccessPermissionsHandler.checkContactsForRoleAtt, true);
				
				try {
//					check the permission provided, for instance, if current user can see contacts of the role
					authServ.checkPermission((Permission)perm);
					filteredProles.add(processRole);
				} catch (AccessControlException e) {
				}
			}
			
			actors = filteredProles;
		}
		
		if(actors != null && !actors.isEmpty()) {
			
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			UserBusiness userBusiness = getUserBusiness(iwac);
			
			ArrayList<Group> allGroups = new ArrayList<Group>();
			HashMap<String, User> allUsers = new HashMap<String, User>();
			
			for (Actor prole : actors) {
				
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
	public List<Actor> createProcessRoles(String processName, Collection<Role> roles, Long processInstanceId) {
		
		if(processName == null) {
			
			JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
			
			try {
				processName = jctx.getProcessInstance(processInstanceId).getProcessDefinition().getName();
				
			} finally {
				getIdegaJbpmContext().closeAndCommit(jctx);
			}
		}
		
		HashSet<String> rolesNamesToCreate = new HashSet<String>(roles.size());
		
		for (Role role : roles) {
			
			rolesNamesToCreate.add(role.getRoleName());
//			if(role.getScope() == scope) {
//				rolesNamesToCreate.add(role.getRoleName());
//			}
		}
		
		ArrayList<Actor> processRoles = new ArrayList<Actor>();
		
		if(!rolesNamesToCreate.isEmpty()) {
		
			List<Actor> proles = 
				getBpmDAO().getResultList(Actor.getSetByRoleNamesAndPIId, Actor.class,
						new Param(Actor.processRoleNameProperty, rolesNamesToCreate),
						new Param(Actor.processInstanceIdProperty, processInstanceId)
				);
			
			for (Actor processRole : proles) {
				
				if(rolesNamesToCreate.contains(processRole.getProcessRoleName()))
					rolesNamesToCreate.remove(processRole.getProcessRoleName());
				
				processRoles.add(processRole);
			}
			
			for (String roleNameToCreate : rolesNamesToCreate) {
				
				Actor processRole = new Actor();
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
			
//			TODO: should check/compare each existing permission with what we have for each role's accesses, and see
//			if there's a need for merge

			/*
			List<ActorPermissions> perms = getBpmDAO().getResultList(ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceIdPureRoles, ActorPermissions.class,
					new Param(ActorPermissions.roleNameProperty, rolesNames),
					new Param(Actor.processInstanceIdProperty, processInstanceId)
			);
			
//			if eq, then all perms are created
			if(perms.size() < rolesNames.size()) {
				
				for (ActorPermissions actorPermissions : perms) {
					roles.remove(actorPermissions.getRoleName());
					rolesNames.remove(actorPermissions.getRoleName());
				}
				*/
			
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
				/*
			}
			*/
			
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
			/*
			List<ActorPermissions> perms = getBpmDAO().getResultList(ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceIdForContacts,
					ActorPermissions.class,
					new Param(ActorPermissions.roleNameProperty, rolesNames),
					new Param(Actor.processInstanceIdProperty, processInstanceId)
			);
			*/
			
//			if eq, then all perms are created
			/*
			if(perms.size() < rolesNames.size()) {
				
				for (ActorPermissions actorPermissions : perms) {
					roles.remove(actorPermissions.getRoleName());
					rolesNames.remove(actorPermissions.getRoleName());
				}
*/
			
				for (Role role : roles) {
					
					if(role.getRolesContacts() != null && !role.getRolesContacts().isEmpty()) {

						ArrayList<Role> contactsRolesToCreate = new ArrayList<Role>(role.getRolesContacts().size());
						
						for (String roleContact : role.getRolesContacts()) {
							
							ActorPermissions perm = new ActorPermissions();
							perm.setRoleName(role.getRoleName());
							perm.setCanSeeContacts(role.getAccesses() != null && role.getAccesses().contains(Access.seeContacts));
							perm.setCanSeeContactsOfRoleName(roleContact);
							
							getBpmDAO().persist(perm);
							
							if(createdPermissions == null) {
								createdPermissions = new ArrayList<ActorPermissions>();
							}
							createdPermissions.add(perm);
							
							if(!"all".equals(roleContact))
								contactsRolesToCreate.add(new Role(roleContact));
						}
						
						if(!contactsRolesToCreate.isEmpty())
							createProcessRoles(null, contactsRolesToCreate, processInstanceId);
					}
				}
			}
		/*
		}
		*/
		return createdPermissions;
	}
	
	@Transactional(readOnly=true)
	public List<Actor> getProcessRoles(Collection<String> rolesNames, Long processInstanceId) {
		
		List<Actor> proles = 
			getBpmDAO().getResultList(
					Actor.getSetByRoleNamesAndPIId, Actor.class,
					new Param(Actor.processRoleNameProperty, rolesNames),
					new Param(Actor.processInstanceIdProperty, processInstanceId)
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
		
		List<Actor> proles = getProcessRoles(rolesNames, processInstanceId);
		
		if(proles != null && !proles.isEmpty()) {
		
			for (Iterator<Actor> iterator = proles.iterator(); iterator.hasNext();) {
				
				Actor prole = iterator.next();
				
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
				
				for (Actor prole : proles) {
					
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
		
		List<Actor> proles = getProcessRoles(rolesNames, processInstanceId);
		
		if(proles != null && !proles.isEmpty()) {
				
				for (Actor prole : proles) {
					
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
			
			processInstancesIds = getBpmDAO().getResultList(Actor.getAllProcessInstancesIdsHavingRoleName, Long.class);
			
		} else {
			
			final String userId;
			try {
				userId = user.getPrimaryKey().toString();
			} catch(NumberFormatException e) {
				throw new RuntimeException(e);
			}
		
			Set<String> userRoles = getAccessController().getAllRolesForUser(user);
			
			/*
			@NamedNativeQuery(name=Actor.getProcessInstanceIdsByUserIdentity, resultSetMapping="processInstanceId",
					query=
//						TODO: no need for native here, move to native identities and rewrite in jpaql
						"select pr.process_instance_id as processInstanceId from "+Actor.TABLE_NAME+" pr "+
						"inner join "+NativeIdentityBind.TABLE_NAME+" ni "+ 
						"on ni.process_role_fk = pr.actor_id "+
						"where ni.identity_id = :"+NativeIdentityBind.identityIdProperty+" and ni.identity_type = :"+NativeIdentityBind.identityTypeProperty
			),
			@NamedNativeQuery(name=Actor.getProcessInstanceIdsByUserRolesAndUserIdentity, resultSetMapping="processInstanceId",
					query=
//						TODO: no need for native here, move to native identities and rewrite in jpaql
						"select pr.process_instance_id as processInstanceId from "+Actor.TABLE_NAME+" pr "+
						"where pr.role_name in (:"+Actor.processRoleNameProperty+") and pr.process_instance_id is not null "+
						"union "+
						"select pr.process_instance_id as processInstanceId from "+Actor.TABLE_NAME+" pr "+
						"inner join "+NativeIdentityBind.TABLE_NAME+" ni "+ 
						"on ni.process_role_fk = pr.actor_id "+
						"where ni.identity_id = :"+NativeIdentityBind.identityIdProperty+" and ni.identity_type = :"+NativeIdentityBind.identityTypeProperty
			)
			*/
			
			if(userRoles != null && !userRoles.isEmpty()) {
				
				processInstancesIds = 
					getBpmDAO().getResultList(Actor.getProcessInstanceIdsByUserRolesAndUserIdentity, Long.class,
							new Param(Actor.processRoleNameProperty, userRoles),
							new Param(NativeIdentityBind.identityIdProperty, userId),
							new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER.toString())
					);
				
//				processInstancesIds = 
//					getBpmDAO().getResultListByInlineQuery(
//							"select act."+Actor.processInstanceIdProperty+" as piid from "+Actor.class.getName()+" act "+
//							"where act."+Actor.processRoleNameProperty+" in (:"+Actor.processRoleNameProperty+") and act."+Actor.processInstanceIdProperty+" is not null "+
//							"union "+
//							"select act."+Actor.processInstanceIdProperty+" as piid from com.idega.jbpm.data.Actor act "+
//							"inner join act."+Actor.nativeIdentitiesProperty+" ni "+ 
//							"where ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+" and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty,
//							Long.class,
//							new Param(Actor.processRoleNameProperty, userRoles),
//							new Param(NativeIdentityBind.identityIdProperty, userId),
//							new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER)
//					);
				
			} else {
				
				processInstancesIds = 
					getBpmDAO().getResultList(Actor.getProcessInstanceIdsByUserIdentity, Long.class,
							new Param(NativeIdentityBind.identityIdProperty, userId),
							new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER)
					);
				
//				a." + Actor.actorPermissionsProperty + "
				
//				processInstancesIds = 
//					getBpmDAO().getResultListByInlineQuery(
//							"select act."+Actor.processInstanceIdProperty+" as piid from com.idega.jbpm.data.Actor act "+
//							"inner join act."+Actor.nativeIdentitiesProperty+" ni "+ 
//							"where ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+" and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty, 
//							Long.class,
//							new Param(NativeIdentityBind.identityIdProperty, userId),
//							new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER)
//					);
			}
		}
		
		if (ListUtil.isEmpty(processInstancesIds)) {
			return null;
		}
		
		return ListUtil.isEmpty(processInstancesIds) ? null : processInstancesIds;
	}
	
	public List<Actor> getProcessRolesForProcessInstanceByTaskInstance(Long processInstanceId, Long taskInstanceId, String processRoleName) {
		
		JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
//			taskInstanceProcessInstanceId might not match with processInstanceId because the task instance could be of the subprocess, taking priorities here
			Long taskInstanceProcessInstanceId = jctx.getTaskInstance(taskInstanceId).getProcessInstance().getId();
			
			List<Actor> proles;
			
			if(processInstanceId == null || processInstanceId.equals(taskInstanceProcessInstanceId)) {
				
				if(processRoleName == null) {
				
					proles = 
						getBpmDAO().getResultList(Actor.getSetByPIIdHavingRoleName, Actor.class,
								new Param(Actor.processInstanceIdProperty, taskInstanceProcessInstanceId)
						);
				} else {

					proles = getBpmDAO().getResultList(Actor.getSetByRoleNamesAndPIId, Actor.class,
							new Param(Actor.processInstanceIdProperty, taskInstanceProcessInstanceId),
							new Param(Actor.processRoleNameProperty, Arrays.asList(new String[] {processRoleName}))
					);
				}
				
			} else {
				
				if(processRoleName == null) {
					
					proles = getBpmDAO().getResultList(Actor.getSetByPIIdsHavingRoleName, Actor.class,
							new Param(Actor.processInstanceIdProperty, Arrays.asList(new Long[] {taskInstanceProcessInstanceId, processInstanceId}))
					);
					
				} else {
					
					proles = getBpmDAO().getResultList(Actor.getSetByPIIdsAndRoleNames, Actor.class,
							new Param(Actor.processRoleNameProperty, Arrays.asList(new String[] {processRoleName})),
							new Param(Actor.processInstanceIdProperty, Arrays.asList(new Long[] {taskInstanceProcessInstanceId, processInstanceId}))
					);
				}
				
				if(proles != null) {
					
					HashMap<String, Actor> prolesMap = new HashMap<String, Actor>(proles.size());
				
					for (Actor prole : proles) {

//						checking, if role is with taskinstance processinstanceid - that's the preferred. keeping only one prole for role name
						if(taskInstanceProcessInstanceId.equals(prole.getProcessInstanceId()) || !prolesMap.containsKey(prole.getProcessRoleName())) {
						
							prolesMap.put(prole.getProcessRoleName(), prole);
						}
					}
					
					proles = new ArrayList<Actor>(prolesMap.values());
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
			
				final List<Actor> proles = getProcessRolesForProcessInstanceByTaskInstance(processInstanceId, taskInstanceId, roleName);
				
				if(proles != null && !proles.isEmpty()) {
					
					Actor prole = proles.iterator().next();
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
							aperm.setActors(proles);
							
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
						tiPerm.setActors(proles);
						
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
								tiPerm.setActors(proles);
								
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
	
	@Transactional(readOnly = false)
	public void setContactsPermission(Role role, Long processInstanceId, Integer userId) {
		
		if(userId == null)
			throw new IllegalArgumentException("User id not provided");
		
		List<Actor> actors = getBpmDAO().getResultListByInlineQuery(
				"select act from Actor act inner join act."+Actor.nativeIdentitiesProperty+" ni where act." + Actor.processInstanceIdProperty + " = :" + Actor.processInstanceIdProperty +" and act."+Actor.processRoleNameProperty+" is null and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty+" and ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty, 
				Actor.class, 
				new Param(Actor.processInstanceIdProperty, processInstanceId),
				new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER),
				new Param(NativeIdentityBind.identityIdProperty, userId.toString())
		);
		
		boolean setDefault = "default".equals(role.getRoleName());
		
		final Actor actor;
		
		if((actors == null || actors.isEmpty()) && !setDefault) {

//			creating new actor for the user
			JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
			
			String processName;
			
			try {
				processName = jctx.getProcessInstance(processInstanceId).getProcessDefinition().getName();
				
			} finally {
				getIdegaJbpmContext().closeAndCommit(jctx);
			}
			
			actor = new Actor();
			actor.setProcessName(processName);
			actor.setProcessInstanceId(processInstanceId);
			
//			and native identity
			
			getBpmDAO().persist(actor);
			
			NativeIdentityBind ni = new NativeIdentityBind();
			ni.setIdentityId(userId.toString());
			ni.setIdentityType(NativeIdentityBind.IdentityType.USER);
			ni.setActor(actor);
			
			getBpmDAO().persist(ni);
			
		} else {
			
			actor = actors == null || actors.isEmpty() ? null : actors.iterator().next();
		}
		
		if(setDefault) {
			
			if(actor != null) {
				
//				only caring if there's an specific actor for the user
				
				List<ActorPermissions> perms = actor.getActorPermissions();
				
				if(perms != null) {
					
					for (ActorPermissions perm : perms) {
						
						getBpmDAO().remove(perm);
					}
					
					actor.setActorPermissions(null);
				}
			}
			
		} else if("all".equals(role.getRoleName())) {
			
		} else {
			
			List<ActorPermissions> perms = actor.getActorPermissions();
			ActorPermissions canSeeRolePerm = null;
			
			if(perms != null) {
			
				for (ActorPermissions perm : perms) {
					
//					find the permission, if exist first
					
					if(role.getRoleName().equals(perm.getCanSeeContactsOfRoleName())) {
						canSeeRolePerm = perm;
						break;
					}
				}
			}
			
			if(canSeeRolePerm == null) {
				
//				create permission
				
				canSeeRolePerm = new ActorPermissions();
				canSeeRolePerm.setCanSeeContactsOfRoleName(role.getRoleName());
				getBpmDAO().persist(canSeeRolePerm);
				
				List<ActorPermissions> actPerms = actor.getActorPermissions();
				
				if(actPerms == null)
					actPerms = new ArrayList<ActorPermissions>();
				
				actPerms.add(canSeeRolePerm);
				actor.setActorPermissions(actPerms);
			}
			
			if(role.getAccesses() != null && role.getAccesses().contains(Access.contactsCanBeSeen)) {
				
//				has rights to see role
				
//				setting access to see role name contacts
				canSeeRolePerm.setCanSeeContacts(true);
				
			} else {
				
//				doesn't have rights to see role

//				removing see contact permission
				canSeeRolePerm.setCanSeeContacts(false);
			}
		}
	}
	
	public List<Role> getRolesPermissionsForTaskInstance(Long processInstanceId, Long taskInstanceId, String variableIdentifier) {
		
//		TODO: check permissions if rights can be seen
		if(taskInstanceId == null)
			throw new IllegalArgumentException("TaskInstanceId not provided");
		
		JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Long taskId = jctx.getTaskInstance(taskInstanceId).getTask().getId();
			
			List<Actor> proles = getProcessRolesForProcessInstanceByTaskInstance(processInstanceId, taskInstanceId, null);
			
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
	
	public Collection<Role> getUserPermissionsForRolesContacts(Long processInstanceId, Integer userId) {
		
//		TODO: check permissions if rights can be seen
		if(userId == null)
			throw new IllegalArgumentException("userId not provided");
		
		JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			List<ActorPermissions> perms = getBpmDAO().getResultListByInlineQuery(
					"select ap from ActorPermissions ap inner join ap."+ActorPermissions.actorsProperty+" act where act." + Actor.processInstanceIdProperty + " = :" + Actor.processInstanceIdProperty +" and ap."+ActorPermissions.canSeeContactsOfRoleNameProperty+" is not null", 
					ActorPermissions.class, 
					new Param(Actor.processInstanceIdProperty, processInstanceId)
			);
			
			if(perms != null) {
			
				HashMultimap<String, ActorActorPerm> candidates = new HashMultimap<String, ActorActorPerm>();
				
				for (ActorPermissions perm : perms) {
					
					for (Actor act : perm.getActors()) {
						
//						resolving all candidate actors, for each _can see role_
						candidates.put(perm.getCanSeeContactsOfRoleName(), new ActorActorPerm(act, perm));
					}
				}
				
				AccessController ac = getAccessController();
				IWApplicationContext iwac = getIWMA().getIWApplicationContext();
				
				boolean canSeeAll = false;
				boolean canSeeAllPriority = false;
				Set<String> candidatesKeyset = candidates.keySet();
				
				String canSeeAllRoleName = "all";
				
				if(candidatesKeyset.contains(canSeeAllRoleName)) {
					
//					resolving, if can see all contacts, and the level of "can see". 
//					i.e. if it's explicitly set for the user, or just ordinary rights from the role actor

					Set<ActorActorPerm> actorsNPerms = candidates.get(canSeeAllRoleName);
					
					for (ActorActorPerm actNPerm : actorsNPerms) {
					
						if(checkFallsInActor(actNPerm.actor, userId, ac, iwac)) {
							
							if(actNPerm.actor.getProcessRoleName() == null)
								canSeeAllPriority = true;
							else
								canSeeAll = true;
						}
					}
					
					candidates.removeAll(canSeeAllRoleName);
				}
				
				HashSet<Role> explicitPermRoles = new HashSet<Role>();
				HashSet<Role> implicitPermRoles = new HashSet<Role>();
				
				if(!canSeeAllPriority) {
				
					for (String contactRoleName : candidatesKeyset) {
						
						Set<ActorActorPerm> actorsNPerms = candidates.get(contactRoleName);
						
//						putting roles, that have or doesn't access
//						the priority here is -> if the actor is without role, then we use it (highest priority)
						
						for (ActorActorPerm actNPerm : actorsNPerms) {
							
							boolean directlySet = actNPerm.actor.getProcessRoleName() == null;
						
							if(checkFallsInActor(actNPerm.actor, userId, ac, iwac)) {
								
//								separating two, so we can merge later regarding priorities: explicit > implicit
								HashSet<Role> roles = directlySet ? explicitPermRoles : implicitPermRoles;
								
//								putting role if it's no there yet, or actor role name == null (meaning it's explicitly set for the user, therefore higher priority)
								
								boolean canSee1 = (contactRoleName.equals(actNPerm.actorPerm.getCanSeeContactsOfRoleName()) && actNPerm.actorPerm.getCanSeeContacts());
								boolean canSee = directlySet ? canSee1 : canSeeAll ? true : canSee1;
										
								Role role = new Role(contactRoleName, canSee ? Access.contactsCanBeSeen : null);
										
//								can see always overrides cannot see, and of course putting, if nothing there yet
								if(canSee || !roles.contains(role)) {
									
									if(roles.contains(role))
										roles.remove(role);
								
									roles.add(role);
								}
							}
						}
					}
				}
				
				HashSet<Role> mergedRoles = new HashSet<Role>(explicitPermRoles.size()+implicitPermRoles.size());
				mergedRoles.addAll(explicitPermRoles);
				
				for (Role implicitRole : implicitPermRoles) {
					
					if(!mergedRoles.contains(implicitRole))
						mergedRoles.add(implicitRole);
				}
				
				final List<Actor> notIncludedActorsRoles;
				
				if(mergedRoles != null && !mergedRoles.isEmpty()) {
					
					HashSet<String> rolesWithAccessNames = new HashSet<String>();
					
					for (Role role : mergedRoles) {
						rolesWithAccessNames.add(role.getRoleName());
					}
					
					notIncludedActorsRoles = getBpmDAO().getResultList(Actor.getSetByPIIdAndNotContainingRoleNames, Actor.class, 
							new Param(Actor.processInstanceIdProperty, processInstanceId),
							new Param(Actor.processRoleNameProperty, rolesWithAccessNames)
					);
					
				} else {
					
					notIncludedActorsRoles = getBpmDAO().getResultList(Actor.getSetByPIIdHavingRoleName, Actor.class, 
							new Param(Actor.processInstanceIdProperty, processInstanceId)
					);
				}
				
				if(notIncludedActorsRoles != null) {
					
					for (Actor actor : notIncludedActorsRoles) {

						Role role = canSeeAll || canSeeAllPriority ? new Role(actor.getProcessRoleName(), Access.contactsCanBeSeen) : new Role(actor.getProcessRoleName());
						mergedRoles.add(role);
					}
				}
				
				return mergedRoles;
			}
			
			return Collections.emptyList();
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(jctx);
		}
	}
	
	class ActorActorPerm {
		
		Actor actor;
		ActorPermissions actorPerm;
		
		ActorActorPerm(Actor actor, ActorPermissions actorPerm) {
			this.actor = actor;
			this.actorPerm = actorPerm;
		}
	}
	
	public boolean checkFallsInActor(Actor actor, int userId, AccessController ac, IWApplicationContext iwac) {
		
		if(actor.getProcessRoleName() == null) {
			
			List<NativeIdentityBind> nis = actor.getNativeIdentities();
			
			if(nis != null) {
				
				for (NativeIdentityBind ni : nis) {
					
					if(ni.getIdentityType() == IdentityType.USER && String.valueOf(userId).equals(ni.getIdentityId()))
						return true;
				}
			}
			
			
		} else {
			
			return checkFallsInRole(actor.getProcessRoleName(), actor.getNativeIdentities(), userId, ac, iwac);
		}
		
		return false;
	}
	
	public boolean checkFallsInRole(String roleName, List<NativeIdentityBind> nativeIdentities, /*Collection<Group> usrGrps,*/ int userId, AccessController ac, IWApplicationContext iwac) {
		
		if(nativeIdentities != null && !nativeIdentities.isEmpty()) {
			if(fallsInUsers(userId, nativeIdentities) || fallsInGroups(userId, nativeIdentities))
				return true;
		} else {
			
			try {
				User usr = getUserBusiness(IWContext.getCurrentInstance()).getUser(userId);
				Set<String> roles = ac.getAllRolesForUser(usr);
				
				if(roles.contains(roleName))
					return true;
				
			} catch (RemoteException e) {
				throw new IBORuntimeException(e);
			}
		}
		
		return false;
	}
	
	protected boolean fallsInGroups(int userId, List<NativeIdentityBind> nativeIdentities) {
		
		try {
			UserBusiness ub = getUserBusiness(IWContext.getCurrentInstance());
			@SuppressWarnings("unchecked")
			Collection<Group> userGroups = ub.getUserGroups(userId);
			
			if(userGroups != null) {
			
				for (Group group : userGroups) {
					
					String groupId = group.getPrimaryKey().toString();
					
					for (NativeIdentityBind nativeIdentity : nativeIdentities) {
						
						if(nativeIdentity.getIdentityType() == IdentityType.GROUP && nativeIdentity.getIdentityId().equals(groupId))
							return true;
					}
				}
			}
			
			return false;
			
		} catch (RemoteException e) {
			throw new IDORuntimeException(e);
		}
	}
	
	protected boolean fallsInUsers(int userId, List<NativeIdentityBind> nativeIdentities) {
		
		for (NativeIdentityBind nativeIdentity : nativeIdentities) {
			
			if(nativeIdentity.getIdentityType() == IdentityType.USER && nativeIdentity.getIdentityId().equals(String.valueOf(userId)))
				return true;
		}
		
		return false;
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

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}
}