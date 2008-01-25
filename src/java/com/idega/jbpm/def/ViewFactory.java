package com.idega.jbpm.def;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/01/25 15:24:26 $ by $Author: civilis $
 */
public interface ViewFactory {

	public abstract String getViewType();
	
	public abstract View getViewNoLoad(String viewIdentifier);
	
	public abstract View getView(String viewIdentifier, boolean submitable);
	
	public abstract String getBeanIdentifier();
}
