package com.idega.jbpm.artifacts.presentation.bean;

public interface BPMProcessWatcher {
	
	public static final String SPRING_BEAN_IDENTIFIER = "BPMProcessWatcher";
	
	public abstract boolean takeWatch(Long processInstanceId);
	
	public abstract boolean removeWatch(Long processInstanceId);
	
	public abstract boolean isWatching(Long processInstanceId);
	
	public abstract String getWatchCaseStatusMessage(boolean isWatched);
	
	public abstract String getWatchCaseStatusLabel(boolean isWatched);

}
