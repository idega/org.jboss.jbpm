package org.jbpm;

import org.jbpm.configuration.ObjectFactory;
import org.jbpm.svc.Services;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/15 15:45:54 $ by $Author: civilis $
 */
public class _JbpmContextW extends JbpmContext {

	private static final long serialVersionUID = 6304093008294763536L;
	
	public _JbpmContextW(Services services, ObjectFactory objectFactory) {
		super(services, objectFactory);
	}

	/**
	 * Closing  
	
	@Override
	public void close() {

		JbpmConfigurationW jcw = (JbpmConfigurationW)getJbpmConfiguration();
		
		if(jcw.getDoCommitStack().isEmpty() || jcw.getDoCommitStack().pop()) {
			
			super.close();
			getSession().getTransaction().commit();
		}
	}
	 */
}