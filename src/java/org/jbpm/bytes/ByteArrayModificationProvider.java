package org.jbpm.bytes;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceModificationProvider;

@Service("jbpmByteArrayVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ByteArrayModificationProvider implements BPMInstanceModificationProvider<ByteArray> {

	@Override
	public Number getVersion(ByteArray instance) {
		return null;
	}

	@Override
	public void setId(ByteArray instance, Long id) {
		instance.id = id;
	}

}