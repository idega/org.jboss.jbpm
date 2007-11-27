package com.idega.jbpm.def;

import com.idega.jbpm.data.ViewTaskBind;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/11/27 16:33:26 $ by $Author: civilis $
 */
public interface ViewFactory {

	public abstract View createView();
	
	public abstract View createView(ViewTaskBind viewTaskBind);
	
	public abstract String getViewType();
}
