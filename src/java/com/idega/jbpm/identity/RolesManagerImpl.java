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
import org.springframework.beans.factory.config.BeanDefinition;
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
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.identity.permission.RoleAccessPermissionsHandler;
import com.idega.jbpm.rights.Right;
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
 * No synchronizations or constraints are put, so the entries might duplicate on race condition.
 * Yet, in all cases (afaik) extra entries, that could happen, don't do any real harm. TODO: but
 * none said it would be nice to fix that.
 * </p>
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.69 $ Last modified: $Date: 2009/07/03 08:59:57 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service("bpmRolesManager")
@Transactional(readOnly = true, noRollbackFor = {AccessControlException.class, BPMAccessControlException.class})
public class RolesManagerImpl implements RolesManager {

	@Autowired
	private BPMDAO bpmDAO;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	AuthorizationService authorizationService;
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private VariablesHandler variablesHandler;
	@Autowired
	private PermissionsFactory permissionsFactory;

	private static final Logger logger = Logger.getLogger(RolesManagerImpl.class.getName());

	@Override
	@Transactional(readOnly = false)
	public void createIdentitiesForRolesNames(Set<String> rolesNames, Identity identity, long processInstanceId) {
		modifyIdentitiesForRoles(rolesNames, identity, processInstanceId, false);
	}

	@Override
	@Transactional(readOnly = false)
	public void createIdentitiesForRoles(Collection<Role> roles, Identity identity, long processInstanceId) {
		if (ListUtil.isEmpty(roles))
			return;

		Set<String> rolesNames = new HashSet<String>(roles.size());
		for (Role role: roles) {
			rolesNames.add(role.getRoleName());
		}

		modifyIdentitiesForRoles(rolesNames, identity, processInstanceId, false);
	}

	@Override
	@Transactional(readOnly = false)
	public void removeIdentitiesForRoles(Collection<Role> roles, Identity identity, long processInstanceId) {
		if (ListUtil.isEmpty(roles))
			return;

		Set<String> rolesNames = new HashSet<String>(roles.size());
		for (Role role : roles) {
			rolesNames.add(role.getRoleName());
		}

		modifyIdentitiesForRoles(rolesNames, identity, processInstanceId, true);
	}

	@Override
	@Transactional(readOnly = false)
	public void removeIdentitiesForActors(Collection<Actor> actors, Identity identity, long processInstanceId) {
		modifyIdentitiesForActors(actors, identity, processInstanceId, true);
	}

	@Override
	@Transactional(readOnly = false)
	public void createIdentitiesForActors(Collection<Actor> actors, Identity identity, long processInstanceId) {
		modifyIdentitiesForActors(actors, identity, processInstanceId, false);
	}

