package org.jbpm.graph.exe;

import java.util.logging.Logger;

import org.hibernate.Session;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceVersionUpdater;

@Service("jbpmTokenVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JBPMTokenVersionUpdater implements BPMInstanceVersionUpdater<Token> {

	@Override
	public boolean doUpdateInstanceVersion(Token token, int version) {
		if (token != null && version > 0) {
			int previousVersion = token.version;
			token.version = version;
			Logger.getLogger(getClass().getName()).info("Set version " + version + " for " + token + ", ID: " + token.getId() +
					". Previous version was " + previousVersion);
			return true;
		}
		return false;
	}

	@Override
	public boolean isPossibleToUpdateVersion(Session session, Token token) {
		if (token == null) {
			return false;
		}
		if (token.id <= 0) {
			return false;
		}

		return true;
	}

	@Override
	public Number getVersion(Token token) {
		return token == null ? null : token.version;
	}

}