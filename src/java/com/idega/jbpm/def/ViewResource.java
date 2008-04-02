package com.idega.jbpm.def;

import java.io.IOException;

import com.idega.idegaweb.IWMainApplication;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/04/02 19:25:54 $ by $Author: civilis $
 */
public interface ViewResource {

	public abstract View store(IWMainApplication iwma) throws IOException;

	public abstract String getTaskName();
}