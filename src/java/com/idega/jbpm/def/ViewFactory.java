package com.idega.jbpm.def;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2007/12/06 20:32:29 $ by $Author: civilis $
 */
public interface ViewFactory {

	public abstract String getViewType();
	
	public abstract View getViewNoLoad(String viewIdentifier);
	
	public abstract View getView(String viewIdentifier, boolean submitable);
}
