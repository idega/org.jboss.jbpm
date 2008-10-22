package com.idega.jbpm.exe;

/**
 * assigns/unassigns process to the user's process list (usually used for admins - to put the process into mycases list)
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/10/22 15:10:18 $ by $Author: civilis $
 */
public interface ProcessWatch {
	
	public abstract boolean takeWatch(Long processInstanceId);
	
	public abstract boolean assignWatch(Long processInstanceId, Integer assignedUserId);
	
	public abstract boolean removeWatch(Long processInstanceId);
	
	public abstract boolean removeWatch(Long processInstanceId, Integer userIdToRemoveFrom);
	
	public abstract boolean isWatching(Long processInstanceId);
	
	public abstract String getWatchCaseStatusMessage(boolean isWatched);
	
	public abstract String getWatchCaseStatusLabel(boolean isWatched);
}