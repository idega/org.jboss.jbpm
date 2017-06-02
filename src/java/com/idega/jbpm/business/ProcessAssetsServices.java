package com.idega.jbpm.business;

import java.util.List;

import com.idega.jbpm.bean.BPMAttachment;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.presentation.IWContext;

public interface ProcessAssetsServices {

	public IWContext getIWContext(boolean checkIfLogged);

	/**
	 *
	 * @param piId - ID of process instance
	 * @param tasksNamesToReturn - names of tasks to return (optional)
	 * @param showExternalEntity - false by default
	 * @return
	 */
	public List<BPMDocument> getTasks(Long piId, List<String> tasksNamesToReturn, boolean showExternalEntity);

	/**
	 *
	 * @param piId - ID of process instance
	 * @param tasksNamesToReturn - names of documents to return (optional)
	 * @param showExternalEntity - false by default
	 * @param allowPDFSigning - false by default
	 * @return
	 */
	public List<BPMDocument> getDocuments(Long piId, List<String> tasksNamesToReturn, boolean showExternalEntity, boolean allowPDFSigning);

	/**
	 *
	 * @param piId - ID of process instance
	 * @return
	 */
	public List<BPMAttachment> getAttachments(Long piId);

	/**
	 *
	 * @param tasks
	 * @return
	 */
	public List<BPMAttachment> getAttachments(List<TaskInstanceW> tasks);

}