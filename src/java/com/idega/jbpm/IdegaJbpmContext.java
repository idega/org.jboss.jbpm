package com.idega.jbpm;

import java.sql.SQLException;

import javax.persistence.EntityManagerFactory;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 * 
 *          Last modified: $Date: 2009/01/07 18:31:22 $ by $Author: civilis $
 */
public class IdegaJbpmContext implements BPMContext, InitializingBean {

	public static final String beanIdentifier = "idegaJbpmContext";
	private EntityManagerFactory entityManagerFactory;

	private HibernateTemplate hibernateTemplate;

	public JbpmConfiguration getJbpmConfiguration() {
		return JbpmConfiguration.getInstance();
	}

	public JbpmContext createJbpmContext() {

		return JbpmConfiguration.getInstance().createJbpmContext();
	}

	public void closeAndCommit(JbpmContext ctx) {

		ctx.close();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(
			EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public void saveProcessEntity(Object entity) {

		JbpmContext current = getJbpmConfiguration().getCurrentJbpmContext();

		if (current != null) {

			current.getSession().save(entity);
		} else
			throw new IllegalStateException(
					"No current JbpmContext resolved. Create JbpmContext around calling this method");
	}

	public <T> T mergeProcessEntity(T entity) {

		JbpmContext ctx = getJbpmConfiguration().getCurrentJbpmContext();

		if (ctx != null) {

			@SuppressWarnings("unchecked")
			T merged = (T) ctx.getSession().merge(entity);
			return merged;

		} else
			throw new IllegalStateException(
					"No current JbpmContext resolved. Create JbpmContext around calling this method");
	}

	/**
	 * Idea taken from jbpm springmodules
	 * 
	 * Execute the action specified by the given action object within a
	 * JbpmSession.
	 * 
	 * @param callback
	 * @return
	 */
	public <T> T execute(final JbpmCallback callback) {

		final JbpmContext context = JbpmConfiguration.getInstance()
				.createJbpmContext();

		try {
			@SuppressWarnings("unchecked")
			T res = (T) hibernateTemplate.execute(new HibernateCallback() {
				/**
				 * @see org.springframework.orm.hibernate3.HibernateCallback#doInHibernate(org.hibernate.Session)
				 */
				public Object doInHibernate(Session session)
						throws HibernateException, SQLException {
					// inject the session in the context

					context.setSession(session);
					return callback.doInJbpm(context);
				}
			});

			return res;
		} catch (JbpmException ex) {
			throw convertJbpmException(ex);
		} finally {
			context.close();
		}
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {

		Session hibernateSession = (Session) getEntityManagerFactory()
				.createEntityManager().getDelegate();

		hibernateTemplate = new HibernateTemplate(hibernateSession
				.getSessionFactory());
	}

	/**
	 * Copied from jbpm springmodules
	 * 
	 * Converts Jbpm RuntimeExceptions into Spring specific ones (if possible).
	 * 
	 * @param ex
	 * @return
	 */
	public RuntimeException convertJbpmException(JbpmException ex) {
		// decode nested exceptions
		if (ex.getCause() instanceof HibernateException) {
			DataAccessException rootCause = SessionFactoryUtils
					.convertHibernateAccessException((HibernateException) ex
							.getCause());
			return rootCause;
		}

		// cannot convert the exception in any meaningful way
		return ex;
	}
}