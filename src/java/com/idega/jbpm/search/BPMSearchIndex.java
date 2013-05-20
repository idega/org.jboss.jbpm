package com.idega.jbpm.search;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.impl.SimpleIndexingProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.data.Variable;

public class BPMSearchIndex {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	public void rebuild() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		try {
			FullTextSession fullTextSession = Search.getFullTextSession((Session) entityManager.getDelegate());

			fullTextSession
				.createIndexer(Variable.class)
				.progressMonitor(new SimpleIndexingProgressMonitor(1))
				.startAndWait();
		} finally {
			entityManager.close();
		}
	}

}