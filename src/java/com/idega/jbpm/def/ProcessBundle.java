package com.idega.jbpm.def;

import java.io.IOException;

import javax.faces.context.FacesContext;

import com.idega.idegaweb.IWBundle;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/12/04 14:06:02 $ by $Author: civilis $
 */
public interface ProcessBundle {

	public abstract void createDefinitions(FacesContext facesCtx, IWBundle bundle, String templateBundleLocationWithinBundle, String formName, Object parameters) throws IOException;
}