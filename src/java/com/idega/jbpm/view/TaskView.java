package com.idega.jbpm.view;

import org.jbpm.taskmgmt.def.Task;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/19 13:52:40 $ by $Author: civilis $
 */
public interface TaskView extends View {

	public abstract Task getTask();
}