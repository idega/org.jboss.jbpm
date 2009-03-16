package com.idega.jbpm;

import java.sql.SQLException;

import javax.persistence.EntityManagerFactory;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.idega.idegaweb.IWMainApplication;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.18 $
 * 
 *          Last modified: $Date: 2009/03/16 16:33:27 $ by $Author: civilis $
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
	@Transactional // temporal let's hope. need to understand and merge jbpm + jpa transactions behavior
	public <T> T execute(final JbpmCallback callback) {

		final JbpmContext context = JbpmConfiguration.getInstance()
				.createJbpmContext();

		try {
			
			String useNewTransactionHandlingProp = IWMainApplication
			.getDefaultIWMainApplication().getSettings()
			.getProperty("bpm_use_new_transaction_handling",
					"true");
			
			Boolean useNewTransactionHandling = "true".equals(useNewTransactionHandlingProp);
			
			HibernateTemplate hibernateTemplate;
			
			if(useNewTransactionHandling) {
				
				EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(getEntityManagerFactory());
				if (holder != null) {
					Session session = ((HibernateEntityManager) (holder.getEntityManager())).getSession();
					SessionFactory sessionFactory = ((HibernateEntityManagerFactory) getEntityManagerFactory()).getSessionFactory();
					if (!SessionFactoryUtils.isSessionTransactional(session, sessionFactory)) {
						TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory);
						SessionHolder holderToUse = new SessionHolder(session);
						TransactionSynchronizationManager.bindResource(sessionFactory, holderToUse);
					}
					hibernateTemplate = new HibernateTemplate(sessionFactory);
				} else {
					hibernateTemplate = this.hibernateTemplate;
				}
			} else {
				
				hibernateTemplate = this.hibernateTemplate;
			}
			
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