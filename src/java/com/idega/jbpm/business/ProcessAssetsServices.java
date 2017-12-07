package com.idega.jbpm.business;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import com.idega.jbpm.bean.BPMAttachment;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.presentation.IWContext;

public interface ProcessAssetsServices {

	public IWContext getIWContext(boolean checkIfLogged);

	public <T extends Serializable> List<BPMDocument> getTasks(T piId);
	/**
	 *
	 * @param piId - ID of process instance
	 * @param tasksNamesToReturn - names of tasks to return (optional)
	 * @param showExternalEntity - false by default
	 * @return
	 */
	public <T extends Serializable> List<BPMDocument> getTasks(T piId, List<String> tasksNamesToReturn, boolean showExternalEntity);

	public <T extends Serializable> List<BPMDocument> getDocuments(T piId);
	/**
	 *
	 * @param piId - ID of process instance
	 * @param tasksNamesToReturn - names of documents to return (optional)
	 * @param showExternalEntity - false by default
	 * @param allowPDFSigning - false by default
	 * @return
	 */
	public <T extends Serializable> List<BPMDocument> getDocuments(T piId, List<String> tasksNamesToReturn, boolean showExternalEntity, boolean allowPDFSigning);

	public <T extends Serializable> List<TaskInstanceW> getSubmittedTasks(T piId);

	public <T extends Serializable> List<BPMDocument> getBPMDocuments(T piId, List<TaskInstanceW> tiWs, Locale locale);

	/**
	 *
	 * @param piId - ID of process instance
	 * @return
	 */
	public <T extends Serializable> List<BPMAttachment> getAttachments(T piId);

	/**
	 *
	 * @param tasks
	 * @return
	 */
	public List<BPMAttachment> getAttachments(List<TaskInstanceW> tasks);

}