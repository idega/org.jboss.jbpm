package com.idega.jbpm.search;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.business.DefaultSpringBean;
import com.idega.data.SimpleQuerier;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.Variable;
import com.idega.jbpm.data.VariableBytes;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.DBUtil;
import com.idega.util.ListUtil;

public class BPMSearchIndex extends DefaultSpringBean {

	@Autowired
	private BPMContext bpmContext;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	public void rebuild() throws Exception {
		doRebuildIndexes(new Class<?>[] {Variable.class, VariableBytes.class/*, VariableByteArray.class*/});
	}

	private void doRebuildIndexes(Class<?>... classesOfObjectsToIndex) throws Exception {
		if (ArrayUtil.isEmpty(classesOfObjectsToIndex)) {
			return;
		}

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		try {
			FullTextSession indexer = Search.getFullTextSession((Session) entityManager.getDelegate());

			for (Class<?> theClass: classesOfObjectsToIndex) {
				indexer
					.createIndexer(theClass)
					.progressMonitor(new SimpleIndexingProgressMonitor(1))
					.startAndWait();
			}
		} finally {
			entityManager.close();
		}
	}

	public void doIndexVariable(Session session, Serializable id) {
		if (session == null || !(id instanceof Number)) {
			getLogger().warning("Session is invalid (" + session + ") or ID is not type of Number: " + id +
					(id == null ? CoreConstants.EMPTY : ", class: " + id.getClass()));
			return;
		}

		try {
			FullTextSession indexer = org.hibernate.search.Search.getFullTextSession(session);

			Object var = indexer.load(Variable.class, ((Number) id).longValue());
			if (var == null) {
				getLogger().warning("Variable can not be found by ID: " + id);
				return;
			}
			var = DBUtil.getInstance().initializeAndUnproxy(var);
			if (var == null) {
				getLogger().warning("Object became null after initialization and un-proxying. Variable ID: " + id);
				return;
			}

			indexer.index(var);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error indexing variable by ID: " + id, e);
		}
	}

	public void doIndexEachVariable() {
		String query = "select distinct var.name_ from jbpm_variableinstance var";
		List<Serializable[]> names = null;
		try {
			names = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query", e);
		}
		if (ListUtil.isEmpty(names)) {
			return;
		}

		try {
			int total = names.size();
			for (int i = 0; i < names.size(); i++) {
				Serializable[] name = names.get(i);
				if (ArrayUtil.isEmpty(name)) {
					continue;
				}

				Serializable nameCandidate = name[0];
				if (!(nameCandidate instanceof String)) {
					getLogger().warning("Invalid variable name: " + nameCandidate);
					continue;
				}

				final String varName = (String) nameCandidate;
				getLogger().info("Indexing variables by name: " + varName + ". " + (i + 1) + " out of " + total);

				try {
					bpmContext.execute(new JbpmCallback<Boolean>() {

						@Override
						public Boolean doInJbpm(JbpmContext context) throws JbpmException {
							String varsIdsQuery = "select distinct var.id_ from jbpm_variableinstance var where var.name_ = '" + varName + "'";
							List<Serializable[]> ids = null;
							try {
								ids = SimpleQuerier.executeQuery(varsIdsQuery, 1);
							} catch (Exception e) {
								getLogger().log(Level.WARNING, "Error executing query: " + varsIdsQuery, e);
							}
							if (ListUtil.isEmpty(ids)) {
								getLogger().warning("No IDs found for variables by name: " + varName);
								return false;
							}

							FullTextSession indexer = org.hibernate.search.Search.getFullTextSession(context.getSession());
							Transaction tx = indexer.beginTransaction();
							for (Serializable[] varId: ids) {
								if (ArrayUtil.isEmpty(varId)) {
									continue;
								}

								Serializable id = varId[0];
								if (!(id instanceof Number)) {
									getLogger().warning("ID is not type of Number: " + id +
											(id == null ? CoreConstants.EMPTY : ", class: " + id.getClass()));
									continue;
								}

								Object var = indexer.load(Variable.class, ((Number) id).longValue());
								if (var == null) {
									getLogger().warning("Variable can not be found by ID: " + id);
									continue;
								}

								indexer.index(var);
							}
							tx.commit();

							return true;
						}

					});
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Failed to index variables by name: " + varName, e);
				}
			}

			doRebuildIndexes(new Class<?>[] {VariableBytes.class});
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Failed to index BPM variables", e);
		}
	}

}