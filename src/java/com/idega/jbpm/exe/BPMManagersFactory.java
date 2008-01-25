package com.idega.jbpm.exe;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.idega.jbpm.exe.impl.BPMManagersFactoryPluggedInEvent;

/**
 * This class is intended to be extended by specific managers (either view or process, for now) creators.
 * Those would be registered to BMPFactory(Impl) and therefore be known to it, when requested.
 * The idea behind this is, that for each process definition, there might be specific behavior managers.
 * E.g. manager for cases (CasesBpmViewManager or CasesBpmProcessManager).
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/01/25 15:24:25 $ by $Author: civilis $
 */
public class BPMManagersFactory implements ApplicationContextAware, ApplicationListener {
	
	private ApplicationContext applicationContext;
	
	/**
	 * Extend&implement this method
	 * @return
	 */
	public ViewManager getViewManager() {
		
		throw new UnsupportedOperationException("Default view manager not supported. Provide specific implementation");
	}
	
	/**
	 * Extend&implement this method
	 * @return
	 */
	public ProcessManager getProcessManager() {
		
		throw new UnsupportedOperationException("Default process manager not supported. Provide specific implementation");
	}
	
	/**
	 * Extend&implement this method
	 * @return type used to bind process definition with managers (e.g. cases)
	 */
	public String getManagersType() {
		
		throw new UnsupportedOperationException("Subclasses are supposed to implement this"); 
	}
	
	/**
	 * Extend&implement this method
	 * 
	 * Either of these methods should return not null (getBeanIdentifier, getCreatorClass)
	 * 
	 * @return Bean identifier in the container (currently spring or jsf) of the specific managers creator
	 * 
	 */
	public String getBeanIdentifier() {
		
		return null;
	}
	
	/* ==== */
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void onApplicationEvent(ApplicationEvent applicationevent) {
	
		if(applicationevent instanceof ContextRefreshedEvent) {
			
			applicationContext.publishEvent(getApplicationEvent());
		}
	}
	
	protected BPMManagersFactoryPluggedInEvent getApplicationEvent() {
	
		return new BPMManagersFactoryPluggedInEvent(this);
	}
}