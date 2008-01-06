package com.idega.jbpm.data.dao.impl;

import java.util.List;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.jbpm.data.ProcessViewByActor;
import com.idega.jbpm.data.dao.ProcessViewDao;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/01/06 17:02:58 $ by $Author: civilis $
 */
public class ProcessViewDaoImpl extends GenericDaoImpl implements ProcessViewDao {

	public ProcessViewByActor getProcessViewByViewerType(String viewerType, String viewType, Long processDefinition) {
		
		return (ProcessViewByActor)getEntityManager().createNamedQuery(ProcessViewByActor.GET_BY_VIEWER_TYPE_QUERY_NAME)
		.setParameter(ProcessViewByActor.viewerTypeParam, viewerType)
		.setParameter(ProcessViewByActor.viewTypeParam, viewType)
		.setParameter(ProcessViewByActor.processDefinitionIdParam, processDefinition)
		.getSingleResult();
	}
	
	public List<ProcessViewByActor> getByViewType(String viewType, Long processDefinition) {
		
		@SuppressWarnings("unchecked")
		List<ProcessViewByActor> views = getEntityManager().createNamedQuery(ProcessViewByActor.GET_BY_VIEW_TYPE_QUERY_NAME)
		.setParameter(ProcessViewByActor.viewTypeParam, viewType)
		.setParameter(ProcessViewByActor.processDefinitionIdParam, processDefinition)
		.getResultList();

		return views;
	}
}