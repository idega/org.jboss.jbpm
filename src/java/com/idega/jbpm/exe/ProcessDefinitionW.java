package com.idega.jbpm.exe;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

import com.idega.block.process.variables.Variable;
import com.idega.core.accesscontrol.data.ICRole;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.presentation.IWContext;
import com.idega.user.data.Group;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 *
 *          Last modified: $Date: 2009/02/16 22:02:39 $ by $Author: donatas $
 */
public interface ProcessDefinitionW {

	public static final String VARIABLE_MANAGER_ROLE_NAME = "managerRoleName";

	public abstract Object doPrepareProcess(Map<String, Object> parameters);

	/**
	 * Starts process
	 * @param viewSubmission
	 * @return process instance ID
	 */
	public abstract <T extends Serializable> T startProcess(ViewSubmission viewSubmission);

	public abstract View loadInitView(Integer initiatorId);
	public abstract View loadInitView(Integer initiatorId, String identifier);

	public abstract <T extends Serializable> void setProcessDefinitionId(T processDefinitionId);

	public abstract <T extends Serializable> T getProcessDefinitionId();

	public abstract <T> T getProcessDefinition();
	public abstract <T> T getProcessDefinition(JbpmContext context);

	public abstract void setRolesCanStartProcess(List<String> roles, Object context);

	public abstract List<String> getRolesCanStartProcess(Object context);

	public abstract String getStartTaskName();

	public abstract List<Variable> getTaskVariableList(String taskName);

	public abstract List<Variable> getTaskVariableWithAccessesList(String taskName);

	public abstract Collection<String> getTaskNodeTransitionsNames(String taskName);

	public abstract String getProcessName(Locale locale);

	public abstract String getProcessDefinitionName();

	/**
	 *
	 * <p>Checks if given {@link User} has cases </p>
	 * @param user which should be in {@link Group} with {@link ICRole} of
	 * manager in {@link ProcessDefinition}, not <code>null</code>;
	 * @return <code>true</code> if {@link User} has managers role,
	 * <code>false</code> otherwise;
	 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
	 */
	public boolean hasManagerRole(User user);

	public boolean isAvailable(IWContext iwc);

	public String getNotAvailableLink(IWContext iwc);

}