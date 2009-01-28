package com.idega.jbpm.identity.permission;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.security.AuthenticationService;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.persistence.Param;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $ Last modified: $Date: 2009/01/28 21:09:51 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class TaskAccessPermissionsHandler implements BPMTypedHandler {
	
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private RolesManager rolesManager;
	
	public static final String taskInstanceAtt = "taskInstance";
	public static final String checkOnlyInActorsPoolAtt = "checkOnlyInActorsPool";
	public static final String accessesWantedAtt = "accessesWanted";
	public static final String variableIdentifierAtt = "variableIdentifier";
	
	public String[] getHandledTypes() {
		return new String[] {
		        PermissionsFactoryImpl.submitTaskParametersPermType,
		        PermissionsFactoryImpl.viewTaskParametersPermType,
		        PermissionsFactoryImpl.viewTaskVariableParametersType };
	}
	
	public void handle(Permission perm) throws AccessControlException {
		
		if (!(perm instanceof BPMTypedPermission)) {
			
			throw new IllegalArgumentException("Unsupported permission type="
			        + perm.getClass().getName());
		}
		
		BPMTypedPermission permission = (BPMTypedPermission) perm;
		
		Integer userId = permission.getUserId();
		
		if (userId == null) {
			
			String loggedInActorId = getAuthenticationService().getActorId();
			
			if (loggedInActorId == null)
				throw new AccessControlException("Not logged in");
			
			userId = new Integer(loggedInActorId);
		}
		
		TaskInstance taskInstance = permission.getAttribute(taskInstanceAtt);
		
		if (userId.toString().equals(taskInstance.getActorId()))
			return;
		
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
				
				if (PermissionsFactoryImpl.viewTaskVariableParametersType
				        .equals(permission.getType())) {
					
					String variableIdentifier = permission
					        .getAttribute(variableIdentifierAtt);
					
					if (variableIdentifier == null
					        || CoreConstants.EMPTY.equals(variableIdentifier))
						throw new IllegalArgumentException(
						        "Illegal permission passed. Passed of type="
						                + PermissionsFactoryImpl.viewTaskVariableParametersType
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
	
	// TODO: make this more effective, as it was changed in a rush
	protected void checkPermissionsForTaskInstance(int userId,
	        TaskInstance taskInstance, Collection<Access> accesses,
	        String variableIdentifier) throws AccessControlException {
		
		Long taskId = taskInstance.getTask().getId();
		Long taskInstanceId = taskInstance.getId();
		
		ProcessInstance mainProcessInstance = taskInstance.getProcessInstance();
		
		Long taskInstanceProcessInstanceId = mainProcessInstance.getId();
		
		if (mainProcessInstance.getSuperProcessToken() != null) {
			
			// actors are bound with main process instance, therefore we find it here (in case task
			// instance is in subprocess)
			
			while (mainProcessInstance.getSuperProcessToken() != null) {
				mainProcessInstance = mainProcessInstance
				        .getSuperProcessToken().getProcessInstance();
			}
		}
		
		Long processInstanceId = mainProcessInstance.getId();
		
		BPMDAO bpmDAO = getBpmBindsDAO();
		User user = getRolesManager().getUser(userId);
		String processName = null;
		Set<String> userGroupsIds = null;
		
		Set<String> nativeRoles = getRolesManager().getUserNativeRoles(user);
		
		List<ActorPermissions> userPermissions = bpmDAO.getPermissionsForUser(
		    userId, processName, processInstanceId, nativeRoles, userGroupsIds);
		
		if(!processInstanceId.equals(taskInstanceProcessInstanceId)) {
//			this is backward compatibily for subprocesses tasks, which created actors not for the outermost parent, but for the subprocess pids
//			20090128
			
//			TODO: add warning (and possibly send email here) for subprocesses, that are unknown, and have permissions for the subprocess (simply, there are actors for the subprocess)
			
			userPermissions.addAll(bpmDAO.getPermissionsForUser(
				    userId, null, taskInstanceProcessInstanceId, nativeRoles, userGroupsIds));
		}
		
		boolean checkWriteAccess = accesses.contains(Access.write);
		boolean checkReadAccess = accesses.contains(Access.read);
		Boolean hasTaskInstanceScopeAccess = null;
		Boolean hasTaskInstanceScopeANDVarAccess = null;
		Boolean hasTaskScopeAccess = null;
		Boolean hasTaskScopeANDVarAccess = null;
		
		for (ActorPermissions perm : userPermissions) {
			
			if (taskInstanceId.equals(perm.getTaskInstanceId())) {
				
				if (variableIdentifier != null) {
					
					if (variableIdentifier.equals(perm.getVariableIdentifier())) {
						
						hasTaskInstanceScopeANDVarAccess = (!checkReadAccess || (perm
						        .getReadPermission() != null && perm
						        .getReadPermission()))
						        && (!checkWriteAccess || (perm
						                .getWritePermission() != null && perm
						                .getWritePermission()));
						break;
					}
				}
				
				if (perm.getVariableIdentifier() == null) {
					
					hasTaskInstanceScopeAccess = (!checkReadAccess || (perm
					        .getReadPermission() != null && perm
					        .getReadPermission()))
					        && (!checkWriteAccess || (perm.getWritePermission() != null && perm
					                .getWritePermission()));
					
					if (variableIdentifier == null)
						break;
				}
				
			} else if (taskId.equals(perm.getTaskId())
			        && perm.getTaskInstanceId() == null) {
				
				if (variableIdentifier != null) {
					
					if (variableIdentifier.equals(perm.getVariableIdentifier())) {
						
						hasTaskScopeANDVarAccess = (!checkReadAccess || (perm
						        .getReadPermission() != null && perm
						        .getReadPermission()))
						        && (!checkWriteAccess || (perm
						                .getWritePermission() != null && perm
						                .getWritePermission()));
					}
				}
				
				if (perm.getVariableIdentifier() == null) {
					
					hasTaskScopeAccess = (!checkReadAccess || (perm
					        .getReadPermission() != null && perm
					        .getReadPermission()))
					        && (!checkWriteAccess || (perm.getWritePermission() != null && perm
					                .getWritePermission()));
				}
			}
		}
		
		if ((variableIdentifier != null && ((hasTaskInstanceScopeANDVarAccess != null && hasTaskInstanceScopeANDVarAccess)
		        || (hasTaskInstanceScopeANDVarAccess == null
		                && hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess)
		        || (hasTaskInstanceScopeANDVarAccess == null
		                && hasTaskInstanceScopeAccess == null
		                && hasTaskScopeANDVarAccess != null && hasTaskScopeANDVarAccess) || (hasTaskInstanceScopeANDVarAccess == null
		        && hasTaskInstanceScopeAccess == null
		        && hasTaskScopeANDVarAccess == null
		        && hasTaskScopeAccess != null && hasTaskScopeAccess)))
		        || (variableIdentifier == null && ((hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess) || (hasTaskInstanceScopeAccess == null
		                && hasTaskScopeAccess != null && hasTaskScopeAccess)))) {
			
			// access granted
			return;
		}
		
		if (true)
			throw new AccessControlException("No permission for user=" + userId
			        + ", for taskInstance=" + taskInstance.getId()
			        + ", variableIdentifier=" + variableIdentifier);
		
		final List<ActorPermissions> allPerms = getBpmBindsDAO().getResultList(
		    ActorPermissions.getSetByTaskIdOrTaskInstanceId,
		    ActorPermissions.class,
		    new Param(ActorPermissions.taskIdProperty, taskId),
		    new Param(ActorPermissions.taskInstanceIdProperty, taskInstanceId));
		
		if (allPerms != null) {
			
			// boolean checkWriteAccess = accesses.contains(Access.write);
			// boolean checkReadAccess = accesses.contains(Access.read);
			
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			
			AccessController ac = getAccessController();
			
			HashSet<Actor> roles = new HashSet<Actor>(allPerms.size() + 10);
			
			for (ActorPermissions perm : allPerms) {
				
				if (perm.getActors() != null)
					roles.addAll(perm.getActors());
			}
			
			for (Actor processRole : roles) {
				
				List<ActorPermissions> perms = processRole
				        .getActorPermissions();
				
				if (perms != null) {
					
					// Boolean hasTaskInstanceScopeAccess = null;
					// Boolean hasTaskInstanceScopeANDVarAccess = null;
					// Boolean hasTaskScopeAccess = null;
					// Boolean hasTaskScopeANDVarAccess = null;
					
					for (ActorPermissions perm : perms) {
						
						if (taskInstanceId.equals(perm.getTaskInstanceId())) {
							
							if (variableIdentifier != null) {
								
								if (variableIdentifier.equals(perm
								        .getVariableIdentifier())) {
									
									hasTaskInstanceScopeANDVarAccess = (!checkReadAccess || (perm
									        .getReadPermission() != null && perm
									        .getReadPermission()))
									        && (!checkWriteAccess || (perm
									                .getWritePermission() != null && perm
									                .getWritePermission()));
									break;
								}
							}
							
							if (perm.getVariableIdentifier() == null) {
								
								hasTaskInstanceScopeAccess = (!checkReadAccess || (perm
								        .getReadPermission() != null && perm
								        .getReadPermission()))
								        && (!checkWriteAccess || (perm
								                .getWritePermission() != null && perm
								                .getWritePermission()));
								
								if (variableIdentifier == null)
									break;
							}
							
						} else if (taskId.equals(perm.getTaskId())
						        && perm.getTaskInstanceId() == null) {
							
							if (variableIdentifier != null) {
								
								if (variableIdentifier.equals(perm
								        .getVariableIdentifier())) {
									
									hasTaskScopeANDVarAccess = (!checkReadAccess || (perm
									        .getReadPermission() != null && perm
									        .getReadPermission()))
									        && (!checkWriteAccess || (perm
									                .getWritePermission() != null && perm
									                .getWritePermission()));
								}
							}
							
							if (perm.getVariableIdentifier() == null) {
								
								hasTaskScopeAccess = (!checkReadAccess || (perm
								        .getReadPermission() != null && perm
								        .getReadPermission()))
								        && (!checkWriteAccess || (perm
								                .getWritePermission() != null && perm
								                .getWritePermission()));
							}
						}
					}
					
					if ((variableIdentifier != null && ((hasTaskInstanceScopeANDVarAccess != null && hasTaskInstanceScopeANDVarAccess)
					        || (hasTaskInstanceScopeANDVarAccess == null
					                && hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess)
					        || (hasTaskInstanceScopeANDVarAccess == null
					                && hasTaskInstanceScopeAccess == null
					                && hasTaskScopeANDVarAccess != null && hasTaskScopeANDVarAccess) || (hasTaskInstanceScopeANDVarAccess == null
					        && hasTaskInstanceScopeAccess == null
					        && hasTaskScopeANDVarAccess == null
					        && hasTaskScopeAccess != null && hasTaskScopeAccess)))
					        || (variableIdentifier == null && ((hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess) || (hasTaskInstanceScopeAccess == null
					                && hasTaskScopeAccess != null && hasTaskScopeAccess)))) {
						
						List<NativeIdentityBind> nativeIdentities = processRole
						        .getNativeIdentities();
						
						if (getRolesManager().checkFallsInRole(
						    processRole.getProcessRoleName(), nativeIdentities,
						    userId, ac, iwac))
							return;
					}
				}
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
	
	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}
	
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
	
	private IWMainApplication getIWMA() {
		
		IWMainApplication iwma;
		FacesContext fctx = FacesContext.getCurrentInstance();
		
		if (fctx == null)
			iwma = IWMainApplication.getDefaultIWMainApplication();
		else
			iwma = IWMainApplication.getIWMainApplication(fctx);
		
		return iwma;
	}
	
	private AccessController getAccessController() {
		
		return getIWMA().getAccessController();
	}
	
	public RolesManager getRolesManager() {
		return rolesManager;
	}
	
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
}