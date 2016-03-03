package org.jbpm.graph.def;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceModificationProvider;

@Service("jbpmEventVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class EventModificationProvider implements BPMInstanceModificationProvider<Event> {

	@Override
	public Number getVersion(Event instance) {
		return null;
	}

	@Override
	public void setId(Event instance, Long id) {
		instance.id = id;
	}

}