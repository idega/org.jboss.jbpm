package com.idega.jbpm;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.annotation.Transactional;

import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.19 $ Last modified: $Date: 2009/04/22 10:48:53 $ by $Author: civilis $
 */
public class IdegaJbpmContext implements BPMContext, InitializingBean {

	public static final String beanIdentifier = "idegaJbpmContext";

	private EntityManagerFactory entityManagerFactory;

	@Override
	public JbpmConfiguration getJbpmConfiguration() {
		return JbpmConfiguration.getInstance();
	}

	@Override
	public JbpmContext createJbpmContext() {
		return JbpmConfiguration.getInstance().createJbpmContext();
	}

	@Override
	public void closeAndCommit(JbpmContext ctx) {
		if (ctx == null)
			return;

		Session session = ctx.getSession();
		ctx.close();
		if (session != null && session.isConnected())
			session.disconnect();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public <T> void saveProcessEntity(T entity) {
		JbpmContext current = getJbpmConfiguration().getCurrentJbpmContext();
		saveProcessEntity(current, entity);
	}

	@Override
	@Transactional(readOnly = false)
	public <T> void saveProcessEntity(JbpmContext context, T entity) {
		if (context == null)
			throw new IllegalStateException("No current JbpmContext resolved. Create JbpmContext around calling this method");

		context.getSession().save(entity);
	}

	@Override
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

	private class IdegaHibernateTemplate extends HibernateTemplate {

		private Session session;

		private boolean startingProcess = false;

		private IdegaHibernateTemplate(SessionFactory sessionFactory, boolean startingProcess) {
			super(sessionFactory);

			this.startingProcess = startingProcess;
		}

		private IdegaHibernateTemplate(Session session, boolean startingProcess) {
			this(session.getSessionFactory(), startingProcess);
			this.session = session;
		}

		@Override
		protected Session getSession() {
			return this.session;
		}

		@Override
		public boolean isExposeNativeSession() {
			return true;
		}

		@Autowired
		private BPMDAO bpmDAO;

		private BPMDAO getBPMDAO() {
			if (bpmDAO == null)
				ELUtil.getInstance().autowire(this);
			return bpmDAO;
		}

		@Override
		protected void flushIfNecessary(Session session, boolean existingTransaction) throws HibernateException {
			if (!session.isOpen()) {
				Logger.getLogger(getClass().getName()).warning("Session is closed");
				return;
			}

			boolean canRestore = true;
			if (startingProcess &&
					IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.restore_vrs_on_start_only", Boolean.FALSE)) {
				canRestore = false;
			}
			if (canRestore) {
				Logger.getLogger(getClass().getName()).info("Will restore versions for BPM enities (if found)");
				getBPMDAO().doRestoreVersion(session);
			} else {
				Logger.getLogger(getClass().getName()).info("Version restoring for BPM enities is turned off");
			}

			super.flushIfNecessary(session, existingTransaction);
		}
	}

	@Override
	public <T> T execute(final JbpmCallback<T> callback) {
		return execute(callback, null, false);
	}

	@Override
	public <T> T execute(final JbpmCallback<T> callback, boolean startingProcess) {
		return execute(callback, null, startingProcess);
	}

	@Override
	public <T> T execute(final JbpmCallback<T> callback, FlushMode flushMode) {
		return execute(callback, flushMode, false);
	}

	/**
	 * Idea taken from jbpm springmodules Execute the action specified by the given action object
	 * within a JbpmSession.
	 *
	 * @param callback
	 * @return
	 */
	@Override
	@Transactional
	public <T> T execute(final JbpmCallback<T> callback, FlushMode flushMode, boolean startingProcess) {
		final JbpmContext context = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			final Session session = entityManagerFactory.createEntityManager().unwrap(Session.class);
			if (flushMode != null)
				session.setFlushMode(flushMode);

			HibernateTemplate hibernateTemplate = new IdegaHibernateTemplate(session, startingProcess);
			return hibernateTemplate.execute(new HibernateCallback<T>() {

				/**
				 * @see org.springframework.orm.hibernate3.HibernateCallback#doInHibernate(org.hibernate.Session)
				 */
				@Override
				public T doInHibernate(Session session) throws HibernateException, SQLException {
					context.setSession(session);
					return callback.doInJbpm(context);
				}
			});
		} catch (JbpmException ex) {
			throw convertJbpmException(ex);
		} catch (Throwable ex) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error executing callback: " + callback, ex);
		} finally {
			closeAndCommit(context);
		}

		return null;
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
	}

	/**
	 * Copied from jbpm springmodules Converts Jbpm RuntimeExceptions into Spring specific ones (if
	 * possible).
	 *
	 * @param ex
	 * @return
	 */
	public RuntimeException convertJbpmException(JbpmException ex) {
		// decode nested exceptions
		if (ex.getCause() instanceof HibernateException) {
			DataAccessException rootCause = SessionFactoryUtils.convertHibernateAccessException((HibernateException) ex.getCause());
			return rootCause;
		}

		// cannot convert the exception in any meaningful way
		return ex;
	}

}