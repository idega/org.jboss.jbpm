package com.idega.jbpm.def;

import java.io.IOException;

import javax.faces.context.FacesContext;

import com.idega.idegaweb.IWBundle;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/01/06 17:02:59 $ by $Author: civilis $
 */
public interface ProcessBundleManager {

	public abstract ProcessBundle createBundle(FacesContext facesCtx, IWBundle bundle, String templateBundleLocationWithinBundle, String formName, Object parameters) throws IOException;
}