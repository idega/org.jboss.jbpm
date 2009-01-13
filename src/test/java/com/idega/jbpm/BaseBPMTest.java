package com.idega.jbpm;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.test.base.IdegaBaseTransactionalTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class BaseBPMTest extends IdegaBaseTransactionalTest {

	@Autowired
	private BPMContext bpmContext;

	@Test
	public void testDummy() throws Exception {
	}

	protected void deployProcessDefinition(
			final ProcessDefinition processDefinition) {

		getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				context.deployProcessDefinition(processDefinition);
				return null;
			}

		});
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}
}
