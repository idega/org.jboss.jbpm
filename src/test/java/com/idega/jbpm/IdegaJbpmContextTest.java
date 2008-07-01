package com.idega.jbpm;

import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.GenericDao;
import com.idega.core.persistence.Param;
import com.idega.core.test.base.IdegaBaseTransactionalTest;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/07/01 19:39:04 $ by $Author: civilis $
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public final class IdegaJbpmContextTest extends IdegaBaseTransactionalTest {

	@Autowired
	private IdegaJbpmContext bpmContext;
	
	@Test
	public void testLoadTitle() throws Exception {

		GenericDao genericDao = getGenericDao();
		
		JbpmContext jctx = bpmContext.createJbpmContext();
		
		try {
			ProcessInstance pi = jctx.getProcessInstance(1);
			assertNotNull(pi);
			 
		} finally {
			bpmContext.closeAndCommit(jctx);
		}
		
		ManagersTypeProcessDefinitionBind b = new ManagersTypeProcessDefinitionBind();
		b.setManagersType("whatever");
		b.setProcessDefinitionId(new Long(1));
		
		genericDao.persist(b);
		
		List<ManagersTypeProcessDefinitionBind> binds = 
			genericDao.getResultList(ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getByProcessDefinitionId, ManagersTypeProcessDefinitionBind.class,
					new Param(ManagersTypeProcessDefinitionBind.processDefinitionIdParam, new Long(1))
			);
		
		assertNotNull(binds);
		assertFalse(binds.isEmpty());
	}
}