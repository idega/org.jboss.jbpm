package com.idega.jbpm.def;

import org.jbpm.taskmgmt.def.Task;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/10 01:20:26 $ by $Author: civilis $
 */
public interface TaskView extends View {

	public abstract Task getTask();
}