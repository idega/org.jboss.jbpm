package com.idega.jbpm.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.business.BPMPointcuts;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2009/02/07 18:20:55 $ by $Author: civilis $
 */

@Service
@Aspect
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class FireEventAspect {

	private static final Logger LOGGER = Logger.getLogger(FireEventAspect.class.getName());
	private static final String eventType = "postStartActivity";

	@Autowired
	private BPMContext idegaJbpmContext;

	/**
	 * fires postStartActivity event, after non start task instance submitted
	 * @param jp
	 */
	@Transactional(readOnly=true)
	@AfterReturning(BPMPointcuts.submitAtTaskInstanceW)
	public void firePostStartEvent(JoinPoint jp) {
		try {
			final Object jpThis = jp.getThis();

			if (jpThis instanceof TaskInstanceW) {
				getIdegaJbpmContext().execute(new JbpmCallback<Void>() {

					@Override
					public Void doInJbpm(JbpmContext context) throws JbpmException {
						TaskInstance taskInstance = context.getTaskInstance(((TaskInstanceW) jpThis).getTaskInstanceId());
						Task startTask = taskInstance.getTaskMgmtInstance().getTaskMgmtDefinition().getStartTask();

						if (!taskInstance.getTask().equals(startTask)) {
							//	Firing event only for task submit for non start task submit
							final Token tkn = taskInstance.getToken();
							fireEvent(tkn, context);
						}
						return null;
					}
				});
			} else {
				throw new IllegalArgumentException("Only objects of " + TaskInstanceW.class.getName() + " supported, got: " +
						jpThis.getClass().getName());
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while firing event", e);
		}
	}

	/**
	 * fires postStartActivity event, when any access permission is changed (e.g. for document, or for contacts)
	 * @param jp
	 */
	@Transactional(readOnly=true)
	@AfterReturning("("+BPMPointcuts.setContactsPermissionAtProcessInstanceW+" || "+BPMPointcuts.setTaskRolePermissionsAtTaskInstanceW+")")
	public void firePostStartEventOnPermissionChange(JoinPoint jp) {
		try {
			final Object jpThis = jp.getThis();

			getIdegaJbpmContext().execute(new JbpmCallback<Void>() {
				@Override
				public Void doInJbpm(JbpmContext context) throws JbpmException {
					Token tkn = null;
					if (jpThis instanceof TaskInstanceW) {
						TaskInstance taskInst = context.getTaskInstance(((TaskInstanceW) jpThis).getTaskInstanceId());
						tkn = taskInst.getToken();
						fireEvent(tkn, context);

					} else if (jpThis instanceof ProcessInstanceW) {
						fireEvent((ProcessInstanceW) jpThis, context);

					} else {
						throw new IllegalArgumentException("Only objects of " + TaskInstanceW.class.getName() + " and " +
								ProcessInstanceW.class.getName() + " supported, got: " + jpThis.getClass().getName());
					}
					return null;
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while firing event", e);
		}
	}

	private Boolean fireEvent(final ProcessInstanceW piw, JbpmContext context) {
		Token tkn = piw.getProcessInstance(context).getRootToken();
		ProcessDefinition pd = tkn.getProcessInstance().getProcessDefinition();
		return fireEvent(tkn, pd, context);
	}

	private Boolean fireEvent(final Token token, JbpmContext context) {
		return fireEvent(token, null, context);
	}

	private Boolean fireEvent(final Token token, final ProcessDefinition procDef, JbpmContext context) {
		ProcessDefinition pd = procDef;
		if (pd == null) {
			pd = token.getProcessInstance().getProcessDefinition();
		}

		ExecutionContext ectx = new ExecutionContext(token);
		pd.fireEvent(eventType, ectx);
		return Boolean.TRUE;
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
}