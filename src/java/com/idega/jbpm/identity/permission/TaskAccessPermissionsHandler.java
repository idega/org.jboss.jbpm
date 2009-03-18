package com.idega.jbpm.identity.permission;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.security.AuthenticationService;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $ Last modified: $Date: 2009/03/18 20:12:21 $ by
 *          $Author: civilis $
 */
@Scope("singleton")
@Service
@Transactional(readOnly = true, noRollbackFor = AccessControlException.class)
public class TaskAccessPermissionsHandler implements BPMTypedHandler {

	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private BPMDAO bpmDAO;
	@Autowired
	private RolesManager rolesManager;
	@Autowired
	private BPMFactory bpmFactory;

	public static final String taskInstanceAtt = "taskInstance";
	public static final String checkOnlyInActorsPoolAtt = "checkOnlyInActorsPool";
	public static final String accessesWantedAtt = "accessesWanted";
	public static final String variableIdentifierAtt = "variableIdentifier";

	private static final int TI_VAR = 0;
	private static final int TI = 1;
	private static final int TA_VAR = 2;
	private static final int TA = 3;

	public String[] getHandledTypes() {
		return new String[] {
				PermissionsFactoryImpl.submitTaskParametersPermType,
				PermissionsFactoryImpl.viewTaskInstancePermType,
				PermissionsFactoryImpl.viewTaskInstanceVariablePermType };
	}

	@Transactional(readOnly = true, noRollbackFor = AccessControlException.class)
	public void handle(Permission perm) throws AccessControlException {

		if (!(perm instanceof BPMTypedPermission)) {

			throw new IllegalArgumentException("Unsupported permission type="
					+ perm.getClass().getName());
		}

		BPMTypedPermission permission = (BPMTypedPermission) perm;

		Integer userId = permission.getUserId();

		if (userId == null) {

			String loggedInActorId = getAuthenticationService().getActorId();

			if (loggedInActorId == null) {
			
				if(PermissionsFactoryImpl.submitTaskParametersPermType.equals(permission.getType()))
					return;
				else
					throw new AccessControlException("Not logged in");
			}

			userId = new Integer(loggedInActorId);
		}

		TaskInstance taskInstance = permission.getAttribute(taskInstanceAtt);

//		if (userId.toString().equals(taskInstance.getActorId()))
//			return;

		Boolean checkOnlyInActorsPool = permission
				.getAttribute(checkOnlyInActorsPoolAtt);

		if (!taskInstance.hasEnded() && taskInstance.getActorId() != null
				&& !checkOnlyInActorsPool) {

			if (!userId.toString().equals(taskInstance.getActorId()))
				throw new AccessControlException(
						"You shall not pass. Logged in actor id doesn't match the assigned actor id. Assigned: "
								+ taskInstance.getActorId()
								+ ", taskInstanceId: " + taskInstance.getId());

		} else {

			// super admin always gets an access
			if (!IWContext.getCurrentInstance().isSuperAdmin()) {

				List<Access> accessesWanted = permission
						.getAttribute(accessesWantedAtt);

				if (PermissionsFactoryImpl.viewTaskInstanceVariablePermType
						.equals(permission.getType())) {

					String variableIdentifier = permission
							.getAttribute(variableIdentifierAtt);

					if (StringUtil.isEmpty(variableIdentifier))
						throw new IllegalArgumentException(
								"Illegal permission passed. Passed of type="
										+ PermissionsFactoryImpl.viewTaskInstanceVariablePermType
										+ ", but not variable identifier provided");

					checkPermissionsForTaskInstance(userId, taskInstance,
							accessesWanted, variableIdentifier);
				} else {

					checkPermissionsForTaskInstance(userId, taskInstance,
							accessesWanted, null);
				}
			}
		}
	}

	private boolean hasAccess(ActorPermissions perm, boolean checkReadAccess,
			boolean checkWriteAccess) {

		return (!checkReadAccess || (perm.getReadPermission() != null && perm
				.getReadPermission()))
				&& (!checkWriteAccess || (perm.getWritePermission() != null && perm
						.getWritePermission()));
	}

	private void putAccess(Map<String, Boolean[]> accesses, boolean hasAccess,
			int scope, String roleName) {

		final Boolean[] accMtrx;

		if (!accesses.containsKey(roleName)) {

			accMtrx = new Boolean[4];
			accesses.put(roleName, accMtrx);
		} else {

			accMtrx = accesses.get(roleName);
		}

		accMtrx[scope] = hasAccess;
	}

