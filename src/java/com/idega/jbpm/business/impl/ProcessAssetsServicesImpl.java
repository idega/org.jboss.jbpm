package com.idega.jbpm.business.impl;

import java.io.Serializable;
import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.IWTimestamp;
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
	public <T extends Serializable> List<BPMDocument> getTasks(T piId) {
		return getTasks(piId, null, false);
	}

	@Override
	public <T extends Serializable> List<BPMDocument> getTasks(T piId, User user, List<TaskInstanceW> tasks) {
		if (piId == null || piId == null) {
			return null;
		}

		Locale locale = null;
		List<BPMDocument> tasksDocuments = new ArrayList<>();
		try {
			IWContext iwc = getIWContext(false);
			if (iwc == null) {
				return null;
			}

			locale = iwc.getCurrentLocale();

			tasksDocuments = bpmFactory.getProcessManagerByProcessInstanceId(piId).getProcessInstance(piId).getTaskDocumentsForUser(iwc, user, locale, tasks);
			if (!ListUtil.isEmpty(tasksDocuments)) {
				Collections.sort(tasksDocuments, (c1, c2) -> c2.getDocumentName().compareTo(c1.getDocumentName()));
			}

			return tasksDocuments;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting tasks for process instance: " + piId + " and user: " + user + " using locale: " +	locale + " and tasks " + tasks, e);
		}
		return null;
	}

	@Override
	public <T extends Serializable> List<BPMDocument> getTasks(T piId, List<String> tasksNamesToReturn, boolean showExternalEntity) {
		try {
			if (piId == null || piId == null) {
				return null;
			}

			IWContext iwc = getIWContext(false);
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
							iwc,
							loggedInUser,
							userLocale,
							showExternalEntity,
							tasksNamesToReturn
				);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error getting tasks for process instance: " + piId + " and user: " + loggedInUser + " using locale: " +	userLocale, e);
			}
			tasksDocuments = tasksDocuments == null ? new ArrayList<>(0) : tasksDocuments;

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
	public <T extends Serializable> List<BPMDocument> getDocuments(T piId) {
		return getDocuments(piId, null, false, false);
	}

	@Override
	public <T extends Serializable> List<BPMDocument> getDocuments(T piId, User user, List<TaskInstanceW> submittedTasks) {
		if (piId == null || piId == null) {
			return null;
		}

		Locale locale = null;
		try {
			IWContext iwc = getIWContext(false);
			if (iwc == null) {
				return null;
			}

			locale = iwc.getCurrentLocale();

			return bpmFactory.getProcessManagerByProcessInstanceId(piId).getProcessInstance(piId).getSubmittedDocumentsForUser(iwc, user, locale, submittedTasks);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting tasks for process instance: " + piId + " and user: " + user + " using locale: " +	locale + " and submitted tasks " + submittedTasks, e);
		}
		return null;
	}

	@Override
	public <T extends Serializable> List<BPMDocument> getDocuments(T piId, List<String> tasksNamesToReturn, boolean showExternalEntity, boolean allowPDFSigning) {
		return getSubmittedTasks(piId, tasksNamesToReturn, showExternalEntity, allowPDFSigning, BPMDocument.class);
	}

	@Override
	public <T extends Serializable> List<TaskInstanceW> getSubmittedTasks(T piId) {
		return getSubmittedTasks(piId, null, false, false);
	}

	private <T extends Serializable> List<TaskInstanceW> getSubmittedTasks(T piId, List<String> tasksNamesToReturn, boolean showExternalEntity, boolean allowPDFSigning) {
		return getSubmittedTasks(piId, tasksNamesToReturn, showExternalEntity, allowPDFSigning, TaskInstanceW.class);
	}

	private <T extends Serializable, C> List<C> getSubmittedTasks(T piId, List<String> tasksNamesToReturn, boolean showExternalEntity, boolean allowPDFSigning, Class<C> resultType) {
		try {
			if (piId == null) {
				return null;
			}

			IWContext iwc = getIWContext(false);
			if (iwc == null) {
				return null;
			}

			User loggedInUser = bpmFactory.getBpmUserFactory().getCurrentBPMUser().getUserToUse();
			Locale userLocale = iwc.getCurrentLocale();

			ProcessInstanceW pi = bpmFactory.getProcessManagerByProcessInstanceId(piId).getProcessInstance(piId);
			if (resultType.getName().equals(BPMDocument.class.getName())) {
				@SuppressWarnings("unchecked")
				List<C> results = (List<C>) pi.getSubmittedDocumentsForUser(iwc, loggedInUser, userLocale, showExternalEntity, allowPDFSigning, tasksNamesToReturn);
				return results;
			} else if (resultType.getName().equals(TaskInstanceW.class.getName())) {
				@SuppressWarnings("unchecked")
				List<C> results = (List<C>) pi.getSubmittedTasksForUser(iwc, loggedInUser, userLocale, showExternalEntity, allowPDFSigning, tasksNamesToReturn);
				return results;
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting BPM documents for proc. inst. ID: " + piId, e);
		}
		return null;
	}

	@Override
	public <T extends Serializable> List<BPMDocument> getBPMDocuments(T piId, List<TaskInstanceW> tiWs, Locale locale) {
		try {
			ProcessInstanceW pi = bpmFactory.getProcessManagerByProcessInstanceId(piId).getProcessInstance(piId);
			return pi.getBPMDocuments(tiWs, locale);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting BPM documents for proc. inst. ID: " + piId + " and task instances: " + tiWs, e);
		}
		return null;
	}

	@Override
	public <T extends Serializable> List<BPMAttachment> getAttachments(T piId) {
		List<TaskInstanceW> tasks = getSubmittedTasks(piId, null, false, false);
		return getAttachments(tasks);
	}

	@Override
	public List<BPMAttachment> getAttachments(List<BinaryVariable> binaryVariables, Date submittedAt, Serializable id) {
		if (ListUtil.isEmpty(binaryVariables)) {
			return null;
		}

		Locale locale = getCurrentLocale();
		String mediaServletURI = IWMainApplication.getDefaultIWMainApplication().getMediaServletURI();
		String encrytptedURI = IWMainApplication.getEncryptedClassName(AttachmentWriter.class);

		List<BPMAttachment> attachments = new ArrayList<>();
		for (BinaryVariable binaryVariable: binaryVariables) {
			if (binaryVariable == null || (binaryVariable.getHidden() != null && binaryVariable.getHidden() == true)) {
				continue;
			}

			BPMAttachment attachment = new BPMAttachment();
			attachments.add(attachment);

			attachment.setTimestamp(submittedAt);

			Integer hash = binaryVariable.getHash();
			String attachmentId = hash == null ? binaryVariable.getFileName().concat(id == null ? CoreConstants.EMPTY : id.toString()) : hash.toString();
			attachment.setId(attachmentId);

			String description = binaryVariable.getDescription();
			attachment.setDescription(StringUtil.isEmpty(description) ? binaryVariable.getFileName() : description);

			String fileName = binaryVariable.getFileName();
			attachment.setFileName(fileName);

			Long fileSize = binaryVariable.getContentLength();
			attachment.setFileSize(FileUtil.getHumanReadableSize(fileSize == null ? Long.valueOf(0) : fileSize));

			if (id != null && hash != null) {
				URIUtil uri = new URIUtil(mediaServletURI);
				uri.setParameter(MediaWritable.PRM_WRITABLE_CLASS, encrytptedURI);
				uri.setParameter(AttachmentWriter.PARAMETER_TASK_INSTANCE_ID, id.toString());
				uri.setParameter(AttachmentWriter.PARAMETER_VARIABLE_HASH, hash.toString());
				attachment.setDownloadLink(uri.getUri());
				attachment.setSource(binaryVariable.getIdentifier());
			} else {
				attachment.setDownloadLink(binaryVariable.getIdentifier());
			}

			if (submittedAt != null) {
				attachment.setDate(new IWTimestamp(submittedAt).getLocaleDateAndTime(locale, DateFormat.MEDIUM, DateFormat.MEDIUM));
			}

			Map<String, Object> metadata = binaryVariable.getMetadata();
			if (metadata != null){
				Object sc  = metadata.get(JBPMConstants.SOURCE);
				if (sc != null){
					attachment.setSource(sc.toString());
				}
			}
		}

		return attachments;
	}

	@Override
	public List<BPMAttachment> getAttachments(List<TaskInstanceW> tasks) {
		try {
			if (ListUtil.isEmpty(tasks)) {
				return null;
			}

			Locale locale = getCurrentLocale();
			IWContext iwc = getIWContext(false);

			List<BPMAttachment> attachments = new ArrayList<>();
			for (TaskInstanceW tiW: tasks) {
				List<BinaryVariable> binaryVariables = tiW.getAttachments(iwc);
				if (ListUtil.isEmpty(binaryVariables)) {
					continue;
				}

				List<BPMAttachment> taskAttachments = getAttachments(binaryVariables, tiW.getEnd(), tiW.getTaskInstanceId());
				if (!ListUtil.isEmpty(taskAttachments)) {
					attachments.addAll(taskAttachments);
				}
			}

			if (!ListUtil.isEmpty(attachments)) {
				try {
					Collator collator = Collator.getInstance(locale);
					Collections.sort(attachments, new Comparator<BPMAttachment>() {

						@Override
						public int compare(BPMAttachment o1, BPMAttachment o2) {
							return collator.compare(o1.getFileName(), o2.getFileName());
						}

					});
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error sorting attachments by names: " + attachments, e);
				}
			}

			return attachments;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting attachments for tasks: " + tasks, e);
		}
		return null;
	}

}