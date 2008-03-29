package com.idega.jbpm;

import java.util.Stack;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/03/29 20:28:24 $ by $Author: civilis $
 */
public class IdegaJbpmContext {
	
	public static final String beanIdentifier = "idegaJbpmContext";
	private static final String mainJbpmContext = "idegaMain";
	private JbpmConfiguration jbpmConfiguration;
	private EntityManagerFactory entityManagerFactory;
	private ThreadLocal<Stack<Boolean>> doCommitStackLocal = new ThreadLocal<Stack<Boolean>>();

	public JbpmConfiguration getJbpmConfiguration() {
		return jbpmConfiguration;
	}

	public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}
	
	protected Stack<Boolean> getDoCommitStack() {
		
		Stack<Boolean> stack = (Stack<Boolean>)doCommitStackLocal.get();
		
	    if (stack == null) {
	    	
	      stack = new Stack<Boolean>();
	      doCommitStackLocal.set(stack);
	    }
	    
	    return stack;
	}
	
	public JbpmContext createJbpmContext() {
		
		JbpmConfiguration cfg = getJbpmConfiguration();
		
		JbpmContext current = cfg.getCurrentJbpmContext();
		JbpmContext context;
		
		if(current != null) {
			context = current;
		} else {
			context = cfg.createJbpmContext(mainJbpmContext);
			Session hibernateSession = (Session)getEntityManagerFactory().createEntityManager().getDelegate();
			context.setSession(hibernateSession);
		}
		
		Session hibernateSession = context.getSession();
		
		if(hibernateSession.getTransaction().isActive()) {
			
			getDoCommitStack().push(false);
			
		} else {
		
			hibernateSession.getTransaction().begin();
			getDoCommitStack().clear();
			getDoCommitStack().push(true);
		}
		
		return context;
	}
	
	public void closeAndCommit(JbpmContext ctx) {
	
//		empty if there were failure only
		if(getDoCommitStack().isEmpty() || getDoCommitStack().pop()) {
			
			ctx.close();
			ctx.getSession().getTransaction().commit();
		}
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
}