	@Transactional(readOnly = true, noRollbackFor = AccessControlException.class)
	protected void checkPermissionsForTaskInstance(int userId,
			TaskInstance taskInstance, Collection<Access> accesses,
			String variableIdentifier) throws AccessControlException {

		Long taskId = taskInstance.getTask().getId();
		Long taskInstanceId = taskInstance.getId();

		Long taskInstanceProcessInstanceId = taskInstance.getProcessInstance().getId();
		
		ProcessInstance mainProcessInstance = getBpmFactory().getMainProcessInstance(taskInstanceProcessInstanceId);
		
		Long mainProcessInstanceId = mainProcessInstance.getId();

		BPMDAO bpmDAO = getBpmDAO();
		User user = getRolesManager().getUser(userId);
		String processName = null;
		Set<String> userGroupsIds = null;

		Set<String> nativeRoles = getRolesManager().getUserNativeRoles(user);

		List<ActorPermissions> userPermissions = bpmDAO.getPermissionsForUser(
				userId, processName, mainProcessInstanceId, nativeRoles,
				userGroupsIds);

		/*
		if (!mainProcessInstanceId.equals(taskInstanceProcessInstanceId)) {
			// this is backward compatibily for subprocesses tasks, which
			// created actors not for the outermost parent, but for the
			// subprocess pids
			// 20090128

			// TODO: add warning (and possibly send email here) for
			// subprocesses, that are unknown, and have permissions for the
			// subprocess (simply, there are actors for the subprocess)

			userPermissions.addAll(bpmDAO.getPermissionsForUser(userId, null,
					taskInstanceProcessInstanceId, nativeRoles, userGroupsIds));
		}
		*/

		boolean checkWriteAccess = accesses.contains(Access.write);
		boolean checkReadAccess = accesses.contains(Access.read);

		Map<String, Boolean[]> accessesForRoles = new HashMap<String, Boolean[]>(
				10);

		for (ActorPermissions perm : userPermissions) {

			String roleName = perm.getRoleName();

			if (taskInstanceId.equals(perm.getTaskInstanceId())) {

				// permission for task instance

				if (variableIdentifier != null) {

					// checking permission for variable

					if (variableIdentifier.equals(perm.getVariableIdentifier())) {

						boolean hasTaskInstanceScopeANDVarAccess = hasAccess(
								perm, checkReadAccess, checkWriteAccess);

						putAccess(accessesForRoles,
								hasTaskInstanceScopeANDVarAccess, TI_VAR,
								roleName);

						if (hasTaskInstanceScopeANDVarAccess)
							// we have the permission for variable, so we can
							// search no more
							break;
					}
				}

				if (perm.getVariableIdentifier() == null) {

					// check if we have access for task instance
					boolean hasTaskInstanceScopeAccess = hasAccess(perm,
							checkReadAccess, checkWriteAccess);

					putAccess(accessesForRoles, hasTaskInstanceScopeAccess, TI,
							roleName);

					if (hasTaskInstanceScopeAccess
							&& variableIdentifier == null)
						// we have access for task instance, and don't need
						// access for variable, so we can search no more
						break;
				}

			} else if (taskId.equals(perm.getTaskId())
					&& perm.getTaskInstanceId() == null) {

				// permission for task

				if (variableIdentifier != null) {

					// checking permission for variable in task scope

					if (variableIdentifier.equals(perm.getVariableIdentifier())) {

						boolean hasTaskScopeANDVarAccess = hasAccess(perm,
								checkReadAccess, checkWriteAccess);

						putAccess(accessesForRoles, hasTaskScopeANDVarAccess,
								TA_VAR, roleName);
					}
				}

				if (perm.getVariableIdentifier() == null) {

					// check if we have access for task
					boolean hasTaskScopeAccess = hasAccess(perm,
							checkReadAccess, checkWriteAccess);

					putAccess(accessesForRoles, hasTaskScopeAccess, TA,
							roleName);
				}
			}
		}

		for (Boolean[] accMtrx : accessesForRoles.values()) {

			Boolean hasTaskInstanceScopeANDVarAccess = accMtrx[TI_VAR];
			Boolean hasTaskInstanceScopeAccess = accMtrx[TI];
			Boolean hasTaskScopeANDVarAccess = accMtrx[TA_VAR];
			Boolean hasTaskScopeAccess = accMtrx[TA];
			
//			WARNING: Boolean null, and false have different meanings here!

			if (
					//for variable
					(variableIdentifier != null && ((hasTaskInstanceScopeANDVarAccess != null && hasTaskInstanceScopeANDVarAccess)
					|| 
					/*
					 * not checking for taskInstance scope access anymore (if not set for variable, defaulting to task access)
					 * (hasTaskInstanceScopeANDVarAccess == null
							&& hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess)
							|| */
					 (hasTaskInstanceScopeANDVarAccess == null
							&& hasTaskInstanceScopeAccess == null
							&& hasTaskScopeANDVarAccess != null && hasTaskScopeANDVarAccess) || (hasTaskInstanceScopeANDVarAccess == null
					&& hasTaskInstanceScopeAccess == null
					&& hasTaskScopeANDVarAccess == null
					&& hasTaskScopeAccess != null && hasTaskScopeAccess)))
					
					//for taskInstance
					|| (variableIdentifier == null && ((hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess) || (hasTaskInstanceScopeAccess == null
							&& hasTaskScopeAccess != null && hasTaskScopeAccess)))) {

				// access granted
				return;
			}
		}

		throw new AccessControlException("No permission for user=" + userId
				+ ", for taskInstance=" + taskInstance.getId()
				+ ", variableIdentifier=" + variableIdentifier);
	}

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	public void setAuthenticationService(
			AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

//	private IWMainApplication getIWMA() {
//
//		IWMainApplication iwma;
//		FacesContext fctx = FacesContext.getCurrentInstance();
//
//		if (fctx == null)
//			iwma = IWMainApplication.getDefaultIWMainApplication();
//		else
//			iwma = IWMainApplication.getIWMainApplication(fctx);
//
//		return iwma;
//	}

//	private AccessController getAccessController() {
//
//		return getIWMA().getAccessController();
//	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	protected BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	protected BPMDAO getBpmDAO() {
		return bpmDAO;
	}
}