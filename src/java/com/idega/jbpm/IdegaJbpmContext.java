package com.idega.jbpm;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/02/07 13:58:16 $ by $Author: civilis $
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
		
		JbpmConfiguration cfg = getJbpmConfiguration();
		JbpmContext context;
		
		synchronized (cfg) {
		
			context = cfg.createJbpmContext();
		}
		
		Session hibernateSession = (Session)getEntityManagerFactory().createEntityManager().getDelegate();
		context.setSession(hibernateSession);
		hibernateSession.getTransaction().begin();
		
		return context;
	}
	
	public void closeAndCommit(JbpmContext ctx) {
	
		ctx.getSession().getTransaction().commit();
		ctx.close();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
}