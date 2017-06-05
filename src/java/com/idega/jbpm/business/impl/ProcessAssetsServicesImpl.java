package com.idega.jbpm.business.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.MediaWritable;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.jbpm.bean.BPMAttachment;
import com.idega.jbpm.business.ProcessAssetsServices;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ProcessAssetsServicesImpl extends DefaultSpringBean implements ProcessAssetsServices {

	@Autowired
	private BPMFactory bpmFactory;

	@Override
	public IWContext getIWContext(boolean checkIfLogged) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			getLogger().warning("IWContext is unavailable!");
		}

		if (checkIfLogged && !iwc.isLoggedOn()) {
			return null;
		}

		return iwc;
	}

	@Override
	public List<BPMDocument> getTasks(Long piId, List<String> tasksNamesToReturn, boolean showExternalEntity) {
		try {
			if (piId == null || piId < 0) {
				return null;
			}

			IWContext iwc = getIWContext(true);
			if (iwc == null) {
				return null;
			}

			List<BPMDocument> tasksDocuments = new ArrayList<>();

			User loggedInUser = bpmFactory.getBpmUserFactory().getCurrentBPMUser().getUserToUse();
			Locale userLocale = iwc.getCurrentLocale();

			try {
				tasksDocuments = loggedInUser == null ?
						null :
						bpmFactory.getProcessManagerByProcessInstanceId(piId).getProcessInstance(piId).getTaskDocumentsForUser(
							loggedInUser,
							userLocale,
							showExternalEntity,
							tasksNamesToReturn
				);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting tasks for process instance: " + piId + " and user: " + loggedInUser + " using locale: " +	userLocale, e);
			}
			tasksDocuments = tasksDocuments == null ? new ArrayList<BPMDocument>(0) : tasksDocuments;

			if (!ListUtil.isEmpty(tasksDocuments)) {
				Collections.sort(tasksDocuments, (c1, c2) -> c2.getDocumentName().compareTo(c1.getDocumentName()));
			}

			return tasksDocuments;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting BPM tasks for proc. inst. ID: " + piId, e);
		}
		return null;
	}

	@Override
	public List<BPMDocument> getDocuments(Long piId, List<String> tasksNamesToReturn, boolean showExternalEntity, boolean allowPDFSigning) {
		return getSubmittedTasks(piId, tasksNamesToReturn, showExternalEntity, allowPDFSigning, BPMDocument.class);
	}
	public List<TaskInstanceW> getSubmittedTasks(Long piId, List<String> tasksNamesToReturn, boolean showExternalEntity, boolean allowPDFSigning) {
		return getSubmittedTasks(piId, tasksNamesToReturn, showExternalEntity, allowPDFSigning, TaskInstanceW.class);
	}

	private <T> List<T> getSubmittedTasks(Long piId, List<String> tasksNamesToReturn, boolean showExternalEntity, boolean allowPDFSigning, Class<T> resultType) {
		try {
			if (piId == null) {
				return null;
			}
			IWContext iwc = getIWContext(true);
			if (iwc == null) {
				return null;
			}

			User loggedInUser = bpmFactory.getBpmUserFactory().getCurrentBPMUser().getUserToUse();
			Locale userLocale = iwc.getCurrentLocale();

			ProcessInstanceW pi = bpmFactory.getProcessManagerByProcessInstanceId(piId).getProcessInstance(piId);
			if (resultType.getName().equals(BPMDocument.class.getName())) {
				@SuppressWarnings("unchecked")
				List<T> results = (List<T>) pi.getSubmittedDocumentsForUser(loggedInUser, userLocale, showExternalEntity, allowPDFSigning, tasksNamesToReturn);
				return results;
			} else if (resultType.getName().equals(TaskInstanceW.class.getName())) {
				@SuppressWarnings("unchecked")
				List<T> results = (List<T>) pi.getSubmittedTasksForUser(loggedInUser, userLocale, showExternalEntity, allowPDFSigning, tasksNamesToReturn);
				return results;
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting BPM documents for proc. inst. ID: " + piId, e);
		}
		return null;
	}

	@Override
	public List<BPMAttachment> getAttachments(Long piId) {
		List<TaskInstanceW> tasks = getSubmittedTasks(piId, null, false, false);
		return getAttachments(tasks);
	}

	@Override
	public List<BPMAttachment> getAttachments(List<TaskInstanceW> tasks) {
		try {
			if (ListUtil.isEmpty(tasks)) {
				return null;
			}

			String mediaServletURI = IWMainApplication.getDefaultIWMainApplication().getMediaServletURI();
			String encrytptedURI = IWMainApplication.getEncryptedClassName(AttachmentWriter.class);

			List<BPMAttachment> attachments = new ArrayList<>();
			for (TaskInstanceW tiW: tasks) {
				List<BinaryVariable> binaryVariables = tiW.getAttachments();
				if (ListUtil.isEmpty(binaryVariables)) {
					continue;
				}

				Long tiId = tiW.getTaskInstanceId();
				for (BinaryVariable binaryVariable: binaryVariables) {
					if (binaryVariable.getHash() == null || (binaryVariable.getHidden() != null && binaryVariable.getHidden() == true)) {
						continue;
					}

					BPMAttachment attachment = new BPMAttachment();
					attachments.add(attachment);

					String hash = binaryVariable.getHash().toString();
					attachment.setId(hash);

					String description = binaryVariable.getDescription();
					attachment.setDescription(StringUtil.isEmpty(description) ? binaryVariable.getFileName() : description);

					String fileName = binaryVariable.getFileName();
					attachment.setFileName(fileName);

					Long fileSize = binaryVariable.getContentLength();
					attachment.setFileSize(FileUtil.getHumanReadableSize(fileSize == null ? Long.valueOf(0) : fileSize));

					URIUtil uri = new URIUtil(mediaServletURI);
					uri.setParameter(MediaWritable.PRM_WRITABLE_CLASS, encrytptedURI);
					uri.setParameter(AttachmentWriter.PARAMETER_TASK_INSTANCE_ID, tiId.toString());
					uri.setParameter(AttachmentWriter.PARAMETER_VARIABLE_HASH, hash);
					attachment.setDownloadLink(uri.getUri());
				}
			}
			return attachments;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting attachments for tasks: " + tasks, e);
		}
		return null;
	}

}