	@Transactional(readOnly = false)
	void modifyIdentitiesForActors(Collection<Actor> actors, Identity identity, long processInstanceId, boolean remove) {
		if (identity.getIdentityType() == null || StringUtil.isEmpty(identity.getIdentityId()))
			throw new IllegalArgumentException(
			        "Identity with no identity type or identity id provided. ProcessInstanceId="
			                + processInstanceId + ", identity id = "
			                + identity.getIdentityId() + ", identity id = "
			                + identity.getIdentityId()
			                + ", identity expression = "
			                + identity.getIdentityIdExpression());

		for (Actor actor: actors) {
			List<NativeIdentityBind> identities = actor.getNativeIdentities();
			boolean contains = false;
			if (identities != null) {
				for (Iterator<NativeIdentityBind> iterator = identities.iterator(); iterator.hasNext();) {
					NativeIdentityBind nativeIdentityBind = iterator.next();

					if (identity.getIdentityType() == nativeIdentityBind.getIdentityType()
					        && identity.getIdentityId().equals(nativeIdentityBind.getIdentityId())) {

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
				nidentity.setIdentityId(identity.getIdentityId());
				nidentity.setIdentityType(identity.getIdentityType());
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
	void modifyIdentitiesForRoles(Set<String> rolesNames, Identity identity, long processInstanceId, boolean remove) {
		if (ListUtil.isEmpty(rolesNames))
			return;

		List<Actor> actors = getBpmDAO().getResultList(
		    Actor.getSetByRoleNamesAndPIId, Actor.class,
		    new Param(Actor.processRoleNameProperty, rolesNames),
		    new Param(Actor.processInstanceIdProperty, processInstanceId)
		);

		modifyIdentitiesForActors(actors, identity, processInstanceId, remove);
	}

	@Override
	@Transactional(readOnly = true)
	public void hasRightsToStartTask(final long taskInstanceId, final int userId) throws BPMAccessControlException {
		try {
			getBpmContext().execute(new JbpmCallback<Void>() {

				@Override
				public Void doInJbpm(JbpmContext context) throws JbpmException {
					TaskInstance taskInstance = context.getTaskInstance(taskInstanceId);

					if (taskInstance.getStart() != null || taskInstance.hasEnded()) throw new BPMAccessControlException(
						        "Task ("
						                + taskInstanceId
						                + ") has already been started, or has already ended",
						        "Task has already been started, or has already ended");

					if (taskInstance.getActorId() == null || !taskInstance.getActorId().equals(String.valueOf(userId)))
						throw new BPMAccessControlException(
						        "User ("
						                + userId
						                + ") tried to start task, but not assigned to the user provided. Assigned: "
						                + taskInstance.getActorId(),
						        "User should be taken or assigned of the task first to start working on it");

					Permission permission = getPermissionsFactory().getTaskInstanceSubmitPermission(false, taskInstanceId);
					getAuthorizationService().checkPermission(permission);
					return null;
				}
			});
		} catch (BPMAccessControlException e) {
			throw e;
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("User has no access to modify this task");
		}
	}

	@Override
	public void checkPermission(Permission permission) throws BPMAccessControlException {
		try {
			getAuthorizationService().checkPermission(permission);
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("No access");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void hasRightsToAssignTask(final long taskInstanceId, final int userId) throws BPMAccessControlException {
		try {
			getBpmContext().execute(new JbpmCallback<Void>() {

				@Override
				public Void doInJbpm(JbpmContext context) throws JbpmException {
					TaskInstance taskInstance = context.getTaskInstance(taskInstanceId);

					if (taskInstance.getStart() != null || taskInstance.hasEnded())
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

					Permission permission = getPermissionsFactory().getTaskInstanceSubmitPermission(false, taskInstanceId);
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

	@Override
	public Collection<User> getAllUsersForRoles(Collection<String> rolesNames,
	        long piId) {

		return getAllUsersForRoles(rolesNames, piId, null);
	}

	@Override
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

	@Override
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
//				role.setScope(RoleScope.PI);
				roles.add(role);
			}

			return roles;
		}

		return null;
	}

	@Override
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

					Collection<Group> grps = getAccessController()
					        .getAllGroupsForRoleKeyLegacy(actor.getProcessRoleName(),
					            iwac);

					if (grps != null)
						allGroups.addAll(grps);

				} else {

					try {
						for (NativeIdentityBind identity: actor.getNativeIdentities()) {

							if (identity.getIdentityType() == IdentityType.USER) {
								User user = null;
								try {
									user = userBusiness.getUser(new Integer(identity.getIdentityId()));
								} catch (Exception e) {
									logger.log(Level.WARNING, "Error getting user by ID " + identity.getIdentityId(), e);
								}
								if (user != null) {
									allUsers.put(user.getPrimaryKey().toString(),  user);
								}
							} else if (identity.getIdentityType() == IdentityType.GROUP) {
								Collection<User> groupUsers = userBusiness.getUsersInGroup(new Integer(identity.getIdentityId()));
								for (User user : groupUsers)
									allUsers.put(user.getPrimaryKey().toString(), user);

							} else if (identity.getIdentityType() == IdentityType.ROLE) {
								Collection<Group> grps = getAccessController().getAllGroupsForRoleKeyLegacy(identity.getIdentityId(), iwac);
								if (grps != null)
									allGroups.addAll(grps);
							}
						}

					} catch (RemoteException e) {
						logger.log(Level.SEVERE, "Exception while loading users from nativeIdentities", e);
					}
				}
			}

			try {
				for (Group group : allGroups) {
					Collection<User> users = userBusiness.getUsersInGroup(group);

					for (User user : users) {
						allUsers.put(user.getPrimaryKey().toString(), user);
					}
				}

			} catch (RemoteException e) {
				logger.log(Level.SEVERE, "Exception while resolving users from roles assigned groups", e);
			}

			return allUsers.values();
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	@Transactional(readOnly = false)
	public List<Actor> createProcessActors(JbpmContext context, Collection<Role> roles, final ProcessInstance processInstance) {
		final Set<String> rolesNamesToCreate = new HashSet<String>(roles.size());

		for (Role role : roles)
			rolesNamesToCreate.add(role.getRoleName());

		if (ListUtil.isEmpty(rolesNamesToCreate))
			return Collections.emptyList();

		List<Actor> processActors = new ArrayList<Actor>();

		List<Actor> existingActors = getBpmDAO().getResultList(
		    Actor.getSetByRoleNamesAndPIId,
		    Actor.class,
		    new Param(Actor.processRoleNameProperty, rolesNamesToCreate),
		    new Param(Actor.processInstanceIdProperty, processInstance.getId())
		);

		// removing from "to create" list roles, that already have actors
		for (Actor existingActor: existingActors) {
			if (rolesNamesToCreate.contains(existingActor.getProcessRoleName()))
				rolesNamesToCreate.remove(existingActor.getProcessRoleName());

			processActors.add(existingActor);
		}

		if (!rolesNamesToCreate.isEmpty()) {
			// creating actors for roles
			String processName = processInstance.getProcessDefinition().getName();
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

		return processActors;
	}

	@Override
	@Transactional(readOnly = false)
	public List<Actor> createProcessActors(final Collection<Role> roles, final ProcessInstance processInstance) {
		return getBpmContext().execute(new JbpmCallback<List<Actor>>() {
			@Override
			public List<Actor> doInJbpm(JbpmContext context) throws JbpmException {
				return createProcessActors(context, roles, processInstance);
			}
		});
	}

	@Override
	@Transactional(readOnly = false)
	public void createTaskRolesPermissions(JbpmContext context, Long taskId, List<Role> roles) {
		if (ListUtil.isEmpty(roles))
			return;

		Set<String> rolesNames = new HashSet<String>(roles.size());
		for (Role role: roles) {
			rolesNames.add(role.getRoleName());
		}

		List<ActorPermissions> perms = getBpmDAO().getResultList(
		    ActorPermissions.getSetByTaskIdAndProcessRoleNames,
		    ActorPermissions.class,
		    new Param(ActorPermissions.taskIdProperty, taskId),
		    new Param(ActorPermissions.roleNameProperty, rolesNames)
		);

		// if eq, then all perms are created
		if (perms.size() < rolesNames.size()) {
			for (ActorPermissions actorPermissions: perms) {
				roles.remove(actorPermissions.getRoleName());
				rolesNames.remove(actorPermissions.getRoleName());
			}

			logger.info("Creating permissions for task: " + taskId + ", for roles: " + rolesNames);

			for (Role role: roles) {
				ActorPermissions perm = new ActorPermissions();
				perm.setTaskId(taskId);
				perm.setRoleName(role.getRoleName());
				perm.setReadPermission(role.getAccesses().contains(Access.read));
				perm.setWritePermission(role.getAccesses().contains(Access.write));
				perm.setModifyRightsPermission(role.getAccesses().contains(Access.modifyPermissions));
				perm.setCaseHandlerPermission(role.getAccesses().contains(Access.caseHandler));

				getBpmDAO().persist(perm);
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
			boolean hasCommentsPermissions = false;

			for (Role role : roles) {
				rolesNames.add(role.getRoleName());

				if (!hasContactsPermissions && role.getRolesContacts() != null && !role.getRolesContacts().isEmpty())
					hasContactsPermissions = true;
				if (!hasCommentsPermissions && !ListUtil.isEmpty(role.getRolesComments()))
					hasCommentsPermissions = true;
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

			if (hasCommentsPermissions) {
				List<ActorPermissions> createdCommentsPermissions = createRolesCommentsPermissions(roles, processInstance);

				if (createdCommentsPermissions != null) {

					if (createdPermissions == null)
						createdPermissions = createdCommentsPermissions;
					else
						createdPermissions.addAll(createdCommentsPermissions);
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
	public List<ActorPermissions> createRolesCommentsPermissions(List<Role> roles, ProcessInstance processInstance) {

		List<ActorPermissions> createdPermissions = null;

		if (!roles.isEmpty()) {

			Set<String> rolesNames = new HashSet<String>(roles.size());

			for (Role role : roles) {
				rolesNames.add(role.getRoleName());
			}

			for (Role role : roles) {

				if (!ListUtil.isEmpty(role.getRolesComments())) {

					List<Role> commentsRolesToCreate = new ArrayList<Role>(role.getRolesComments().size());

					for (String roleComment : role.getRolesComments()) {

						ActorPermissions perm = new ActorPermissions();
						perm.setRoleName(role.getRoleName());
						if ("all".equals(roleComment)) {
							perm.setCanSeeComments(Boolean.TRUE);
							perm.setCanWriteComments(Boolean.TRUE);
						} else {
							perm.setCanSeeComments(role.getAccesses() != null && role.getAccesses().contains(Access.seeComments));
							perm.setCanWriteComments(role.getAccesses() != null && role.getAccesses().contains(Access.writeComments));
						}

						getBpmDAO().persist(perm);

						if (createdPermissions == null) {
							createdPermissions = new ArrayList<ActorPermissions>();
						}
						createdPermissions.add(perm);

						if (!"all".equals(roleComment))
							commentsRolesToCreate.add(new Role(roleComment));
					}

					if (!commentsRolesToCreate.isEmpty())
						createProcessActors(commentsRolesToCreate, processInstance);
				}
			}
		}
		return createdPermissions;
	}

	@Override
	@Transactional(readOnly = false)
	public void assignIdentities(ProcessInstance processInstance,
	        List<Role> roles) {

		for (Role role : roles) {

			if (!ListUtil.isEmpty(role.getIdentities())) {

				for (Identity identity : role.getIdentities()) {

					Set<String> rolesNames = new HashSet<String>(1);
					rolesNames.add(role.getRoleName());

					createIdentitiesForRolesNames(rolesNames, identity,
					    processInstance.getId());
				}
			}
		}
	}

	@Override
	@Transactional(readOnly = false)
	public void assignTaskRolesPermissions(JbpmContext context, Long taskId, List<Role> roles, Long processInstanceId) {
		if (ListUtil.isEmpty(roles))
			return;

		createTaskRolesPermissions(context, taskId, roles);

		Set<String> rolesNames = new HashSet<String>(roles.size());

		for (Role role : roles) {
			rolesNames.add(role.getRoleName());
		}

		List<Actor> actorsForRoles = getProcessRoles(rolesNames, processInstanceId);

		if (!ListUtil.isEmpty(actorsForRoles)) {
			List<Actor> actors = new ArrayList<Actor>(actorsForRoles);

			for (Iterator<Actor> actorsIterator = actors.iterator(); actorsIterator.hasNext();) {
				Actor actor = actorsIterator.next();

				List<ActorPermissions> perms = actor.getActorPermissions();
				if (!ListUtil.isEmpty(perms)) {

					// look, if this actor has permission for the task (and it
					// is not a task
					// instance permission). If true, then we remove the actor
					// from actors list of
					// further processing
					for (ActorPermissions perm : perms) {
						if (taskId.equals(perm.getTaskId()) && perm.getTaskInstanceId() == null) {
//							this actor already has permission for task (not task instance), so we skip it
							actorsIterator.remove();
							break;
						}
					}
				}
			}

			if (!actors.isEmpty()) {

				// actors, that don't have task permissions, will be assigned
				// with them here

				List<ActorPermissions> permsToSet = getBpmDAO().getResultList(
				    ActorPermissions.getSetByTaskIdAndProcessRoleNames,
				    ActorPermissions.class,
				    new Param(ActorPermissions.taskIdProperty, taskId),
				    new Param(ActorPermissions.roleNameProperty, rolesNames)
				);

				for (Actor actor: actors) {
					for (ActorPermissions perm: permsToSet) {
						if (actor.getProcessRoleName().equals(perm.getRoleName())) {
							actor.addActorPermission(perm);
						}
					}
				}
			}
		} else
			logger.warning("No process roles found by roles: " + rolesNames + ", processInstanceId: " + processInstanceId);
	}

	@Override
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

						// TODO: I commented this out 0117, does this break
						// something? Looked like
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

	@Override
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

	@Override
	public List<Actor> getPermissionsByProcessInstanceId(
	        Long processInstanceId,/* Long processInstanceIdFromTaskInstance, */
	        String processRoleName) {

		// TODO: this is not only for backward compatibility - we will be caring
		// only about actors of the mainProcessInstances from now on
		// So we will use the processInstanceId only. Also, get permissions here
		// not actors

		// taskInstanceProcessInstanceId might not match with processInstanceId
		// because the task instance could be of the subprocess, taking
		// priorities here

		List<Actor> actors;

		if (processRoleName == null) {

			actors = getBpmDAO().getResultList(
			    Actor.getSetByPIIdHavingRoleName, Actor.class,
			    new Param(Actor.processInstanceIdProperty, processInstanceId));
		} else {

			actors = getBpmDAO().getResultList(
			    Actor.getSetByRoleNamesAndPIId,
			    Actor.class,
			    new Param(Actor.processInstanceIdProperty, processInstanceId),
			    new Param(Actor.processRoleNameProperty, Arrays
			            .asList(new String[] { processRoleName })));
		}

		/*
		 *
		 * if (processInstanceId == null ||
		 * processInstanceId.equals(processInstanceIdFromTaskInstance)) {
		 *
		 * // here we resolve actors by processInstanceId from task instance
		 *
		 * if (processRoleName == null) {
		 *
		 * actors = getBpmDAO().getResultList( Actor.getSetByPIIdHavingRoleName,
		 * Actor.class, new Param(Actor.processInstanceIdProperty,
		 * processInstanceIdFromTaskInstance)); } else {
		 *
		 * actors = getBpmDAO().getResultList( Actor.getSetByRoleNamesAndPIId,
		 * Actor.class, new Param(Actor.processInstanceIdProperty,
		 * processInstanceIdFromTaskInstance), new
		 * Param(Actor.processRoleNameProperty, Arrays .asList(new String[] {
		 * processRoleName }))); }
		 *
		 * } else {
		 *
		 * if (processRoleName == null) {
		 *
		 * actors = getBpmDAO().getResultList(
		 * Actor.getSetByPIIdsHavingRoleName, Actor.class, new
		 * Param(Actor.processInstanceIdProperty, Arrays .asList(new Long[] {
		 * processInstanceIdFromTaskInstance, processInstanceId })));
		 *
		 * } else {
		 *
		 * actors = getBpmDAO().getResultList( Actor.getSetByPIIdsAndRoleNames,
		 * Actor.class, new Param(Actor.processRoleNameProperty, Arrays
		 * .asList(new String[] { processRoleName })), new
		 * Param(Actor.processInstanceIdProperty, Arrays .asList(new Long[] {
		 * processInstanceIdFromTaskInstance, processInstanceId }))); }
		 *
		 * if (actors != null) {
		 *
		 * HashMap<String, Actor> prolesMap = new HashMap<String, Actor>(
		 * actors.size());
		 *
		 * for (Actor prole : actors) {
		 *
		 * // checking, if role is with taskinstance processinstanceid // -
		 * that's the preferred. keeping only one prole for role // name if
		 * (processInstanceIdFromTaskInstance.equals(prole
		 * .getProcessInstanceId()) || !prolesMap.containsKey(prole
		 * .getProcessRoleName())) {
		 *
		 * prolesMap.put(prole.getProcessRoleName(), prole); } }
		 *
		 * actors = new ArrayList<Actor>(prolesMap.values()); } }
		 */

		return actors;
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
	@Override
	@Transactional(readOnly = false)
	public void setTaskRolePermissionsTIScope(final Role role,
	        final Long taskInstanceId, final boolean setSameForAttachments,
	        final String variableIdentifier) {

		// TODO: check permissions if rights can be changed
		final String roleName = role.getRoleName();

		if (roleName != null && taskInstanceId != null && roleName.length() != 0) {

			List<Actor> actorsToSetPermissionsTo = getBpmContext().execute(new JbpmCallback<List<Actor>>() {
				@Override
				public List<Actor> doInJbpm(JbpmContext context) throws JbpmException {

					TaskInstance ti = context.getTaskInstance(taskInstanceId);
					long processInstanceIdFromTaskInstance = ti.getProcessInstance().getId();

					ProcessInstance mainprocessInstance = getBpmFactory().getMainProcessInstance(context, processInstanceIdFromTaskInstance);
					Long mainProcessInstanceId = mainprocessInstance.getId();

					/*
					 * if(mainprocessInstance != null) {
					 * mainProcessInstanceId =
					 * mainprocessInstance.getId(); } else { //
					 * backwards compat mainProcessInstanceId =
					 * processInstanceIdFromTaskInstance; }
					 */

					final List<Actor> actorsToSetPermissionsTo = getPermissionsByProcessInstanceId(mainProcessInstanceId, roleName);
					return actorsToSetPermissionsTo;
				}
			});

			/*
			 * if(!ListUtil.isEmpty(actorsToSetPermissionsTo) &&
			 * actorsToSetPermissionsTo.size() > 1) {
			 *
			 * // TODO: this is backward compatibility, this code will be
			 * changed (or cleaned) when all actors will refer the
			 * mainProcessInstanceId, and not subprocess processinstanceid Actor
			 * act = actorsToSetPermissionsTo.iterator().next();
			 * actorsToSetPermissionsTo = new ArrayList<Actor>(1);
			 * actorsToSetPermissionsTo.add(act); }
			 */

			setTaskPermissionsTIScopeForActors(actorsToSetPermissionsTo, role.getAccesses(), taskInstanceId, setSameForAttachments, variableIdentifier);
		} else
			logger
			        .log(Level.WARNING,
			            "-RolesManagerImpl.setTaskRolePermissionsTIScope- Insufficient info provided");
	}

	@Transactional(readOnly = false)
	private Actor getPreparedActor(Role role, final Long processInstanceId, Integer userId) {
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
			String processName = getBpmContext().execute(new JbpmCallback<String>() {
				@Override
				public String doInJbpm(JbpmContext context) throws JbpmException {
					return context.getProcessInstance(processInstanceId).getProcessDefinition().getName();
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

		return actor;
	}

	@Override
	@Transactional(readOnly = false)
	public void setAttachmentPermission(Role role, final Long processInstanceId, Long taskInstanceId, String variableIdentifier, Integer userId) {
		Actor actor = getPreparedActor(role, processInstanceId, userId);

		boolean setDefault = "default".equals(role.getRoleName());
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
					//	Find the permission, if exist first
					if (role.getRoleName().equals(perm.getCanSeeAttachmentsOfRoleName())) {
						if (role.getForTaskInstance()) {
							Long permissionTaskInstance = perm.getTaskInstanceId();
							if (permissionTaskInstance != null && taskInstanceId != null && permissionTaskInstance.longValue() == taskInstanceId.longValue()) {
								String varId = perm.getVariableIdentifier();
								if (!StringUtil.isEmpty(varId) && varId.equals(variableIdentifier)) {
									canSeeRolePerm = perm;
									break;
								}
							}
						} else {
							canSeeRolePerm = perm;
							break;
						}
					}
				}
			}

			if (canSeeRolePerm == null) {
				// create permission
				canSeeRolePerm = new ActorPermissions();
				canSeeRolePerm.setCanSeeAttachmentsOfRoleName(role.getRoleName());
				canSeeRolePerm.addActor(actor);
				canSeeRolePerm.setTaskInstanceId(taskInstanceId);
				canSeeRolePerm.setVariableIdentifier(variableIdentifier);
				getBpmDAO().persist(canSeeRolePerm);

				actor.addActorPermission(canSeeRolePerm);
			} else {
				canSeeRolePerm.setTaskInstanceId(taskInstanceId);
				getBpmDAO().persist(canSeeRolePerm);
			}

			if (role.getAccesses() != null && role.getAccesses().contains(Access.seeAttachments)) {
				// has rights to see role, setting access to see role name contacts
				canSeeRolePerm.setCanSeeAttachments(Boolean.TRUE);
			} else {
				// doesn't have rights to see role, removing see contact permission
				canSeeRolePerm.setCanSeeAttachments(Boolean.FALSE);
			}
		}
	}

	@Override
	@Transactional(readOnly = false)
	public void setContactsPermission(Role role, final Long processInstanceId,
	        Integer userId) {

		Actor actor = getPreparedActor(role, processInstanceId, userId);

		boolean setDefault = "default".equals(role.getRoleName());
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

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.idega.jbpm.identity.RolesManager#getRolesPermissionsForTaskInstance
	 * (java.lang.Long, java.lang.Long, java.lang.String)
	 *
	 * The real permission check is handled at
	 * com.idega.jbpm.identity.permission
	 * .TaskAccessPermissionsHandler.checkPermissionsForTaskInstance(int,
	 * TaskInstance, Collection<Access>, String)
	 */
	@Override
	public List<Role> getRolesPermissionsForTaskInstance(
	        final Long taskInstanceId, final String variableIdentifier) {

		// TODO: check permissions if rights can be seen
		if (taskInstanceId == null)
			throw new IllegalArgumentException("TaskInstanceId not provided");

		return getBpmContext().execute(new JbpmCallback<List<Role>>() {

			@Override
			public List<Role> doInJbpm(JbpmContext context) throws JbpmException {

				TaskInstance ti = context.getTaskInstance(taskInstanceId);
				Long processInstanceIdFromTaskInstance = ti
				        .getProcessInstance().getId();
				// processInstanceId from taskInstance can be different than the
				// processInstanceId provided in the case of taskInstance being
				// in the subprocess
				Long taskId = ti.getTask().getId();

				ProcessInstance mainProcessInstance = getBpmFactory().getMainProcessInstance(context, processInstanceIdFromTaskInstance);
				Long mainProcessInstanceId = mainProcessInstance.getId();

				/*
				 * if(mainProcessInstance != null) mainProcessInstanceId =
				 * mainProcessInstance.getId(); else // backward compatibility
				 * mainProcessInstanceId = processInstanceIdFromTaskInstance;
				 */

				List<Actor> actors = getPermissionsByProcessInstanceId(
				    mainProcessInstanceId, null);

				ArrayList<Role> roles = new ArrayList<Role>(actors.size());

				for (Actor actor : actors) {

					Role role = new Role(actor.getProcessRoleName());

					List<ActorPermissions> perms = actor.getActorPermissions();

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

						} else if (taskInstanceId.equals(perm
						        .getTaskInstanceId())
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

						// for variable, we resolve permissions in this order:
						// 1. if permission is set for task instance level
						// variable (case, when permission is set in the UI)
						// 2. if permission is set for task level variable (not
						// used at all (20090211))
						// 3. if permission is set for task. Actually the
						// permission cannot be not set for task (well, kinda),
						// so we could throw the exception here, logging for the
						// moment

						if (taskInstanceScopeANDVar != null) {
							role.setAccesses(taskInstanceScopeANDVar);
						} else if (taskScopeANDVar != null) {
							role.setAccesses(taskScopeANDVar);
							/*
							 * } else if (taskInstanceScopeNoVar != null) {
							 * role.setAccesses(taskInstanceScopeNoVar);
							 */
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
		});
	}

	@Override
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

			HashMultimap<String, ActorActorPerm> candidates = HashMultimap.create();

			for (ActorPermissions perm : perms) {
				List<Actor> actors = perm.getActors();
				if(ListUtil.isEmpty(actors)){
					continue;
				}
				for (Actor act : actors) {

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

	@Override
	public Set<String> getUserNativeRoles(User user) {
		return getAccessController().getAllRolesForUser(user);
	}

	@Override
	public User getUser(int userId) {
		try {
			return getUserBusiness().getUser(userId);
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		}
	}

	@Override
	public boolean checkFallsInRole(String roleName, List<NativeIdentityBind> nativeIdentities, int userId, AccessController ac,
			IWApplicationContext iwac) {

		if (nativeIdentities != null && !nativeIdentities.isEmpty()) {
			if (fallsInUsers(userId, nativeIdentities)
			        || fallsInGroups(userId, nativeIdentities)
			        || fallsInRoles(userId, nativeIdentities))
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

	@Override
	@Transactional(readOnly = false)
	public void setTaskPermissionsTIScopeForActors(
	        final List<Actor> actorsToSetPermissionsTo,
	        final List<Access> accesses, final Long taskInstanceId,
	        final boolean setSameForAttachments, final String variableIdentifier) {

		getBpmContext().execute(new JbpmCallback<Void>() {

			@Override
			public Void doInJbpm(JbpmContext context) throws JbpmException {

				TaskInstance ti = context.getTaskInstance(taskInstanceId);
				long taskId = ti.getTask().getId();
				// long processInstanceId = ti.getProcessInstance().getId();

				// TODO: get permissions here, and differently - for task
				// instances, and for variables

				if (!ListUtil.isEmpty(actorsToSetPermissionsTo)) {

					for (int i = 0; i < actorsToSetPermissionsTo.size(); i++) {

						// for (Actor actorToSetPermissionTo :
						// actorsToSetPermissionsTo) { //doesn't work after
						// getBpmDAO().merge(actorToSetPermissionTo);

						Actor actorToSetPermissionTo = actorsToSetPermissionsTo
						        .get(i);

						List<ActorPermissions> perms = actorToSetPermissionTo
						        .getActorPermissions();

						if (ListUtil.isEmpty(perms)) {
							perms = new ArrayList<ActorPermissions>(1);
							actorToSetPermissionTo.setActorPermissions(perms);
						}

						// List<Access> accesses = role.getAccesses();
						boolean setReadPermission = accesses != null
						        && accesses.contains(Access.read);
						boolean setWritePermission = accesses != null
						        && accesses.contains(Access.write);

						if (!StringUtil.isEmpty(variableIdentifier)) {

							// setting permissions for variable

							ActorPermissions varPerm = null;

							for (ActorPermissions perm : perms) {

								/*
								 * if (taskInstanceId.equals(perm
								 * .getTaskInstanceId()) &&
								 * perm.getVariableIdentifier() == null) {
								 * tiPerm = perm; } else
								 */if (taskInstanceId.equals(perm
								        .getTaskInstanceId())
								        && variableIdentifier.equals(perm
								                .getVariableIdentifier())) {
									varPerm = perm;
									break;
								}
							}

							if (varPerm != null) {

								// found variable permission, updating
								varPerm.setWritePermission(setWritePermission);
								varPerm.setReadPermission(setReadPermission);
								varPerm = getBpmDAO().merge(varPerm);

							} else {

								// creating permission for variable

								varPerm = new ActorPermissions();
								varPerm.setRoleName(actorToSetPermissionTo
								        .getProcessRoleName());
								varPerm.setTaskId(taskId);
								varPerm.setTaskInstanceId(taskInstanceId);
								varPerm
								        .setVariableIdentifier(variableIdentifier);
								varPerm.setReadPermission(setReadPermission);
								varPerm.setWritePermission(setWritePermission);
								varPerm.setActors(actorsToSetPermissionsTo);

								getBpmDAO().persist(varPerm);
								perms.add(varPerm);
							}
						} else {

							// setting permissions for task instance

							ActorPermissions tiPerm = null;

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
								tiPerm.setTaskId(taskId);
								tiPerm.setTaskInstanceId(taskInstanceId);
								tiPerm.setVariableIdentifier(null);
								tiPerm.setWritePermission(setWritePermission);
								tiPerm.setReadPermission(setReadPermission);
								tiPerm.setActors(actorsToSetPermissionsTo);

								getBpmDAO().persist(tiPerm);
								perms.add(tiPerm);
							}

							if (setSameForAttachments) {

								// setting the same rights for all attachments,
								// as for task instance

								List<BinaryVariable> binaryVariables = getVariablesHandler()
								        .resolveBinaryVariables(taskInstanceId);

								if (!binaryVariables.isEmpty()) {

									for (ActorPermissions perm : perms) {

										if (taskInstanceId.equals(perm
										        .getTaskInstanceId())
										        && perm.getVariableIdentifier() != null) {

											for (Iterator<BinaryVariable> iterator = binaryVariables
											        .iterator(); iterator
											        .hasNext();) {
												BinaryVariable binVar = iterator
												        .next();

												if (perm
												        .getVariableIdentifier()
												        .equals(
												            binVar.getHash()
												                    .toString())) {

													perm
													        .setReadPermission(setReadPermission);
													perm
													        .setWritePermission(setWritePermission);
													perm = getBpmDAO().merge(
													    perm);
													iterator.remove();
												}
											}
										}
									}

									// creating non existent permissions for
									// vars
									for (BinaryVariable binVar : binaryVariables) {

										tiPerm = new ActorPermissions();
										tiPerm
										        .setRoleName(actorToSetPermissionTo
										                .getProcessRoleName());
										tiPerm.setTaskId(ti.getTask().getId());
										tiPerm
										        .setTaskInstanceId(taskInstanceId);
										tiPerm.setVariableIdentifier(binVar
										        .getHash().toString());
										tiPerm
										        .setWritePermission(setWritePermission);
										tiPerm
										        .setReadPermission(setReadPermission);
										tiPerm
										        .setActors(actorsToSetPermissionsTo);

										getBpmDAO().persist(tiPerm);
										perms.add(tiPerm);
									}
								}
							}
						}

						getBpmDAO().merge(actorToSetPermissionTo);
					}
				}
				return null;
			}
		});
	}

	protected boolean fallsInGroups(int userId, List<NativeIdentityBind> nativeIdentities) {
		try {
			UserBusiness ub = getUserBusiness();
			Collection<Group> userGroups = ub.getUserGroups(userId);

			if (userGroups != null) {
				for (Group group : userGroups) {
					String groupId = group.getPrimaryKey().toString();

					for (NativeIdentityBind nativeIdentity : nativeIdentities) {
						if (nativeIdentity.getIdentityType() == IdentityType.GROUP && nativeIdentity.getIdentityId().equals(groupId))
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

	protected boolean fallsInRoles(int userId,
	        List<NativeIdentityBind> nativeIdentities) {

		try {
			User usr = getUserBusiness().getUser(userId);
			Set<String> roles = getAccessController().getAllRolesForUser(usr);

			for (NativeIdentityBind nativeIdentity : nativeIdentities) {

				if (nativeIdentity.getIdentityType() == IdentityType.ROLE
				        && roles.contains(nativeIdentity.getIdentityId()))
					return true;
			}

		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
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
			return IBOLookup.getServiceInstance(
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

	@Override
	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}

	@Override
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

	protected BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Override
	public boolean canSeeComments(Long processInstanceId, User user) {
		if (user == null) {
			return false;
		}

		List<ActorPermissions> perms = null;
		try {
			perms = getBpmDAO().getResultList(ActorPermissions.getSetByProcessInstanceIdAndCanSeeComments, ActorPermissions.class,
					new Param(Actor.processInstanceIdProperty, processInstanceId)
			);
		} catch(Exception e) {
			logger.log(Level.WARNING, "Error getting permissions", e);
		}
		if (ListUtil.isEmpty(perms)) {
			return false;
		}

		for (ActorPermissions permission: perms) {
			Boolean canSeeComments = permission.getCanSeeComments();
			if (canSeeComments != null && canSeeComments) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean canWriteComments(Long processInstanceId, User user) {
		if (user == null) {
			return false;
		}

		List<ActorPermissions> perms = null;
		try {
			perms = getBpmDAO().getResultList(ActorPermissions.getSetByProcessInstanceIdAndCanWriteComments, ActorPermissions.class,
					new Param(Actor.processInstanceIdProperty, processInstanceId)
			);
		} catch(Exception e) {
			logger.log(Level.WARNING, "Error getting permissions", e);
		}
		if (ListUtil.isEmpty(perms)) {
			return false;
		}

		for (ActorPermissions permission: perms) {
			Boolean canWriteComments = permission.getCanWriteComments();
			if (canWriteComments != null && canWriteComments) {
				return true;
			}
		}

		return false;
	}

	@Override
	@Transactional(readOnly = false)
	public boolean doDisableAttachmentForAllRoles(final Integer fileHash, final Long processInstanceId, final Long taskInstanceId) {
		if (fileHash == null && taskInstanceId == null) {
			return false;
		}

		return getBpmContext().execute(new JbpmCallback<Boolean>() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance taskInstance = context.getTaskInstance(taskInstanceId);
				if (taskInstance == null) {
					return false;
				}

				ProcessInstanceW processInstance = null;
				if (processInstanceId == null) {
					processInstance = getBpmFactory().getTaskInstanceW(taskInstanceId).getProcessInstanceW();
				} else {
					processInstance = getBpmFactory().getProcessInstanceW(processInstanceId);
				}
				if (processInstance == null) {
					return false;
				}

				if (!processInstance.hasRight(Right.processHandler) && !processInstance.hasHandlerAssignmentSupport()) {
					return false;
				}

				Task task = null;
				List<ActorPermissions> perms = null;
				try {
					task = taskInstance.getTask();
					Long taskId = task.getId();

					perms = getBpmDAO().getResultList(ActorPermissions.getSetByTaskIdOrTaskInstanceId, ActorPermissions.class,
							new Param(ActorPermissions.taskInstanceIdProperty, taskInstanceId),
							new Param(ActorPermissions.taskIdProperty, taskId)
					);
				} catch(Exception e) {
					logger.log(Level.WARNING, "Error getting permissions for attachment. File hash: " + fileHash + ", proc. inst. ID: "
							+ processInstanceId + ", task instance ID: " + taskInstanceId + ", task: " + task, e);
				}
				if (ListUtil.isEmpty(perms)) {
					return false;
				}

				String variableIdentifier = String.valueOf(fileHash);
				for (ActorPermissions permissions: perms) {
					Boolean canSeeAttachments = permissions.getCanSeeAttachments();
					if (canSeeAttachments != null && canSeeAttachments) {
						String varId = permissions.getVariableIdentifier();
						if (!StringUtil.isEmpty(varId) && variableIdentifier.equals(varId)) {
							permissions.setCanSeeAttachments(Boolean.FALSE);
							permissions.setReadPermission(Boolean.FALSE);
							permissions.setCanSeeAttachmentsOfRoleName("all");
							getBpmDAO().persist(permissions);
						}
					}
				}

				return true;
			}
		});
	}
}