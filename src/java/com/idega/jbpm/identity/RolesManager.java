package com.idega.jbpm.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.idega.jbpm.data.ProcessRole;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/03/11 20:14:26 $ by $Author: civilis $
 */
public interface RolesManager {

	public abstract List<ProcessRole> createRolesByProcessInstance(
			Map<String, Role> roles, long processInstanceId);

	public abstract void assignTaskAccesses(long taskInstanceId,
			List<ProcessRole> processRoles, Map<String, Role> roles);

	public abstract void addGroupsToRoles(Long actorId, Collection<String> groupsIds, Long processInstanceId, Long processDefinitionId);
}