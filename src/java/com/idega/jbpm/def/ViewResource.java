package com.idega.jbpm.def;

import java.io.IOException;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/01/27 13:11:34 $ by $Author: civilis $
 */
public interface ViewResource {

	public abstract View store() throws IOException;

	public abstract String getTaskName();
}