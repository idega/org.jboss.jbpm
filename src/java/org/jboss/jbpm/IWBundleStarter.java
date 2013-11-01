package org.jboss.jbpm;

import java.util.Iterator;

import org.hibernate.ejb.event.EJB3FlushEntityEventListener;
import org.hibernate.ejb.event.EJB3FlushEventListener;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.internal.SessionImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.persistence.IdegaFlushEntityEventListener;
import com.idega.jbpm.persistence.IdegaFlushEventListener;
import com.idega.jbpm.search.BPMSearchIndex;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/05/16 09:47:41 $ by $Author: civilis $
 */
public class IWBundleStarter implements IWBundleStartable {

	public static final String IW_BUNDLE_IDENTIFIER = "org.jboss.jbpm";

	@Autowired
	private BPMDAO bpmDAO;

	@Autowired
	private VariableInstanceQuerier querier;

	@Autowired
	private BPMSearchIndex searchIndex;

	private BPMSearchIndex getSearchIndexForVariable() {
		if (searchIndex == null)
			ELUtil.getInstance().autowire(this);
		return searchIndex;
	}

	@Override
	public void start(IWBundle starterBundle) {
		Object session = getVariableInstanceQuerier().getEntityManager().getDelegate();
		if (session instanceof SessionImpl) {
			EventListenerRegistry eventListenerRegistry = ((SessionImpl) session)
					.getFactory()
					.getServiceRegistry()
					.getService(EventListenerRegistry.class);

			EventListenerGroup<FlushEntityEventListener> flushEntityListenersGroup = eventListenerRegistry
						.getEventListenerGroup(EventType.FLUSH_ENTITY);
			for (Iterator<FlushEntityEventListener> iter = flushEntityListenersGroup.listeners().iterator(); iter.hasNext();) {
				FlushEntityEventListener listener = iter.next();
				if (listener.getClass().getName().equals(EJB3FlushEntityEventListener.class.getName())) {
					iter.remove();
				}
			}
			flushEntityListenersGroup.prependListener(new IdegaFlushEntityEventListener());

			EventListenerGroup<FlushEventListener> flushEventListenersGroup = eventListenerRegistry
					.getEventListenerGroup(EventType.FLUSH);
			for (Iterator<FlushEventListener> iter = flushEventListenersGroup.listeners().iterator(); iter.hasNext();) {
				FlushEventListener listener = iter.next();
				if (listener.getClass().getName().equals(EJB3FlushEventListener.class.getName())) {
					iter.remove();
				}
			}
			flushEventListenersGroup.prependListener(new IdegaFlushEventListener());
		}

		doBuildIndexes(starterBundle.getApplication().getSettings());

		BPMViewManager vm = BPMViewManager.getInstance(starterBundle.getApplication());
		vm.initializeStandardNodes(starterBundle);

		Thread variablesDataImporter = new Thread(new Runnable() {
			@Override
			public void run() {
				getBpmDAO().importVariablesData();
			}
		});
		variablesDataImporter.start();

		Thread procVarsBinder = new Thread(new Runnable() {
			@Override
			public void run() {
				getVariableInstanceQuerier().doBindProcessVariables();
			}
		});
		procVarsBinder.start();
	}

	private void doBuildIndexes(IWMainApplicationSettings settings) {
		try {
			if (settings.getBoolean("bpm.auto_index_vars", Boolean.FALSE)) {
				getSearchIndexForVariable().rebuild();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	VariableInstanceQuerier getVariableInstanceQuerier() {
		if (querier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return querier;
	}

	BPMDAO getBpmDAO() {
		if (bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmDAO;
	}

	@Override
	public void stop(IWBundle starterBundle) { }
}