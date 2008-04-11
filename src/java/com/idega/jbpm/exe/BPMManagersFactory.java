package com.idega.jbpm.exe;


/**
 * This class is intended to be implemented by specific managers (either view or process, for now) creators.
 * Those would be registered to BMPFactory(Impl) and therefore be known to it, when requested.
 * The idea behind this is, that for each process definition, there might be specific behavior managers.
 * E.g. manager for cases (CasesBpmViewManager or CasesBpmProcessManager).
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/04/11 01:28:24 $ by $Author: civilis $
 */
public interface BPMManagersFactory {
	
	public abstract ViewManager getViewManager();
	
	public abstract ProcessManager getProcessManager();
	
	public abstract String getManagersType();

	public abstract String getBeanIdentifier();
}