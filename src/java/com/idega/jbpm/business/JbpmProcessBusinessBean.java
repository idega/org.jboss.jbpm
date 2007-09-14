package com.idega.jbpm.business;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

public class JbpmProcessBusinessBean {

	public List<String> getProcessList() {
//		EntityManagerFactory factory = HibernateHelper.createSessionFactory();
		JbpmConfiguration config = JbpmConfiguration.getInstance();
		JbpmContext ctx = config.createJbpmContext();
		SessionFactory factory = ctx.getSessionFactory();
		Session session = factory.openSession();
		
		ProcessDefinition pd = null;
		List kittens = session.createQuery("from org.jbpm.graph.def.ProcessDefinition")
	    .list();
		
		session.close();
		
		return null;
	}

}
