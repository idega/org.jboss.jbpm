/**
 * @(#)TaskManagementServiceImpl.java    1.0.0 18:36:00
 *
 * Idega Software hf. Source Code Licence Agreement x
 *
 * This agreement, made this 10th of February 2006 by and between 
 * Idega Software hf., a business formed and operating under laws 
 * of Iceland, having its principal place of business in Reykjavik, 
 * Iceland, hereinafter after referred to as "Manufacturer" and Agura 
 * IT hereinafter referred to as "Licensee".
 * 1.  License Grant: Upon completion of this agreement, the source 
 *     code that may be made available according to the documentation for 
 *     a particular software product (Software) from Manufacturer 
 *     (Source Code) shall be provided to Licensee, provided that 
 *     (1) funds have been received for payment of the License for Software and 
 *     (2) the appropriate License has been purchased as stated in the 
 *     documentation for Software. As used in this License Agreement, 
 *     Licensee shall also mean the individual using or installing 
 *     the source code together with any individual or entity, including 
 *     but not limited to your employer, on whose behalf you are acting 
 *     in using or installing the Source Code. By completing this agreement, 
 *     Licensee agrees to be bound by the terms and conditions of this Source 
 *     Code License Agreement. This Source Code License Agreement shall 
 *     be an extension of the Software License Agreement for the associated 
 *     product. No additional amendment or modification shall be made 
 *     to this Agreement except in writing signed by Licensee and 
 *     Manufacturer. This Agreement is effective indefinitely and once
 *     completed, cannot be terminated. Manufacturer hereby grants to 
 *     Licensee a non-transferable, worldwide license during the term of 
 *     this Agreement to use the Source Code for the associated product 
 *     purchased. In the event the Software License Agreement to the 
 *     associated product is terminated; (1) Licensee's rights to use 
 *     the Source Code are revoked and (2) Licensee shall destroy all 
 *     copies of the Source Code including any Source Code used in 
 *     Licensee's applications.
 * 2.  License Limitations
 *     2.1 Licensee may not resell, rent, lease or distribute the 
 *         Source Code alone, it shall only be distributed as a 
 *         compiled component of an application.
 *     2.2 Licensee shall protect and keep secure all Source Code 
 *         provided by this this Source Code License Agreement. 
 *         All Source Code provided by this Agreement that is used 
 *         with an application that is distributed or accessible outside
 *         Licensee's organization (including use from the Internet), 
 *         must be protected to the extent that it cannot be easily 
 *         extracted or decompiled.
 *     2.3 The Licensee shall not resell, rent, lease or distribute 
 *         the products created from the Source Code in any way that 
 *         would compete with Idega Software.
 *     2.4 Manufacturer's copyright notices may not be removed from 
 *         the Source Code.
 *     2.5 All modifications on the source code by Licencee must 
 *         be submitted to or provided to Manufacturer.
 * 3.  Copyright: Manufacturer's source code is copyrighted and contains 
 *     proprietary information. Licensee shall not distribute or 
 *     reveal the Source Code to anyone other than the software 
 *     developers of Licensee's organization. Licensee may be held 
 *     legally responsible for any infringement of intellectual property 
 *     rights that is caused or encouraged by Licensee's failure to abide 
 *     by the terms of this Agreement. Licensee may make copies of the 
 *     Source Code provided the copyright and trademark notices are 
 *     reproduced in their entirety on the copy. Manufacturer reserves 
 *     all rights not specifically granted to Licensee.
 *
 * 4.  Warranty & Risks: Although efforts have been made to assure that the 
 *     Source Code is correct, reliable, date compliant, and technically 
 *     accurate, the Source Code is licensed to Licensee as is and without 
 *     warranties as to performance of merchantability, fitness for a 
 *     particular purpose or use, or any other warranties whether 
 *     expressed or implied. Licensee's organization and all users 
 *     of the source code assume all risks when using it. The manufacturers, 
 *     distributors and resellers of the Source Code shall not be liable 
 *     for any consequential, incidental, punitive or special damages 
 *     arising out of the use of or inability to use the source code or 
 *     the provision of or failure to provide support services, even if we 
 *     have been advised of the possibility of such damages. In any case, 
 *     the entire liability under any provision of this agreement shall be 
 *     limited to the greater of the amount actually paid by Licensee for the 
 *     Software or 5.00 USD. No returns will be provided for the associated 
 *     License that was purchased to become eligible to receive the Source 
 *     Code after Licensee receives the source code. 
 */
package com.idega.jbpm.business.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.business.TaskManagementService;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.permission.Access;
import com.idega.util.DBUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * <p>You can report about problems to: 
 * <a href="mailto:martynas@idega.is">Martynas Stakė</a></p>
 *
 * @version 1.0.0 2015 rugs. 21
 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
 */
/*
 * Spring
 */
@Service(TaskManagementServiceImpl.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_SINGLETON)
@EnableAspectJAutoProxy(proxyTargetClass=true)

/*
 * DWR
 */
