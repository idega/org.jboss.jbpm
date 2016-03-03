package org.jbpm.taskmgmt.exe;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceModificationProvider;

@Service("jbpmTaskInstanceVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JBPMTaskInstanceVersionProvider implements BPMInstanceModificationProvider<TaskInstance> {

	@Override
	public Number getVersion(TaskInstance taskInstance) {
		return taskInstance == null ? null : taskInstance.version;
	}

	@Override
	public void setId(TaskInstance instance, Long id) {
		instance.id = id;
	}

}