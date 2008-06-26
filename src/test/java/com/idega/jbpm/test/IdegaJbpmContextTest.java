package com.idega.jbpm.test;

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
import com.idega.core.test.base.IdegaBaseTest;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/com/idega/jbpm/test/IdegaJbpmContextTest-context.xml"})
@Transactional
public final class IdegaJbpmContextTest extends IdegaBaseTest {

	@Autowired
	private IdegaJbpmContext bpmContext;
	
	@Test
	public void testLoadTitle() throws Exception {

		GenericDao genericDao = getGenericDao();
		System.out.println("bpmContext="+bpmContext);
		
		JbpmContext jctx = bpmContext.createJbpmContext();
		
		try {

			ProcessInstance pi = jctx.getProcessInstance(1);
			System.out.println("pi="+pi);
			
		} finally {
			bpmContext.closeAndCommit(jctx);
		}
		
		///managersTypeProcessDefinitionBind_getByProcessDefinitionId
		
		ManagersTypeProcessDefinitionBind b = new ManagersTypeProcessDefinitionBind();
		b.setManagersType("whatever");
		b.setProcessDefinitionId(new Long(1));
		
		genericDao.persist(b);
		
		List<ManagersTypeProcessDefinitionBind> binds = 
			genericDao.getResultList(ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getByProcessDefinitionId, ManagersTypeProcessDefinitionBind.class,
					new Param(ManagersTypeProcessDefinitionBind.processDefinitionIdParam, new Long(1))
			);
		
		System.out.println("binds="+binds.iterator().next().getManagersType());
		
		assertNotNull(new Object());
	}
}