package org.jbpm.context.exe;

import org.jbpm.context.exe.variableinstance.NullInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceModificationProvider;

@Service("jbpmNullInstanceVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class NullInstanceVersionProvider implements BPMInstanceModificationProvider<NullInstance> {

	@Override
	public Number getVersion(NullInstance instance) {
		if (instance instanceof VariableInstance) {
			return ((VariableInstance) instance).version;
		}
		return null;
	}

	@Override
	public void setId(NullInstance instance, Long id) {
	}

}