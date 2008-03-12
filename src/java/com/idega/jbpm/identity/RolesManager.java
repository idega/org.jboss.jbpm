package com.idega.jbpm.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.idega.jbpm.data.ProcessRole;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/03/12 11:43:55 $ by $Author: civilis $
 */
public interface RolesManager {

	public abstract List<ProcessRole> createRolesByProcessInstance(
			Map<String, Role> roles, long processInstanceId);

	public abstract void assignTaskAccesses(long taskInstanceId,
			List<ProcessRole> processRoles, Map<String, Role> roles);

	public abstract void addGroupsToRoles(Long actorId, Collection<String> groupsIds, Long processInstanceId, Long processDefinitionId);
	
	public abstract List<ProcessRole> getGeneralRoles();
}