@RemoteProxy(
		name=TaskManagementServiceImpl.JAVASCRIPT,
		creator=SpringCreator.class, 
		creatorParams={
			@Param(
					name="beanName", 
					value=TaskManagementServiceImpl.BEAN_NAME),
			@Param(
					name="javascript", 
					value=TaskManagementServiceImpl.JAVASCRIPT)
		}
)
public class TaskManagementServiceImpl extends DefaultSpringBean implements
		TaskManagementService {

	public static final String BEAN_NAME = "taskManagementService";
	public static final String JAVASCRIPT = "TaskManagementService";

	@Autowired
	private BPMContext bpmContext;

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private BPMDAO bpmDAO;

	private BPMDAO getBPMDAO() {
		if (this.bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.bpmDAO;
	}

	private BPMContext getBPMContext() {
		if (this.bpmContext == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.bpmContext;
	}

	private BPMFactory getBPMFactory() {
		if (this.bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.bpmFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.jbpm.business.TaskManagementService#createAccessRights(
	 * java.lang.Long, 
	 * java.lang.Long, 
	 * java.lang.String, 
	 * boolean, 
	 * boolean
	 * )
	 */
	@RemoteMethod
	@Override
	public boolean createAccessRights(
			Long piId, 
			Long tiId, 
			String roleName,
			boolean handler, 
			boolean writeRights) {
		if (piId == null || tiId == null || StringUtil.isEmpty(roleName)) {
			getLogger().warning("Invalid parameters");
			return false;
		}

		List<Access> accesses = null;
		try {
			Role role = new Role(roleName);
			role.setForTaskInstance(true);
			role.setProcessInstanceId(piId);
			role.setRoleName(roleName);
			accesses = new ArrayList<Access>();
			accesses.add(Access.read);
			if (handler) {
				accesses.add(Access.caseHandler);
				accesses.add(Access.contactsCanBeSeen);
				accesses.add(Access.seeAttachments);
			}
			if (writeRights) {
				accesses.add(Access.write);
			}
			role.setAccesses(accesses);
			getBPMFactory().getRolesManager().setTaskRolePermissionsTIScope(role, tiId, true, null);
			return true;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, 
					"Error creating rights for proc. inst. ID " + piId + 
					", task inst. ID " + tiId + ", role: " + roleName +
					", handler: " + handler + ", write rights: " + writeRights + 
					", accesses: " + accesses, e);
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.jboss.jbpm.business.TaskManagementService#createTaskInstance(
	 * java.lang.Long, 
	 * java.lang.Long, 
	 * java.lang.String, 
	 * java.lang.String, 
	 * java.lang.String
	 * )
	 */
	@RemoteMethod
	@Override
	public boolean createTaskInstance(
			final Long piId, 
			final Long taskId, 
			final String tokenName,
			final String nodeName, 
			final String roleName) {
		if (piId == null) {
			getLogger().warning("Proc. inst. ID is not provided");
			return false;
		}

		if (taskId == null) {
			getLogger().warning("Task ID is not provided");
			return false;
		}

//		if (StringUtil.isEmpty(tokenName)) {
//			getLogger().warning("Token name is not provided");
//			return false;
//		}

		if (StringUtil.isEmpty(nodeName)) {
			getLogger().warning("Node name is not provided");
			return false;
		}

		if (StringUtil.isEmpty(roleName)) {
			getLogger().warning("Role name is not provided");
			return false;
		}

		return getBPMContext().execute(new JbpmCallback<Boolean>() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				try {
					ProcessInstanceW piW = getBPMFactory().getProcessInstanceW(piId);
					org.jbpm.graph.exe.ProcessInstance pi = piW.getProcessInstance(context);
					org.jbpm.taskmgmt.def.Task task = getBPMDAO().find(org.jbpm.taskmgmt.def.Task.class, taskId);
					org.jbpm.taskmgmt.exe.TaskInstance ti = pi.getTaskMgmtInstance().createTaskInstance(task);
					if (ti == null || ti.getId() <= 0) {
						getLogger().warning(
								"Failed to create task instance for proc. inst.: " + piId + 
								", task: " + taskId + ", token: " +
								tokenName + " and node name: " + nodeName);
						return false;
					}

					ti.setProcessInstance(pi);

					org.jbpm.graph.exe.Token token = null;
					if (StringUtil.isEmpty(tokenName)) {
//						Finding node
						org.jbpm.graph.def.ProcessDefinition pd = pi.getProcessDefinition();
						pd = DBUtil.getInstance().initializeAndUnproxy(pd);
						org.jbpm.graph.def.Node node = pd.getNode(nodeName);

						token = getBPMDAO().getSingleResultByInlineQuery(
								"from " + org.jbpm.graph.exe.Token.class.getName() + 
								" t where t.processInstance = :pi and t.node = :node",
								org.jbpm.graph.exe.Token.class,
								new com.idega.core.persistence.Param("pi", pi),
								new com.idega.core.persistence.Param("node", node)
						);
					} else {
						token = getBPMDAO().getSingleResultByInlineQuery(
								"from " + org.jbpm.graph.exe.Token.class.getName() + 
								" t where t.processInstance = :pi and t.name = :name",
								org.jbpm.graph.exe.Token.class,
								new com.idega.core.persistence.Param("pi", pi),
								new com.idega.core.persistence.Param("name", tokenName)
						);
					}

					

					if (token == null) {
						//	Finding node
						org.jbpm.graph.def.ProcessDefinition pd = pi.getProcessDefinition();
						pd = DBUtil.getInstance().initializeAndUnproxy(pd);
						org.jbpm.graph.def.Node node = pd.getNode(nodeName);

						//	Creating token
						org.jbpm.graph.exe.Token rootToken = pi.getRootToken();
						token = new org.jbpm.graph.exe.Token(rootToken, tokenName);
						token.setAbleToReactivateParent(true);
						token.setTerminationImplicit(false);
						token.setNode(node);
						token.setProcessInstance(pi);

						context.save(token);
					}

					ti.setToken(token);
					ti.setPriority(task.getPriority());

					context.save(ti);
					context.getSession().flush();

					//	Creating access permission for handler
					return createAccessRights(piId, ti.getId(), roleName, true, true);
				} catch (Exception e) {
					getLogger().log(Level.WARNING, 
							"Unable to create task instance for proc. inst.: " + piId + 
							", task: " + taskId + ", token: " + tokenName + 
							" and node name: " + nodeName, e);
				}

				return false;
			}
		});
	}
}
