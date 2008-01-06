package com.idega.jbpm.data.dao;

import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.data.ProcessViewByActor;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/01/06 17:02:58 $ by $Author: civilis $
 */
public interface ProcessViewDao extends GenericDao {

	public abstract ProcessViewByActor getProcessViewByViewerType(String viewerType, String viewType, Long processDefinition);
	
	public abstract List<ProcessViewByActor> getByViewType(String viewType, Long processDefinition);
}