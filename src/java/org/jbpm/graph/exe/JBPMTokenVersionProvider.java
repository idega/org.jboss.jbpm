package org.jbpm.graph.exe;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceVersionProvider;

@Service("jbpmTokenVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JBPMTokenVersionProvider implements BPMInstanceVersionProvider<Token> {

	@Override
	public Number getVersion(Token token) {
		return token == null ? null : token.version;
	}

}