/**
 * @(#)JBPMTaskCreatorBean.java    1.0.0 10:15:30
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
 *     �Licensee� shall also mean the individual using or installing
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
package com.idega.jbpm.presentation.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.faces.event.ValueChangeEvent;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.data.Case;
import com.idega.data.SimpleQuerier;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.business.TaskManagementService;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.util.CoreUtil;
import com.idega.util.DBUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * <p>JSF managed bean for fetching info about {@link ProcessInstance}s
 * of {@link Case}. THIS WAS MADE AS QUICK, NOT OPTIMAL SOLUTION</p>
 * <p>You can report about problems to:
 * <a href="mailto:martynas@idega.is">Martynas Stakė</a></p>
 *
 * @version 1.0.0 2015 rugs. 22
 * @author <a href="mailto:martynas@idega.is">Martynas Stakė</a>
 */
public class JBPMTaskCreatorBean {

	@Autowired
	private TaskManagementService taskManagementService;

	@Autowired
	private BPMFactory bpmFactory;

	private String caseIdentifier;

	private Long taskId;

	private String roleName;

	@Autowired
	private BPMContext bpmContext;

	private BPMContext getBpmContext() {
		if (this.bpmContext == null) {
			ELUtil.getInstance().autowire(this);
		}

		return bpmContext;
	}

