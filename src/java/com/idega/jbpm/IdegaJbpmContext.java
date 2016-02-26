package com.idega.jbpm;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
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
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.dao.BPMEntityEnum;
import com.idega.util.ArrayUtil;
import com.idega.util.StringHandler;
import com.idega.util.datastructures.map.MapUtil;

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

		private void doCheckEveryInstanceIfDelayed(SessionImpl session, PersistenceContext persistenceContext) {
			try {
				@SuppressWarnings("unchecked")
				Map<Object, Object> entities = persistenceContext.getEntitiesByKey();
				if (MapUtil.isEmpty(entities)) {
					return;
				}

				Set<Object> keys = new HashSet<>(entities.keySet());
				for (Object key: keys) {
					Object value = entities.get(key);
					if (value == null) {
						continue;
					}

					if (key instanceof EntityKey) {
						EntityKey entityKey = (EntityKey) key;
						Serializable id = entityKey.getIdentifier();
						if (id instanceof DelayedPostInsertIdentifier) {
							session.saveOrUpdate(value);

							EntityEntry entityEntry = session.getPersistenceContext().getEntry(value);
							if (entityEntry != null) {
								try {
									EntityPersister ep = entityEntry.getPersister();
									Serializable newKey = ep.getIdentifierGenerator().generate(session, value);
									Object[] state = ep.getPropertyValues(value);
									if (newKey == IdentifierGeneratorHelper.POST_INSERT_INDICATOR) {
										newKey = ep.insert(state, value, session);
									} else {
										ep.insert(newKey, state, value, session);
									}
									ep.setIdentifier(value, newKey, session);

									persistenceContext.replaceDelayedEntityIdentityInsertKeys(entityKey, newKey);
								} catch (Exception e) {
									LOGGER.log(Level.WARNING, "Error generating new key for " + value + ", old key: " + key, e);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error checking delayed identifiers for every instance. Persistence context: " + persistenceContext, e);
			}
		}

		private void doCheckCollectionInstancesIfDelayed(SessionImpl session, PersistenceContext persistenceContext) {
			try {
				@SuppressWarnings("unchecked")
				final Map.Entry<PersistentCollection, CollectionEntry>[] collectionEntries = IdentityMap.concurrentEntries(
						(Map<PersistentCollection, CollectionEntry>) persistenceContext.getCollectionEntries()
				);
				if (ArrayUtil.isEmpty(collectionEntries)) {
					return;
				}

				SessionFactory sessionFactory = session.getSessionFactory();
				for (Map.Entry<PersistentCollection, CollectionEntry> entry: collectionEntries) {
					PersistentCollection coll = entry.getKey();
					CollectionEntry ce = entry.getValue();

					if (ce == null) {
						continue;
					}

					Serializable key = ce.getCurrentKey();
					if (key instanceof DelayedPostInsertIdentifier && coll instanceof AbstractPersistentCollection) {
						AbstractPersistentCollection collection = (AbstractPersistentCollection) coll;
						Object owner = collection.getOwner();
						session.saveOrUpdate(owner);

						if (sessionFactory instanceof SessionFactoryImpl && session instanceof SessionImplementor) {
							CollectionPersister persister = null;
							String role = collection.getRole();
							if (role != null) {
								try {
									SessionFactoryImpl factoryImpl = (SessionFactoryImpl) sessionFactory;
									persister = factoryImpl.getCollectionPersister(role);
								} catch (Exception e) {
									LOGGER.log(Level.WARNING, "Error getting persister for role " + role, e);
								}
							}

							if (persister != null) {
								CollectionType type = persister.getCollectionType();
								Serializable newKey = type.getKeyOfOwner(owner, session);
								ce.setCurrentKey(newKey);
								ce.afterAction(coll);
							}
						}

						if (ce.getCurrentKey() instanceof DelayedPostInsertIdentifier) {
							LOGGER.warning("Still delayed (" + key + ") " + coll);
						}
					} else if (key instanceof DelayedPostInsertIdentifier) {
						LOGGER.info("Do not know how to handle delayed identifier (" + key + ") in " + coll);
					}
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error checking delayed identifiers for collections. Persistence context: " + persistenceContext, e);
			}
		}

		private void doCheckDelayedInstances(SessionImpl session, PersistenceContext persistenceContext) {
			doCheckCollectionInstancesIfDelayed(session, persistenceContext);
			doCheckEveryInstanceIfDelayed(session, persistenceContext);
		}

		@Override
		protected void flushIfNecessary(Session session, boolean existingTransaction) throws HibernateException {
			boolean checkDelayed = doCheckDelayedInstance();
			if (session instanceof SessionImpl && session.isOpen()) {
				PersistenceContext persistenceContext = ((SessionImpl) session).getPersistenceContext();

				if (checkDelayed) {
					try {
						doCheckDelayedInstances((SessionImpl) session, persistenceContext);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error checking delayed instances", e);
					}
				}

				if (doCheckVersion()) {
					Map.Entry<Object, EntityEntry>[] entries = persistenceContext.reentrantSafeEntityEntries();
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

								boolean checkVersion = true;
								boolean readOnly = session.isReadOnly(entity);
								if (readOnly) {
									checkVersion = doCheckVersionForReadOnly();
								}
								if (!checkVersion) {
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
			}

			if (session.isOpen()) {
				if (checkDelayed && session instanceof SessionImpl) {
					SessionImpl tmp = ((SessionImpl) session);
					PersistenceContext persistenceContext = tmp.getPersistenceContext();
					doCheckDelayedInstances(tmp, persistenceContext);
				}

				try {
					super.flushIfNecessary(session, existingTransaction);
				} catch (Exception e) {
					if (checkDelayed && session instanceof SessionImpl) {
						SessionImpl tmp = ((SessionImpl) session);
						PersistenceContext persistenceContext = tmp.getPersistenceContext();
						doCheckDelayedInstances(tmp, persistenceContext);

						try {
							super.flushIfNecessary(session, existingTransaction);
						} catch (NullPointerException npe) {}
					}
				}
			}
		}

		private Boolean versionCheckEnabled = null;
		private Boolean doCheckVersion() {
			if (versionCheckEnabled == null) {
				versionCheckEnabled = IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm_check_entity_version", true);
			}
			return versionCheckEnabled;
		}

		private Boolean delayedInstanceCheckEnabled = null;
		private Boolean doCheckDelayedInstance() {
			if (delayedInstanceCheckEnabled == null) {
				delayedInstanceCheckEnabled = IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm_check_delayed_instance", true);
			}
			return delayedInstanceCheckEnabled;
		}

		private Boolean checkVersion = null;
		private Boolean doCheckVersionForReadOnly() {
			if (checkVersion == null) {
				checkVersion = IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm_check_version_for_read", true);
			}
			return checkVersion;
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