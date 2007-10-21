package com.idega.jbpm.business;

import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2007/10/21 21:19:20 $ by $Author: civilis $
 */
public class ProcessManager {

	private JbpmConfiguration jbpmConfiguration;
	private SessionFactory sessionFactory;
	
	public void submitVariables(Map<String, Object> variables, long taskInstanceId) {

		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			if(variables == null || variables.isEmpty())
				return;

			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = ti.getTask().getTaskController().getVariableAccesses();
			
			for (VariableAccess variableAccess : variableAccesses)
				if(!variableAccess.isWritable() && variables.containsKey(variableAccess.getVariableName()))
					variables.remove(variableAccess.getVariableName());

			ti.setVariables(variables);
			ti.getTask().getTaskController().submitParameters(ti);
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public Map<String, Object> populateVariables(long taskInstanceId) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			@SuppressWarnings("unchecked")
			Map<String, Object> variables = (Map<String, Object>)ti.getVariables();
			
			return variables;
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}

	public JbpmConfiguration getJbpmConfiguration() {
		return jbpmConfiguration;
	}

	public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}