package org.jbpm;

import java.io.InputStream;
import java.util.Map;
import java.util.Stack;

import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.jbpm.configuration.ObjectFactory;
import org.jbpm.configuration.ObjectFactoryImpl;
import org.jbpm.configuration.ObjectInfo;
import org.jbpm.configuration.ValueInfo;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.util.ClassLoaderUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * When initialized (through constructor method createJbpmConfiguration), this class creates
 * itself and puts to static instances map of jbpmConfiguration. Therefore,
 * when JbpmConfiguration.getInstance method is called, JbpmConfigurationW is returned.
 * We need this, because we can manage, how (and which) jbpmContext is created.
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/16 14:21:35 $ by $Author: civilis $
 */
public class JbpmConfigurationW extends JbpmConfiguration {

	private static final long serialVersionUID = 8274943833387294127L;
	public static final String mainJbpmContext = "idegaMain";
	
	private ThreadLocal<Stack<Boolean>> doCommitStackLocal = new ThreadLocal<Stack<Boolean>>();
	@Autowired private EntityManagerFactory entityManagerFactory;
	
	public JbpmConfigurationW(JbpmConfiguration cfg) {
		
		super(cfg.objectFactory);
		
		this.jbpmContextStacks = cfg.jbpmContextStacks;
		this.jobExecutor = cfg.jobExecutor;
		
		@SuppressWarnings("unchecked")
		Map<String, Object> insts = instances;
		insts.put("jbpm.cfg.xml", this);
	}
	
	public static JbpmConfigurationW createJbpmConfiguration(String pathToConfiguration) {
		
		InputStream jbpmCfgXmlStream = ClassLoaderUtil.getStream(pathToConfiguration);

        ObjectFactory objectFactory = parseObjectFactory(jbpmCfgXmlStream);
        JbpmConfiguration cfg = createJbpmConfiguration(objectFactory);
        
        JbpmConfigurationW cfgw = new JbpmConfigurationW(cfg);
        
        ObjectFactoryImpl objectFactoryImpl = (ObjectFactoryImpl)objectFactory;
        
        ObjectInfo jbpmConfigurationInfo = new ValueInfo("jbpmConfiguration", cfgw);
        objectFactoryImpl.addObjectInfo(jbpmConfigurationInfo);

//        JbpmContextWInfo jcwi = new JbpmContextWInfo(objectFactoryImpl);
//        jcwi.setName(mainJbpmContext);
//        objectFactoryImpl.addObjectInfo(jcwi);
        
        return cfgw;
	}
	
	@Override
	void jbpmContextCreated(JbpmContext jbpmContext) {
		super.jbpmContextCreated(jbpmContext);
		
//		if(!getJobExecutor().isStarted())
//			startJobExecutor();
	}
	
	public Stack<Boolean> getDoCommitStack() {
		
		Stack<Boolean> stack = doCommitStackLocal.get();
		
	    if (stack == null) {
	    	
	      stack = new Stack<Boolean>();
	      doCommitStackLocal.set(stack);
	    }
	    
	    return stack;
	}
	
	@Override
	public JbpmContext createJbpmContext() {
	
		return createJbpmContext(mainJbpmContext);
	}
	
	@Override
	public JbpmContext createJbpmContext(String name) {

		JbpmContext current = getCurrentJbpmContext();
		JbpmContext context;
		
		if(current != null) {
			context = current;
		} else {
			
			context = super.createJbpmContext(name);
			Session hibernateSession = (Session)getEntityManagerFactory().createEntityManager().getDelegate();
			context.setSession(hibernateSession);
		}
		
		Session hibernateSession = context.getSession();
		
		if(hibernateSession.getTransaction().isActive()) {
		
//			System.out.println("_____create context. Thread is = "+Thread.currentThread().getName());
			getDoCommitStack().push(false);
			
		} else {
		
//			System.out.println("_____create context NEW TRANSACTION. Thread is = "+Thread.currentThread().getName());
			hibernateSession.getTransaction().begin();
			getDoCommitStack().clear();
			getDoCommitStack().push(true);
		}
		
		return context;
	}
	
	@Override
	void jbpmContextClosed(JbpmContext jbpmContext) {
		
		if(getDoCommitStack().isEmpty() || getDoCommitStack().pop()) {
			
//			if ("org.hibernate.StaleObjectStateException".equals(e.getCause().getClass().getName())) {
			
//			System.out.println("_____jbpmContextClosed and commiting_____. Thread is = "+Thread.currentThread().getName());
			try {
				jbpmContext.getSession().getTransaction().commit();
				
			} catch (StaleObjectStateException e) {
				throw new JbpmPersistenceException("Exception while commiting jbpmContext transaction", e);

			} finally {
				super.jbpmContextClosed(jbpmContext);
			}
		}
//		} else
//			System.out.println("____jbpmContextClosed. Thread is = "+Thread.currentThread().getName());
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
}