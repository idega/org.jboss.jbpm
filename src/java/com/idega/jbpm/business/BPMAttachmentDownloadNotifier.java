package com.idega.jbpm.business;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.file.FileDownloadNotificationProperties;
import com.idega.business.file.FileDownloadNotifier;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.dwr.business.DWRAnnotationPersistance;
import com.idega.idegaweb.IWMainApplication;
import com.idega.io.MediaWritable;
import com.idega.jbpm.artifacts.presentation.AttachmentWriter;
import com.idega.jbpm.bean.BPMAttachmentDownloadNotificationProperties;
import com.idega.user.data.User;
import com.idega.util.URIUtil;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(BPMAttachmentDownloadNotifier.BEAN_IDENTIFIER)
@RemoteProxy(creator=SpringCreator.class, creatorParams={
	@Param(name="beanName", value=BPMAttachmentDownloadNotifier.BEAN_IDENTIFIER),
	@Param(name="javascript", value=BPMAttachmentDownloadNotifier.DWR_OBJECT)
}, name=BPMAttachmentDownloadNotifier.DWR_OBJECT)
public class BPMAttachmentDownloadNotifier extends FileDownloadNotifier implements DWRAnnotationPersistance {

	private static final long serialVersionUID = -6038272277947394981L;

	public static final String BEAN_IDENTIFIER = "bpmAttachmentDownloadNotifier";
	public static final String DWR_OBJECT = "BPMAttachmentDownloadNotifier";

	@RemoteMethod
	public AdvancedProperty sendDownloadNotifications(BPMAttachmentDownloadNotificationProperties properties) { 
		return super.sendNotifications(properties);
	}

	@Override
	public String getUriToAttachment(FileDownloadNotificationProperties properties, User user) {
		if (!(properties instanceof BPMAttachmentDownloadNotificationProperties)) {
			return null;
		}
		
		BPMAttachmentDownloadNotificationProperties bpmProperties = (BPMAttachmentDownloadNotificationProperties) properties;
		
		URIUtil uri = new URIUtil(IWMainApplication.getDefaultIWMainApplication().getMediaServletURI());
		
		uri.setParameter(MediaWritable.PRM_WRITABLE_CLASS, IWMainApplication.getEncryptedClassName(AttachmentWriter.class));
		uri.setParameter(AttachmentWriter.PARAMETER_TASK_INSTANCE_ID, bpmProperties.getTaskId().toString());
		uri.setParameter(AttachmentWriter.PARAMETER_VARIABLE_HASH, bpmProperties.getHash().toString());
		
		if (user != null) {
			uri.setParameter(LoginBusinessBean.PARAM_LOGIN_BY_UNIQUE_ID, user.getUniqueId());
			uri.setParameter(LoginBusinessBean.LoginStateParameter, LoginBusinessBean.LOGIN_EVENT_LOGIN);
		}
		
		return uri.getUri();
	}

}
