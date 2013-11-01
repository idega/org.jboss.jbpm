package org.jbpm.taskmgmt.exe;

import java.util.Map;

import org.hibernate.Session;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.exe.VariableInstanceVerifier;
import org.jbpm.graph.node.StartState;
import org.jbpm.instantiation.Delegation;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.hibernate.HibernateUtil;
import com.idega.jbpm.version.BPMInstanceVersionUpdater;
import com.idega.util.datastructures.map.MapUtil;

@Service("jbpmTaskInstanceVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JBPMTaskInstanceVersionUpdater extends DefaultSpringBean implements BPMInstanceVersionUpdater<TaskInstance> {

	@Override
	public boolean doUpdateInstanceVersion(TaskInstance task, int version) {
		if (task != null && version > 0) {
			int previousVersion = task.version;
			task.version = version;
			getLogger().info("Set version " + version + " for " + task + ", ID: " + task.getId() +
					". Previous version was " + previousVersion);
			return true;
		}
		return false;
	}

	@Override
	public boolean isPossibleToUpdateVersion(Session session, TaskInstance taskInstance) {
		if (taskInstance == null) {
			return false;
		}
		if (taskInstance.id <= 0) {
			return false;
		}

		Task task = taskInstance.getTask();
		if (task == null) {
			return false;
		}
		task = HibernateUtil.initializeAndUnproxy(task);
		if (task.getId() <= 0) {
			return false;
		}
		Delegation delegation = task.getAssignmentDelegation();
		if (delegation != null) {
			delegation = HibernateUtil.initializeAndUnproxy(delegation);
			if (delegation.getId() <= 0) {
				return false;
			}
		}
		StartState startState = task.getStartState();
		if (startState != null) {
			startState = HibernateUtil.initializeAndUnproxy(startState);
			if (startState.getId() <= 0) {
				return false;
			}
		}

		Map<?, ?> vars = taskInstance.getVariableInstances();
		if (!MapUtil.isEmpty(vars)) {
			for (Object o: vars.values()) {
				o = HibernateUtil.initializeAndUnproxy(o);
				if (o instanceof VariableInstance) {
					if (!VariableInstanceVerifier.getInstance().isVariablePersisted(session, (VariableInstance) o)) {
						getLogger().warning("Variable " + o + " is not persisted, can not restore TaskInstance" + taskInstance + ", ID: " + taskInstance.getId());
						return false;
					}
				}
			}
		}

		return true;
	}

	@Override
	public Number getVersion(TaskInstance taskInstance) {
		return taskInstance == null ? null : taskInstance.version;
	}

}