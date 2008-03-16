package com.idega.jbpm.identity;

import java.security.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.exe.BPMAccessControlException;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 * 
 * Last modified: $Date: 2008/03/16 19:00:30 $ by $Author: civilis $
 */
public interface RolesManager {

	public abstract List<ProcessRole> createRolesByProcessInstance(
			Map<String, Role> roles, long processInstanceId);

	public abstract void addGroupsToRoles(Long actorId, Collection<String> groupsIds, Long processInstanceId, Long processDefinitionId);
	
	public abstract List<ProcessRole> getGeneralRoles();
	
	public abstract void createIdentitiesForRoles(List<ProcessRole> processRoles, int userId);
	
	public abstract void hasRightsToStartTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void hasRightsToAsssignTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void checkPermission(Permission permission) throws BPMAccessControlException;
}