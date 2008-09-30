package com.idega.jbpm.signing;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;


/**
 * @author <a href="mailto:juozas@idega.com">Juozapas Zabukas</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/09/30 18:05:09 $ by $Author: civilis $
 */
public class SigningHandler implements Signsdf {
	
	@Autowired private BuilderLogicWrapper builderLogicWrapper;

//	IWResourceBundle iwrb, String taskInstanceId, String hashValue, String image, String uri, String message,
//	String errorMessage
	public String getSigningAction(Long taskInstanceId, String hashValue, String message, String errorMessage) {
		
//		String uri = getBuilderLogicWrapper().getBuilderService(IWMainApplication.getDefaultIWApplicationContext()).getUriToObject(AscertiaSigningForm.class, null);
		String uri = null;
		
		IWResourceBundle iwrb = null;
		
		String parameters = null;
		
//		String parameters = new StringBuilder("['").append(AscertiaConstants.PARAM_TASK_ID).append("', '").append(AscertiaConstants.PARAM_VARIABLE_HASH)
//		.append("']").toString();
		
		hashValue = StringUtil.isEmpty(hashValue) ? CoreConstants.MINUS : hashValue;
		String values = new StringBuilder("['").append(taskInstanceId).append("', '").append(hashValue).append("']").toString();
		return new StringBuilder("(event, '").append(uri).append("', ").append(parameters).append(", ").append(values).append(", '").append(message)
						.append("', '").append(iwrb.getLocalizedString("document_signing_form", "Document signing form")).append("', '")
						.append(iwrb.getLocalizedString("close_signing_form", "Close signing form")).append("', '").append(errorMessage).append("');")
						.toString();
	}

	BuilderLogicWrapper getBuilderLogicWrapper() {
		return builderLogicWrapper;
	}

	void setBuilderLogicWrapper(BuilderLogicWrapper builderLogicWrapper) {
		this.builderLogicWrapper = builderLogicWrapper;
	}
}