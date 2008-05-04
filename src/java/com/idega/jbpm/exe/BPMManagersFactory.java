package com.idega.jbpm.exe;


/**
 * This class is intended to be implemented by specific managers (either view or process, for now) creators.
 * Those would be registered to BMPFactory(Impl) and therefore be known to it, when requested.
 * The idea behind this is, that for each process definition, there might be specific behavior managers.
 * E.g. manager for cases (CasesBpmViewManager or CasesBpmProcessManager).
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/05/04 18:12:26 $ by $Author: civilis $
 */
public interface BPMManagersFactory {
	
	public abstract ProcessManager getProcessManager();
	
	public abstract String getManagersType();

	public abstract String getBeanIdentifier();
}