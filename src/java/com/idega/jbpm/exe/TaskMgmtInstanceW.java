package com.idega.jbpm.exe;

import java.util.List;

/**
 * additional methods for manipulating task instances
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/20 19:18:04 $ by $Author: civilis $
 */
public interface TaskMgmtInstanceW {

	/**
	 * initializes if not already and returns itself (for convenience). Used in ProcessInstanceW
	 * implementations
	 */
	public abstract TaskMgmtInstanceW init(ProcessInstanceW piw);

	public abstract TaskInstanceW createTask(final String taskName, final long tokenId, boolean loadView);

	public abstract void hideTaskInstances(List<TaskInstanceW> tiws);
}