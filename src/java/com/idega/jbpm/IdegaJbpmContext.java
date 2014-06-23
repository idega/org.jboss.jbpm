package com.idega.jbpm;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.internal.SessionImpl;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.annotation.Transactional;

import com.idega.data.SimpleQuerier;
import com.idega.jbpm.data.dao.BPMEntityEnum;
import com.idega.util.ArrayUtil;
import com.idega.util.StringHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.19 $ Last modified: $Date: 2009/04/22 10:48:53 $ by $Author: civilis $
 */
public class IdegaJbpmContext implements BPMContext, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(IdegaJbpmContext.class.getName());

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

		private IdegaHibernateTemplate(SessionFactory sessionFactory) {
			super(sessionFactory);
		}

		private IdegaHibernateTemplate(Session session) {
			this(session.getSessionFactory());
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

		@Override
		protected void flushIfNecessary(Session session, boolean existingTransaction) throws HibernateException {
			if (session instanceof SessionImpl && session.isOpen()) {
				PersistenceContext persistenceContext = ((SessionImpl) session).getPersistenceContext();
				Map.Entry<Object,EntityEntry>[] entries = persistenceContext.reentrantSafeEntityEntries();
				if (!ArrayUtil.isEmpty(entries)) {
					for (Map.Entry<?, EntityEntry> entry: entries) {
						EntityEntry entityEntry = entry.getValue();
						if (entityEntry == null) {
							continue;
						}

						BPMEntityEnum bpmEntity = BPMEntityEnum.getEnum(entityEntry.getEntityName());
						if (bpmEntity == null) {
							continue;
						}

						try {
							String entryId = entityEntry.getId().toString();
							if (!StringHandler.isNumeric(entryId)) {
								continue;
							}

							Long id = Long.valueOf(entityEntry.getId().toString());
							Object entity = session.get(Class.forName(bpmEntity.getEntityClass()), id);
							if (entity == null) {
								continue;
							}

							Number versionInDB = bpmEntity.getVersionFromDatabase(id);
							if (versionInDB instanceof Number) {
								Object previousVersion = entityEntry.getVersion();
								if (previousVersion instanceof Number && versionInDB.intValue() != ((Number) previousVersion).intValue()) {
									String updateSQL = bpmEntity.getUpdateQuery(id, ((Number) previousVersion).intValue());
									try {
										SimpleQuerier.executeUpdate(updateSQL, false);
										LOGGER.info("Changed version to " + previousVersion + " for " + bpmEntity.getEntityClass() + ", ID: " + id + ", class: " + bpmEntity.getEntityClass());
									} catch (SQLException e) {}
								}
							}
						} catch (Exception e) {
							LOGGER.log(Level.WARNING, "Error changing version for " + bpmEntity, e);
						}
					}
				}
			}

			if (session.isOpen()) {
				super.flushIfNecessary(session, existingTransaction);
			}
		}

	}

	@Override
	public <G> G execute(JbpmCallback<G> callback) {
		return execute(callback, null);
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
	public <T> T execute(final JbpmCallback<T> callback, FlushMode flushMode) {
		final JbpmContext context = JbpmConfiguration.getInstance().createJbpmContext();
		try {
			final Session session = entityManagerFactory.createEntityManager().unwrap(Session.class);
			if (flushMode != null)
				session.setFlushMode(flushMode);

			HibernateTemplate hibernateTemplate = new IdegaHibernateTemplate(session);
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
			LOGGER.log(Level.WARNING, "Error executing callback: " + callback, ex);
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