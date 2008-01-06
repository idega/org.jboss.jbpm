package com.idega.jbpm.def;

import java.util.List;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/01/06 17:02:59 $ by $Author: civilis $
 */
public interface ProcessBundle {

	public abstract Long getProcessDefinitionId();
	
	public abstract List<View> getBundleViews();
	
	public abstract void remove();
}