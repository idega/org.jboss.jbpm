package com.idega.jbpm.view;

import java.io.IOException;

import com.idega.idegaweb.IWMainApplication;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/05/19 13:52:40 $ by $Author: civilis $
 */
public interface ViewResource {

	public abstract View store(IWMainApplication iwma) throws IOException;

	public abstract String getTaskName();
}