package org.jbpm.instantiation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceModificationProvider;

@Service("jbpmDelegationVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class DelegationModificationProvider implements BPMInstanceModificationProvider<Delegation> {

	@Override
	public Number getVersion(Delegation instance) {
		return null;
	}

	@Override
	public void setId(Delegation instance, Long id) {
		instance.id = id;
	}

}