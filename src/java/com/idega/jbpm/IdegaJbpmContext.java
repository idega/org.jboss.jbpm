package com.idega.jbpm;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/01/06 17:02:03 $ by $Author: civilis $
 */
public class IdegaJbpmContext {
	
	private JbpmConfiguration jbpmConfiguration;
	private EntityManagerFactory entityManagerFactory;

	public JbpmConfiguration getJbpmConfiguration() {
		return jbpmConfiguration;
	}

	public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}
	
	public JbpmContext createJbpmContext() {
		
		JbpmContext context = getJbpmConfiguration().createJbpmContext();
		Session hibernateSession = (Session)getEntityManagerFactory().createEntityManager().getDelegate();
		context.setSession(hibernateSession);
		return context;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
}