	private BPMFactory getBPMFactory() {
		if (this.bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.bpmFactory;
	}

	private TaskManagementService getTaskManagementService() {
		if (this.taskManagementService == null) {
			ELUtil.getInstance().autowire(this);
		}

		return this.taskManagementService;
	}

	public String getCaseIdentifier() {
		return caseIdentifier;
	}

	public void setCaseIdentifier(String caseIdentifier) {
		this.caseIdentifier = caseIdentifier;
	}

	public void caseIdentifierChange(ValueChangeEvent event) {
		Object value = event.getNewValue();
 		if (value != null) {
			this.caseIdentifier = value.toString();
			this.taskId = null;
			this.roleName = null;
 		}
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/*
	 * Process instances
	 */

	public Integer getProcessInstanceId() {
		if (!StringUtil.isEmpty(getCaseIdentifier())) {
			StringBuilder query = new StringBuilder();
			query.append("SELECT bcp.process_instance_id ");
			query.append("FROM bpm_cases_processinstances bcp ");
			query.append("WHERE bcp.case_identifier = '");
			query.append(getCaseIdentifier());
			query.append("'");

			try {
				return SimpleQuerier.executeIntQuery(query.toString());
			} catch (Exception e) {
				java.util.logging.Logger.getLogger(getClass().getName()).log(
						Level.WARNING,
						"Failed to get process instance id by query: '" + query +
						" cause of: ", e);
			}
		}

		return -1;
	}

	private ProcessInstanceW getProcessInstance() {
		Integer piid = getProcessInstanceId();
		if (piid != null && piid > 0) {
			try {
				return getBPMFactory().getProcessInstanceW(Long.valueOf(piid));
			} catch (Exception e) {}
		}

		return null;
	}

	private ProcessInstanceW getSubProcessInstance() {
		final ProcessInstanceW piw = getProcessInstance();
		if (piw != null) {
			Long subProcessInstanceId = getBpmContext().execute(new JbpmCallback<Long>() {

				@Override
				public Long doInJbpm(JbpmContext context) throws JbpmException {
					ProcessInstance pi = context.getProcessInstance(piw.getProcessInstanceId());
					Token rootToken = DBUtil.getInstance().initializeAndUnproxy(pi.getRootToken());
					ProcessInstance subProcessInstance = DBUtil.getInstance()
							.initializeAndUnproxy(rootToken.getSubProcessInstance());
					return subProcessInstance.getId();
				}
			});

			return getBPMFactory().getProcessInstanceW(subProcessInstanceId);
		}

		return null;
	}

	/*
	 * Process definitions
	 */

	private ProcessDefinitionW getProcessDefinition() {
		ProcessInstanceW pi = getProcessInstance();
		if (pi != null) {
			return pi.getProcessDefinitionW();
		}

		return null;
	}

	private ProcessDefinitionW getSubProcessDefinition() {
		ProcessInstanceW spi = getSubProcessInstance();
		if (spi != null) {
			return spi.getProcessDefinitionW();
		}

		return null;
	}

	/*
	 * Tasks
	 */

	private Collection<Task> getSubProcessTasks() {
		final ArrayList<Task> result = new ArrayList<>();

		final ProcessDefinitionW spdw = getSubProcessDefinition();
		if (spdw != null) {
			getBpmContext().execute(new JbpmCallback<Long>() {

				@Override
				public Long doInJbpm(JbpmContext context) throws JbpmException {
					ProcessDefinition pd = context.getGraphSession().getProcessDefinition(spdw.getProcessDefinitionId());
					TaskMgmtDefinition taskDef = pd.getTaskMgmtDefinition();
					@SuppressWarnings("unchecked")
					Map<String, Task> tasks = taskDef.getTasks();
					for (Task task : tasks.values()) {
						result.add(DBUtil.getInstance().initializeAndUnproxy(task));
					}

					return 0L;
				}
			});
		}

		return result;
	}

	private Collection<TaskInstanceW> getSubProcessTaskInstances() {
		ArrayList<TaskInstanceW> result = new ArrayList<>();

		ProcessInstanceW spiw = getSubProcessInstance();
		if (spiw != null) {
			return spiw.getAllTaskInstances();
		}

		return result;
	}

	Collection<Long> getSubmittedSubProcessTasks() {
		ArrayList<Long> submittedTasks = new ArrayList<>();
		Collection<TaskInstanceW> sptis = getSubProcessTaskInstances();

		for (TaskInstanceW spti : sptis) {
			submittedTasks.add(spti.getTaskInstanceId());
		}

		return submittedTasks;
	}

	private Collection<Task> getUnsubmittedSubProcessTasks() {
		Collection<Task> spt = getSubProcessTasks();
//		Collection<Long> sspt = getSubmittedSubProcessTasks();
//		spt.removeAll(sspt);

		return spt;
	}

	/*
	 * Processed public stuff
	 */

	public Long getSubProcessInstanceId() {
		ProcessInstanceW spi = getSubProcessInstance();
		if (spi != null) {
			return spi.getProcessInstanceId();
		}

		return null;
	}

	public String getProcessDefinitionName() {
		ProcessDefinitionW pd = getProcessDefinition();
		if (pd != null) {
			return pd.getProcessName(CoreUtil.getCurrentLocale());
		}

		return null;
	}

	public String getSubProcessDefinitionName() {
		ProcessDefinitionW spd = getSubProcessDefinition();
		if (spd != null) {
			return spd.getProcessName(CoreUtil.getCurrentLocale());
		}

		return null;
	}

	public Map<String, Long> getUnsubmittedSubProcessTasksMap() {
		Map<String, Long> map = new TreeMap<>();

		Collection<Task> unsubmittedTasks = getUnsubmittedSubProcessTasks();
		for (Task unsubmittedTask : unsubmittedTasks) {
			map.put(unsubmittedTask.getName(), unsubmittedTask.getId());
		}

		return map;
	}

	public void submit() {
		StringBuilder query = new StringBuilder();
		query.append("SELECT jn.name_ ");
		query.append("FROM jbpm_node jn ");
		query.append("JOIN jbpm_task jt ");
		query.append("ON jn.id_ = jt.tasknode_ ");
		query.append("AND jt.id_ = ").append(getTaskId());

		String[] nodeNames = null;
		try {
			nodeNames = SimpleQuerier.executeStringQuery(query.toString());
		} catch (Exception e) {
			java.util.logging.Logger.getLogger(getClass().getName()).log(Level.WARNING,
					"Failed to get jbpm_node name by query: '" + query.toString() +
					"' cause of: ", e);
		}

		getTaskManagementService().createTaskInstance(getSubProcessInstanceId(), getTaskId(), null, nodeNames[0], getRoleName());
	}
}
