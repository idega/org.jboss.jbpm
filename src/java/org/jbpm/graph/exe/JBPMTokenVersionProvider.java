package org.jbpm.graph.exe;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceModificationProvider;

@Service("jbpmTokenVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JBPMTokenVersionProvider implements BPMInstanceModificationProvider<Token> {

	@Override
	public Number getVersion(Token token) {
		return token == null ? null : token.version;
	}

	@Override
	public void setId(Token instance, Long id) {
		instance.id = id;
	}

}