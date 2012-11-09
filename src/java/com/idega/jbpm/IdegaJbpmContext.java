package com.idega.jbpm;

import java.sql.SQLException;


import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

import org.springframework.transaction.annotation.Transactional;


import com.idega.hibernate.SessionFactoryUtil;
import com.idega.idegaweb.IWMainApplication;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.19 $ Last modified: $Date: 2009/04/22 10:48:53 $ by $Author: civilis $
 */
public class IdegaJbpmContext implements BPMContext, InitializingBean {
	
	public static final String beanIdentifier = "idegaJbpmContext";	
	private HibernateTemplate hibernateTemplate;
	
	public JbpmConfiguration getJbpmConfiguration() {
//		return null;
		return JbpmConfiguration.getInstance();
	}
	
	public JbpmContext createJbpmContext() {
//		return null;
		return JbpmConfiguration.getInstance().createJbpmContext();
	}
	
	public void closeAndCommit(JbpmContext ctx) {
		
		ctx.close();
	}
	
	public void saveProcessEntity(Object entity) {
//		return;
		JbpmContext current = getJbpmConfiguration().getCurrentJbpmContext();
		
		if (current != null) {
			
			current.getSession().save(entity);
		} else
			throw new IllegalStateException(
			        "No current JbpmContext resolved. Create JbpmContext around calling this method");
	}
	
	public <T> T mergeProcessEntity(T entity) {
//		return null;
		JbpmContext ctx = getJbpmConfiguration().getCurrentJbpmContext();
		
		if (ctx != null) {
			
			@SuppressWarnings("unchecked")
			T merged = (T) ctx.getSession().merge(entity);
			return merged;
			
		} else
			throw new IllegalStateException(
			        "No current JbpmContext resolved. Create JbpmContext around calling this method");
	}
	
	/**
	 * Idea taken from jbpm springmodules Execute the action specified by the given action object
	 * within a JbpmSession.
	 * 
	 * @param callback
	 * @return
	 */
	@Transactional
	// temporal let's hope. need to understand and merge jbpm + jpa transactions behavior
	public <T> T execute(final JbpmCallback callback) {
//		return null;
		final JbpmContext context = JbpmConfiguration.getInstance()
		        .createJbpmContext();
		
		try {
			
			Boolean useNewTransactionHandling = isUseNewTransactionHandling();
			
			HibernateTemplate hibernateTemplate = null;
			if (SessionFactoryUtil.getSessionFactory() != null) {
				this.hibernateTemplate = new HibernateTemplate(SessionFactoryUtil.getSessionFactory());
			}
			
			if (useNewTransactionHandling) {
				hibernateTemplate = new HibernateTemplate(SessionFactoryUtil.getSessionFactory());
			} else {
				hibernateTemplate = this.hibernateTemplate;
			}
			
			@SuppressWarnings("unchecked")
			T res = (T) hibernateTemplate.execute(new HibernateCallback() {
				/**
				 * @see org.springframework.orm.hibernate3.HibernateCallback#doInHibernate(org.hibernate.Session)
				 */
				public Object doInHibernate(Session session)
				        throws HibernateException, SQLException {
					// inject the session in the context
					
					context.setSession(session);
					return callback.doInJbpm(context);
				}
			});
			
			return res;
		} catch (JbpmException ex) {
			throw convertJbpmException(ex);
		} finally {
			context.close();
		}
	}
	
	private boolean isUseNewTransactionHandling() {
		
		final IWMainApplication iwma = IWMainApplication
		        .getDefaultIWMainApplication();
		
		final Boolean useNewTransactionHandling;
		
		if (iwma != null) {
			
			String useNewTransactionHandlingProp = IWMainApplication
			        .getDefaultIWMainApplication().getSettings().getProperty(
			            "bpm_use_new_transaction_handling", "true");
			
			useNewTransactionHandling = "true"
			        .equals(useNewTransactionHandlingProp);
		} else {
			
			// unit test case
			useNewTransactionHandling = true;
		}
		
		return useNewTransactionHandling;
	}
	
	/**
	 * Copied from jbpm springmodules Converts Jbpm RuntimeExceptions into Spring specific ones (if
	 * possible).
	 * 
	 * @param ex
	 * @return
	 */
	public RuntimeException convertJbpmException(JbpmException ex) {
//		return null;
		
		// decode nested exceptions
		if (ex.getCause() instanceof HibernateException) {
			DataAccessException rootCause = SessionFactoryUtils
			        .convertHibernateAccessException((HibernateException) ex
			                .getCause());
			return rootCause;
		}
		
		// cannot convert the exception in any meaningful way
		return ex;
	}

	@Override
	public void afterPropertiesSet() throws Exception {}
}