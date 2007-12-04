package com.idega.jbpm.def;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2007/12/04 14:06:02 $ by $Author: civilis $
 */
public interface ViewFactory {

	public abstract View createView();
	
	public abstract String getViewType();
	
	public abstract View getView(String viewIdentifier);
}
