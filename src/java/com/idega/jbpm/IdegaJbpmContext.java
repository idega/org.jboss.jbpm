package com.idega.jbpm;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.annotation.Transactional;

import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.dao.BPMEntityEnum;
import com.idega.jbpm.event.BPMContextSavedEvent;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.StringHandler;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

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

		private Map<Serializable, CheckedObject> getInstancesWithoutDelayedIds(SessionImpl session, PersistenceContext persistenceContext) {
			try {
				@SuppressWarnings("unchecked")
				Map<Object, Object> entities = persistenceContext.getEntitiesByKey();
				if (MapUtil.isEmpty(entities)) {
					return null;
				}

				Map<Serializable, CheckedObject> results = new HashMap<>();
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
							Serializable newKey = getRealId(value, session, persistenceContext, entityKey, key);
							if (newKey != null) {
								results.put(newKey, new CheckedObject(id, value));
							}
						}
					}
				}
				return results;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error checking delayed identifiers for every instance. Persistence context: " + persistenceContext, e);
			}
			return null;
		}

		private Serializable getRealId(Object value, SessionImpl session, PersistenceContext persistenceContext, EntityKey entityKey, Object key) {
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

					return newKey;
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error generating new key for " + value + ", old key: " + key, e);
				}
			}

			return null;
		}

		private Map<Serializable, CheckedObject> getCollectionInstancesWithoutDelayedIds(SessionImpl session, PersistenceContext persistenceContext) {
			try {
				@SuppressWarnings("unchecked")
				final Map.Entry<PersistentCollection, CollectionEntry>[] collectionEntries = IdentityMap.concurrentEntries(
						(Map<PersistentCollection, CollectionEntry>) persistenceContext.getCollectionEntries()
				);
				if (ArrayUtil.isEmpty(collectionEntries)) {
					return null;
				}

				Map<Serializable, CheckedObject> results = new HashMap<>();
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

							if (persister == null) {
								EntityEntry entityEntry = persistenceContext.getEntry(owner);
								if (entityEntry != null) {
									EntityKey entityKey = entityEntry.getEntityKey();
									Serializable newKey = getRealId(owner, session, persistenceContext, entityKey, key);
									if (newKey != null) {
										ce.setCurrentKey(newKey);
										ce.afterAction(coll);

										results.put(newKey, new CheckedObject(entityKey, owner));

										persistenceContext.getEntitiesByKey().remove(entityKey);
									}
								}
							} else {
								CollectionType type = persister.getCollectionType();
								Serializable newKey = type.getKeyOfOwner(owner, session);
								ce.setCurrentKey(newKey);
								ce.afterAction(coll);
								results.put(newKey, new CheckedObject(key, owner));
							}
						}

						if (ce.getCurrentKey() instanceof DelayedPostInsertIdentifier) {
							LOGGER.warning("Still delayed (" + key + ") " + coll);
						}
					} else if (key instanceof DelayedPostInsertIdentifier) {
						LOGGER.info("Do not know how to handle delayed identifier (" + key + ") in " + coll);
					}
				}
				return results;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error checking delayed identifiers for collections. Persistence context: " + persistenceContext, e);
			}
			return null;
		}

		Map<Serializable, CheckedObject> results = new HashMap<>();

		private Map<Serializable, CheckedObject> getCheckedDelayedInstances(SessionImpl session, PersistenceContext persistenceContext) {
			Map<Serializable, CheckedObject> changedCollectionsObjects = getCollectionInstancesWithoutDelayedIds(session, persistenceContext);
			doCheckIds(changedCollectionsObjects, session, persistenceContext, false);

			Map<Serializable, CheckedObject> changedObjects = getInstancesWithoutDelayedIds(session, persistenceContext);
			doCheckIds(changedObjects, session, persistenceContext, false);

			if (changedCollectionsObjects != null) {
				results.putAll(changedCollectionsObjects);
			}
			if (changedObjects != null) {
				results.putAll(changedObjects);
			}
			return results;
		}

		private void doCheckIds(Map<Serializable, CheckedObject> objects, SessionImpl session, PersistenceContext persistenceContext, boolean justProvidedObjects) {
			SessionFactoryImplementor factory = session.getFactory();

			if (!MapUtil.isEmpty(objects)) {
				for (Serializable newKey: objects.keySet()) {
					Serializable object = null;
					Serializable oid = null;
					try {
						CheckedObject checkedObject = objects.get(newKey);
						object = (Serializable) checkedObject.object;
						EntityEntry entityEntry = persistenceContext.getEntry(object);
						if (entityEntry == null) {
							continue;
						}

						doCheckId(session, object, entityEntry, factory);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error checking if IDs (new: " + newKey + ", old: " + oid + ") match for " + object, e);
					}
				}
			}

			if (justProvidedObjects) {
				return;
			}

			Map.Entry<Object, EntityEntry>[] entries = persistenceContext.reentrantSafeEntityEntries();
			if (!ArrayUtil.isEmpty(entries)) {
				Serializable object = null;
				Serializable newKey = null, oid = null;
				for (Map.Entry<?, EntityEntry> entry: entries) {
					try {
						doCheckId(session, entry.getKey(), entry.getValue(), factory);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error checking if IDs (new: " + newKey + ", old: " + oid + ") match for " + object, e);
					}
				}
			}
		}

		private void doCheckId(SessionImpl session, Object object, EntityEntry entityEntry, SessionFactoryImplementor factory) {
			if (entityEntry == null) {
				return;
			}

			EntityPersister entityPersister = entityEntry.getPersister();
			if (entityPersister == null) {
				return;
			}

			Serializable newKey = entityEntry.getId();
			if (object instanceof Serializable) {
				Serializable oid = entityPersister.getIdentifier(object, session);
				if (!entityPersister.getIdentifierType().isEqual(newKey, oid, factory)) {
					BPMEntityEnum bpmEntity = BPMEntityEnum.getEnum(entityEntry.getEntityName());
					if (bpmEntity == null) {
						LOGGER.warning("IDs do not match: " + newKey + " old key: " + oid + ", entity entry: " + entityEntry + ", object " + object +
								", can not fix it: BPM entity unknown for " + entityEntry.getEntityName());
						return;
					}

					if (newKey instanceof Long) {
						bpmEntity.setId((Long) newKey, session, (Serializable) object);
					}
				}
			}
		}

		private void doCheckVersion(SessionImpl session, PersistenceContext persistenceContext, boolean expectedLess) {
			Map.Entry<Object, EntityEntry>[] entries = persistenceContext.reentrantSafeEntityEntries();
			if (ArrayUtil.isEmpty(entries)) {
				return;
			}

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
						Object previousVersionObj = entityEntry.getVersion();
						if (previousVersionObj instanceof Number) {
							Integer previousVersion = ((Number) previousVersionObj).intValue();

							boolean updateVersion = versionInDB.intValue() != previousVersion.intValue();
							if (!updateVersion) {
								if (expectedLess) {
									updateVersion = true;
									previousVersion--;
									previousVersion = previousVersion < 0 ? 0 : previousVersion;
									LOGGER.info("Will set version to " + previousVersion + " for " + entity.getClass().getName() + ", ID: " + id);
								}
							}

							if (updateVersion) {
								String updateSQL = bpmEntity.getUpdateQuery(id, previousVersion);
								try {
									SimpleQuerier.executeUpdate(updateSQL, false);
									LOGGER.info("Changed version to " + previousVersion + " for " + bpmEntity.getEntityClass() + ", ID: " + id + ", class: " + bpmEntity.getEntityClass());
								} catch (SQLException e) {}
							}
						}
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error changing version for " + bpmEntity, e);
				}
			}
		}

		@Override
		protected void flushIfNecessary(Session session, boolean existingTransaction) throws HibernateException {
			boolean checkDelayed = doCheckDelayedInstance();
			boolean checkVersion = doCheckVersion();
			if (session instanceof SessionImpl && session.isOpen()) {
				SessionImpl sessionImpl = (SessionImpl) session;
				PersistenceContext persistenceContext = sessionImpl.getPersistenceContext();

				if (checkDelayed) {
					try {
						getCheckedDelayedInstances(sessionImpl, persistenceContext);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error checking delayed instances", e);
					}
				}

				if (checkVersion) {
					try {
						doCheckVersion(sessionImpl, persistenceContext, false);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error checking versions", e);
					}
				}
			}

			doFlush(session, existingTransaction, checkDelayed, IWMainApplication.getDefaultIWMainApplication().getSettings().getInt("bpm_max_checks_for_delayed", 10));
		}

		private void doFlush(Session session, boolean existingTransaction, boolean checkDelayed, int maxAttempts) {
			boolean success = false;
			boolean expectedLess = false;
			int counter = 0;
			Long procInstId = null;
			try {
				Map<Serializable, CheckedObject> checkedObjects = null;
				while (counter < maxAttempts && session.isOpen() && !success) {
					counter++;

					SessionImpl tmp = null;
					try {
						if (session instanceof SessionImpl) {
							tmp = ((SessionImpl) session);
							PersistenceContext persistenceContext = tmp.getPersistenceContext();

							if (checkDelayed) {
								checkedObjects = getCheckedDelayedInstances(tmp, persistenceContext);
							}

							doCheckVersion(tmp, persistenceContext, expectedLess);
						}

						super.flushIfNecessary(session, existingTransaction);
						success = true;

						if (counter > 1 && session instanceof SessionImpl) {
							procInstId = getProcInstId((SessionImpl) session);
						}
					} catch (NullPointerException npe) {
						LOGGER.warning("NullPointerException: error flushing for the " + counter + " time." + (tmp == null ? CoreConstants.EMPTY : " Persistence context: " + tmp.getPersistenceContext()));

						if (!MapUtil.isEmpty(checkedObjects) && session instanceof SessionImpl) {
							SessionImpl sessionImpl = (SessionImpl) session;
							PersistenceContext persistenceContext = sessionImpl.getPersistenceContext();
							for (Serializable key: checkedObjects.keySet()) {
								CheckedObject checkedObject = checkedObjects.get(key);
								if (checkedObject == null) {
									continue;
								}

								Object oldKey = checkedObject.oldKey;
								if (oldKey instanceof EntityKey) {
									EntityKey oldEntityKey = (EntityKey) oldKey;
									if (oldEntityKey.getIdentifier() instanceof DelayedPostInsertIdentifier) {
										@SuppressWarnings("unchecked")
										Map<Object, Object> entitiesByKey = persistenceContext.getEntitiesByKey();
										if (entitiesByKey != null && !entitiesByKey.containsKey(oldEntityKey)) {
											entitiesByKey.put(oldEntityKey, checkedObject.object);
										}
									}
								}
							}

							doCheckIds(results, sessionImpl, persistenceContext, true);
							doCheckVersion(sessionImpl, persistenceContext, expectedLess);
							checkDelayed = false;
						}

						success = false;
						expectedLess = false;
					} catch (ClassCastException cce) {
						LOGGER.warning("ClassCastException: error flushing for the " + counter + " time." + (tmp == null ? CoreConstants.EMPTY : " Persistence context: " + tmp.getPersistenceContext()));

						success = false;
						expectedLess = false;
					} catch (StaleObjectStateException sose) {
						LOGGER.warning("StaleObjectStateException: error flushing for the " + counter + " time." + (tmp == null ? CoreConstants.EMPTY : " Persistence context: " + tmp.getPersistenceContext()));

						doManageActions(tmp instanceof SessionImpl ? tmp : session instanceof SessionImpl ? (SessionImpl) session : null);
						success = false;
						expectedLess = true;
					} catch (HibernateException he) {
						LOGGER.warning("HibernateException: error flushing for the " + counter + " time." + (tmp == null ? CoreConstants.EMPTY : " Persistence context: " + tmp.getPersistenceContext()));

						doManageActions(tmp instanceof SessionImpl ? tmp : session instanceof SessionImpl ? (SessionImpl) session : null);
						checkDelayed = checkDelayed ? checkDelayed : doCheckDelayedInstance();
						success = false;
						expectedLess = false;
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error flushing for the " + counter + " time." + (tmp == null ? CoreConstants.EMPTY : " Persistence context: " + tmp.getPersistenceContext()), e);

						success = false;
						expectedLess = false;
					}
				}
			} finally {
				if (counter > 1 && procInstId != null) {
					final Long tmpProcInstId = procInstId;
					Thread publisher = new Thread(new Runnable() {

						@Override
						public void run() {
							ELUtil.getInstance().publishEvent(new BPMContextSavedEvent(this, tmpProcInstId));
						}

					});
					publisher.start();
				}
			}
		}

		private Long getProcInstId(SessionImpl session) {
			try {
				PersistenceContext context = session.getPersistenceContext();
				Entry<Object, EntityEntry>[] entries = context.reentrantSafeEntityEntries();
				if (ArrayUtil.isEmpty(entries)) {
					LOGGER.warning("There are no entries in persistence context " + context);
					return null;
				}

				for (Entry<Object, EntityEntry> entry: entries) {
					EntityEntry entityEntry = null;
					try {
						entityEntry = entry.getValue();
						Object object = context.getEntity(entityEntry.getEntityKey());
						if (object instanceof TaskInstance) {
							ProcessInstance pi = ((TaskInstance) object).getProcessInstance();
							return pi.getId();
						}
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error getting object for entity entry " + entityEntry, e);
					}
				}

				LOGGER.warning("Did not find " + TaskInstance.class.getName() + " in " + context);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting proc. inst. ID from session " + session, e);
			}
			return null;
		}

		private void doManageActions(SessionImpl session) {
			if (session == null) {
				return;
			}

			ActionQueue actionQueue = session.getActionQueue();
			int oldSize = actionQueue.numberOfCollectionRemovals();
			actionQueue.clearFromFlushNeededCheck(oldSize);
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

	private class CheckedObject {

		private Serializable oldKey;
		private Object object;

		private CheckedObject(Serializable oldKey, Object object) {
			this.oldKey = oldKey;
			this.object = object;
		}

	}

}