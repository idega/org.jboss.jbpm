package com.idega.jbpm.identity.authorization;

import java.security.Permission;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.business.BPMPointcuts;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.permission.PermissionsFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 * 
 *          Last modified: $Date: 2009/02/07 18:20:55 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
@Aspect
public class IdentityAuthorizationAspect {

	@Autowired
	private BPMContext idegaJbpmContext;
	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private AuthorizationService authorizationService;
	@Autowired
	private PermissionsFactory permissionsFactory;

	/*
	 * @Before(BPMPointcuts.startProcessAtProcessManager+" && args(processDefinitionId, ..)"
	 * ) public void checkPermissionToStartProcess(JoinPoint p, long
	 * processDefinitionId) {
	 * 
	 * // FIXME: permitting everyone to start process }
	 */

	@Transactional(readOnly = true)
	@Before("(" + BPMPointcuts.loadViewAtTaskInstanceW + " || "
			+ BPMPointcuts.submitAtTaskInstanceW + ")")
	public void checkPermissionToTaskInstance(JoinPoint jp) {

		Object jpThis = jp.getThis();

		if (!(jpThis instanceof TaskInstanceW))
			throw new IllegalArgumentException("Only objects of "
					+ TaskInstanceW.class.getName() + " supported, got: "
					+ jpThis.getClass().getName());

		final TaskInstanceW tiw = (TaskInstanceW) jpThis;

		getIdegaJbpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				TaskInstance taskInstance = tiw.getTaskInstance();
				Permission permission;

				if (taskInstance.hasEnded()) {
					permission = getPermissionsFactory().getTaskViewPermission(
							false, taskInstance);

				} else {
					permission = getPermissionsFactory()
							.getTaskInstanceSubmitPermission(false,
									taskInstance);
				}

				getAuthorizationService().checkPermission(permission);
				return null;
			}
		});
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	public void setAuthorizationService(
			AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}
}