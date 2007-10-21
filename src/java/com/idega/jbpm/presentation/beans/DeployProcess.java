package com.idega.jbpm.presentation.beans;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2007/10/21 21:19:20 $ by $Author: civilis $
 *
 */
public class DeployProcess {
	
	private UploadedFile pd;
	private SessionFactory sessionFactory;
	
	public UploadedFile getProcessDefinition() {
		return pd;
	}
	
	public void setProcessDefinition(UploadedFile pd) {
		this.pd = pd;
	}
	
	public void upload() {
		
		Session session = getSessionFactory().getCurrentSession();
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(session);
		
		InputStream is = null;
		
		try {
			is = getProcessDefinition().getInputStream();
			ctx.deployProcessDefinition(ProcessDefinition.parseXmlInputStream(is));

		} catch (IOException e) {
			
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while reading process while getting process definition input stream", e);
//			TODO: display err msg
			
		} catch (Exception e) {
			
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while deploying process definition", e);
//			TODO: display err msg				
			
		} finally {
			if(ctx != null)
				ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
			
//			TODO: close is?
		}
	}
	
	private JbpmConfiguration cfg;
	
	public void setJbpmConfiguration(JbpmConfiguration cfg) {
		this.cfg = cfg;
	}
	
	public JbpmConfiguration getJbpmConfiguration() {
		return cfg;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}