package org.jbpm.context.exe;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceVersionProvider;

@Service("jbpmTokenVariableMapVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class TokenVariableMapVersionProvider implements BPMInstanceVersionProvider<TokenVariableMap> {

	@Override
	public Number getVersion(TokenVariableMap instance) {
		return instance == null ? null : instance.version;
	}

}