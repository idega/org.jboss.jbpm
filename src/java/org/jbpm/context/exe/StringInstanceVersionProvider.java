package org.jbpm.context.exe;

import org.jbpm.context.exe.variableinstance.StringInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceModificationProvider;

@Service("jbpmStringInstanceVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class StringInstanceVersionProvider implements BPMInstanceModificationProvider<StringInstance> {

	@Override
	public Number getVersion(StringInstance instance) {
		if (instance instanceof VariableInstance) {
			return ((VariableInstance) instance).version;
		}
		return null;
	}

	@Override
	public void setId(StringInstance instance, Long id) {
		if (instance instanceof VariableInstance) {
			((VariableInstance) instance).id = id;
		}
	}

}