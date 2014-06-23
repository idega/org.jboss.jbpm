package org.jbpm.context.exe;

import org.jbpm.context.exe.variableinstance.NullInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceVersionProvider;

@Service("jbpmNullInstanceVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class NullInstanceVersionProvider implements BPMInstanceVersionProvider<NullInstance> {

	@Override
	public Number getVersion(NullInstance instance) {
		if (instance instanceof VariableInstance) {
			return ((VariableInstance) instance).version;
		}
		return null;
	}

}