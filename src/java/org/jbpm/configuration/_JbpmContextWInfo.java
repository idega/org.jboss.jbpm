package org.jbpm.configuration;

import org.jbpm.JbpmConfigurationW;
import org.jbpm._JbpmContextW;
import org.jbpm.svc.Services;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/15 15:45:54 $ by $Author: civilis $
 */
public class _JbpmContextWInfo extends AbstractObjectInfo {

	private static final long serialVersionUID = -8563638853746782314L;
	private final JbpmContextInfo jci;

	public _JbpmContextWInfo(ObjectFactory objectFact) {

		super();
		
		ObjectFactoryImpl ofi = (ObjectFactoryImpl)objectFact;
		
//		delegating calls to original JbpmContextInfo
		jci = (JbpmContextInfo)ofi.namedObjectInfos.get(JbpmConfigurationW.mainJbpmContext);
		this.isSingleton = jci.isSingleton;
		this.name = jci.name;
	}

	/**
	 * Actually using original JbpmContextInfo, but returning JbpmContextW (wrapper around JbpmContext)
	 */
	public synchronized Object createObject(ObjectFactoryImpl objectFactory) {
		
		jci.createObject(objectFactory);
		
		Services services = new Services(jci.serviceFactories, jci.serviceNames,
				jci.saveOperations);
		
		return new _JbpmContextW(services, objectFactory);
	}
}