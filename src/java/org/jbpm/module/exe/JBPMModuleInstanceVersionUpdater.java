package org.jbpm.module.exe;

import java.io.Serializable;

import org.hibernate.Session;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.version.BPMInstanceVersionUpdater;

public abstract class JBPMModuleInstanceVersionUpdater<I extends Serializable> extends DefaultSpringBean implements BPMInstanceVersionUpdater<I> {

	@Override
	public Number getVersion(I instance) {
		if (instance instanceof TaskMgmtInstance) {
			ModuleInstance moduleInstance = (ModuleInstance) instance;
			return moduleInstance.version;
		}
		return null;
	}

	@Override
	public boolean doUpdateInstanceVersion(I instance, int version) {
		if (instance instanceof TaskMgmtInstance) {
			ModuleInstance moduleInstance = (ModuleInstance) instance;
			moduleInstance.version = version;
			return true;
		}
		return false;
	}

	@Override
	public boolean isPossibleToUpdateVersion(Session session, I instance) {
//		if (instance instanceof TaskMgmtInstance) {
//			TaskMgmtInstance taskMgmtInstance = (TaskMgmtInstance) instance;
//			Collection<TaskInstance> taskInstances = taskMgmtInstance.getTaskInstances();
//			if (!ListUtil.isEmpty(taskInstances)) {
//				for (TaskInstance taskInstance: taskInstances) {
//					Map<?, ?> vars = taskInstance.getVariableInstances();
//					if (!MapUtil.isEmpty(vars)) {
//						for (Object o: vars.values()) {
//							o = HibernateUtil.initializeAndUnproxy(o);
//							if (o instanceof VariableInstance) {
//								if (!VariableInstanceVerifier.getInstance().isVariablePersisted(session, (VariableInstance) o)) {
//									getLogger().warning("Variable " + o + " is not persisted, can not restore TaskInstance" + taskInstance + ", ID: " + taskInstance.getId());
//									return false;
//								}
//							}
//						}
//					}
//
//					session.save(taskInstance);
//				}
//			}
//		}
//		return true;
		return false;
	}

}