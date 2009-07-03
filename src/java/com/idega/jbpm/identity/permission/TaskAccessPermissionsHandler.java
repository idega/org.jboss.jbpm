package com.idega.jbpm.identity.permission;

import java.security.Permission;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.security.AuthenticationService;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionHandleResult.PermissionHandleResultStatus;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $ Last modified: $Date: 2009/07/03 08:58:56 $ by $Author: valdas $
 */
@Scope("singleton")
@Service
@Transactional(readOnly = true)
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
	
	// TODO: just return handleResult object here with status, and message, let
	// identityAuthorizationService throw exceptions or so
	@Transactional(readOnly = true)
	public PermissionHandleResult handle(Permission perm) {
		
		if (!(perm instanceof BPMTypedPermission)) {
			
			throw new IllegalArgumentException("Unsupported permission type="
			        + perm.getClass().getName());
		}
		
		PermissionHandleResult result = null;
		
		BPMTypedPermission permission = (BPMTypedPermission) perm;
		
		Integer userId = permission.getUserId();
		
		if (userId == null) {
			
			String loggedInActorId = getAuthenticationService().getActorId();
			
			if (loggedInActorId == null) {
				
				if (PermissionsFactoryImpl.submitTaskParametersPermType
				        .equals(permission.getType())
				        || PermissionsFactoryImpl.viewTaskInstanceVariablePermType
				                .equals(permission.getType()))
					result = new PermissionHandleResult(
					        PermissionHandleResultStatus.hasAccess);
				else
					result = new PermissionHandleResult(
					        PermissionHandleResultStatus.noAccess,
					        "Not logged in");
			} else {
				userId = new Integer(loggedInActorId);
			}
		}
		
		if (result == null) {
			
			TaskInstance taskInstance = permission
			        .getAttribute(taskInstanceAtt);
			
			// if (userId.toString().equals(taskInstance.getActorId()))
			// return;
			
			Boolean checkOnlyInActorsPool = permission
			        .getAttribute(checkOnlyInActorsPoolAtt);
			
			if (!taskInstance.hasEnded() && taskInstance.getActorId() != null
			        && !checkOnlyInActorsPool) {
				
				if (!userId.toString().equals(taskInstance.getActorId())) {
					
					result = new PermissionHandleResult(
					        PermissionHandleResultStatus.noAccess,
					        "You shall not pass. Logged in actor id doesn't match the assigned actor id. Assigned: "
					                + taskInstance.getActorId()
					                + ", taskInstanceId: "
					                + taskInstance.getId());
				} else
					result = new PermissionHandleResult(
					        PermissionHandleResultStatus.hasAccess);
				
			} else {
				
				// super admin always gets an access
				if (IWContext.getCurrentInstance().isSuperAdmin()) {
					
					result = new PermissionHandleResult(
					        PermissionHandleResultStatus.hasAccess);
					
				} else {
					
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
						
						result = checkPermissionsForTaskInstance(userId,
						    taskInstance, accessesWanted, variableIdentifier);
					} else {
						
						result = checkPermissionsForTaskInstance(userId,
						    taskInstance, accessesWanted, null);
					}
				}
			}
		}
		
		if (result == null) {
			
			result = new PermissionHandleResult(
			        PermissionHandleResultStatus.noAccess);
			Logger
			        .getLogger(getClass().getName())
			        .log(
			            Level.WARNING,
			            "In handling permission ("
			                    + perm
			                    + "), no result at the end retrieved. Returning no access");
		}
		
		return result;
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
	
	@Transactional(readOnly = true)
	protected PermissionHandleResult checkPermissionsForTaskInstance(
	        int userId, TaskInstance taskInstance, Collection<Access> accesses,
	        String variableIdentifier) {
		
		Long taskId = taskInstance.getTask().getId();
		Long taskInstanceId = taskInstance.getId();
		
		Long taskInstanceProcessInstanceId = taskInstance.getProcessInstance()
		        .getId();
		
		ProcessInstance mainProcessInstance = getBpmFactory()
		        .getMainProcessInstance(taskInstanceProcessInstanceId);
		
		Long mainProcessInstanceId = mainProcessInstance.getId();
		
		BPMDAO bpmDAO = getBpmDAO();
		User user = getRolesManager().getUser(userId);
		String processName = null;
		Set<String> userGroupsIds = null;
		
		Set<String> nativeRoles = getRolesManager().getUserNativeRoles(user);
		
		List<ActorPermissions> userPermissions = bpmDAO.getPermissionsForUser(
		    userId, processName, mainProcessInstanceId, nativeRoles,
		    userGroupsIds);
		
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
					
					if (perm.getCanSeeAttachments() != null && perm.getCanSeeAttachments().booleanValue() &&
							!StringUtil.isEmpty(perm.getCanSeeAttachmentsOfRoleName())) {
						if (variableIdentifier.equals(perm.getVariableIdentifier())) {
							boolean hasTaskInstanceScopeAndAttachmentsAccess = false;
							AccessController accessControler = IWMainApplication.getDefaultIWMainApplication().getAccessController();
							hasTaskInstanceScopeAndAttachmentsAccess = accessControler.hasRole(user, perm.getCanSeeAttachmentsOfRoleName());
							
							putAccess(accessesForRoles, hasTaskInstanceScopeAndAttachmentsAccess, TI_VAR, perm.getCanSeeAttachmentsOfRoleName());
							if (hasTaskInstanceScopeAndAttachmentsAccess)
								// we have the permission for attachments, so we can search no more
								break;
						}
					} else if (variableIdentifier.equals(perm.getVariableIdentifier())) {
						
						boolean hasTaskInstanceScopeANDVarAccess = hasAccess(
						    perm, checkReadAccess, checkWriteAccess);
						
						putAccess(accessesForRoles,
						    hasTaskInstanceScopeANDVarAccess, TI_VAR, roleName);
						
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
		
		PermissionHandleResult result = null;
		
		Iterator<Boolean[]> accessesForRolesIterator = accessesForRoles
		        .values().iterator();
		
		while (accessesForRolesIterator.hasNext() && result == null) {
			Boolean[] accMtrx = accessesForRolesIterator.next();
			
			Boolean hasTaskInstanceScopeANDVarAccess = accMtrx[TI_VAR];
			Boolean hasTaskInstanceScopeAccess = accMtrx[TI];
			Boolean hasTaskScopeANDVarAccess = accMtrx[TA_VAR];
			Boolean hasTaskScopeAccess = accMtrx[TA];
			
			// WARNING: Boolean null, and false have different meanings here!
			
			if (
			// for variable
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
			        
			        // for taskInstance
			        || (variableIdentifier == null && ((hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess) || (hasTaskInstanceScopeAccess == null
			                && hasTaskScopeAccess != null && hasTaskScopeAccess)))) {
				
				// access granted
				result = new PermissionHandleResult(
				        PermissionHandleResultStatus.hasAccess);
			}
		}
		
		if (result == null)
			result = new PermissionHandleResult(
			        PermissionHandleResultStatus.noAccess,
			        "No permission for user=" + userId + ", for taskInstance="
			                + taskInstance.getId() + ", variableIdentifier="
			                + variableIdentifier);
		
		return result;
	}
	
	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}
	
	public void setAuthenticationService(
	        AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	// private IWMainApplication getIWMA() {
	//
	// IWMainApplication iwma;
	// FacesContext fctx = FacesContext.getCurrentInstance();
	//
	// if (fctx == null)
	// iwma = IWMainApplication.getDefaultIWMainApplication();
	// else
	// iwma = IWMainApplication.getIWMainApplication(fctx);
	//
	// return iwma;
	// }
	
	// private AccessController getAccessController() {
	//
	// return getIWMA().getAccessController();
	// }
	
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