package com.idega.jbpm.exe;

public interface ProcessWatch {
	
	public abstract boolean takeWatch(Long processInstanceId);
	
	public abstract boolean assignWatch(Long processInstanceId, Integer assignedUserId);
	
	public abstract boolean removeWatch(Long processInstanceId);
	
	public abstract boolean removeWatch(Long processInstanceId, Integer userIdToRemoveFrom);
	
	public abstract boolean isWatching(Long processInstanceId);
	
	public abstract String getWatchCaseStatusMessage(boolean isWatched);
	
	public abstract String getWatchCaseStatusLabel(boolean isWatched);
}