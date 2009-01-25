package com.idega.jbpm.view;

import java.io.IOException;

import com.idega.idegaweb.IWMainApplication;

/**
 * represents one deployable view resource. Used to resolve and create View for
 * the task
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $ Last modified: $Date: 2009/01/25 15:36:31 $ by
 *          $Author: civilis $
 */
public interface ViewResource {

	/**
	 * 
	 * TODO: update javadoc
	 * <p>
	 * stores if necessary (to permanent location) and resolves the View of this
	 * resource, which reflects the stored view. E.g., XFormViewResource stores
	 * XForm and creates View, which reflects that.
	 * </p>
	 * 
	 * @param iwma
	 * @return view, reflecting task representation. View should contain
	 *         necessary information (viewId), so it can resolve relevant UI
	 *         object later
	 * @throws IOException
	 */
	public abstract void store(IWMainApplication iwma) throws IOException;

	/**
	 * @return task name this view represents
	 */
	public abstract String getTaskName();

	public abstract void setProcessName(String processName);

	public abstract void setViewResourceIdentifier(String viewResourceIdentifier);

	public abstract void setViewType(String viewType);

	public abstract String getViewType();

	public abstract String getViewId();
}