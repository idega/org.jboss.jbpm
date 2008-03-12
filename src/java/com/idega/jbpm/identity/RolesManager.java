package com.idega.jbpm.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.idega.jbpm.data.ProcessRole;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/03/12 15:43:02 $ by $Author: civilis $
 */
public interface RolesManager {

	public abstract List<ProcessRole> createRolesByProcessInstance(
			Map<String, Role> roles, long processInstanceId);

	public abstract void addGroupsToRoles(Long actorId, Collection<String> groupsIds, Long processInstanceId, Long processDefinitionId);
	
	public abstract List<ProcessRole> getGeneralRoles();
	
	public abstract void createIdentitiesForRoles(List<ProcessRole> processRoles, int userId);
}