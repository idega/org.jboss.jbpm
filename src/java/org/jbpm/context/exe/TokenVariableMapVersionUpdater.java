package org.jbpm.context.exe;

import org.hibernate.Session;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceVersionUpdater;

@Service("jbpmTokenVariableMapVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class TokenVariableMapVersionUpdater implements BPMInstanceVersionUpdater<TokenVariableMap> {

	@Override
	public boolean doUpdateInstanceVersion(TokenVariableMap instance, int version) {
		if (instance != null) {
			instance.version = version;
			return true;
		}
		return false;
	}

	@Override
	public boolean isPossibleToUpdateVersion(Session session, TokenVariableMap instance) {
		if (instance == null || instance.version <= 0) {
			return false;
		}
		return true;
	}

	@Override
	public Number getVersion(TokenVariableMap instance) {
		return instance == null ? null : instance.version;
	}

}