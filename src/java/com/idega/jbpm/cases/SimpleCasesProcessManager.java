package com.idega.jbpm.cases;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.xml.parsers.DocumentBuilder;

import org.jboss.jbpm.IWBundleStarter;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.w3c.dom.Document;

import com.idega.documentmanager.business.DocumentManager;
import com.idega.documentmanager.business.DocumentManagerFactory;
import com.idega.documentmanager.business.PersistenceManager;
import com.idega.documentmanager.component.beans.LocalizedStringBean;
import com.idega.documentmanager.util.FormManagerUtil;
import com.idega.idegaweb.DefaultIWBundle;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/10/14 10:54:58 $ by $Author: civilis $
 *
 */
public class SimpleCasesProcessManager {
	
	private JbpmConfiguration jbpmConfiguration;
	private String formName;
	private String message;
	private DocumentManagerFactory documentManagerFactory;
	private PersistenceManager persistenceManager;
	
	private String processDefinitionTemplateLocation;
	private String createRequestFormTemplateLocation;
	private String createResponseFormTemplateLocation;

	public PersistenceManager getPersistenceManager() {
		return persistenceManager;
	}

	public void setPersistenceManager(PersistenceManager persistenceManager) {
		this.persistenceManager = persistenceManager;
	}

	public DocumentManagerFactory getDocumentManagerFactory() {
		return documentManagerFactory;
	}

	public void setDocumentManagerFactory(
			DocumentManagerFactory documentManagerFactory) {
		this.documentManagerFactory = documentManagerFactory;
	}

	public String getFormName() {
		return formName;
	}

	public void setFormName(String formName) {
		this.formName = formName;
	}

	public JbpmConfiguration getJbpmConfiguration() {
		return jbpmConfiguration;
	}

	public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}
	
	public String createNewSimpleProcess() {

		if(getFormName() == null || getFormName().equals("")) {
		
			setMessage("Form name not set");
			return null;
		}
			
		JbpmConfiguration cfg = getJbpmConfiguration();
		JbpmContext ctx = cfg.createJbpmContext();
		
		try {
			FacesContext facesCtx = FacesContext.getCurrentInstance();
			IWMainApplication iwma = IWMainApplication.getIWMainApplication(facesCtx);
			
			InputStream pdIs = getResourceInputStream(iwma, getProcessDefinitionTemplateLocation());
			InputStream createReqFormIs = getResourceInputStream(iwma, getCreateRequestFormTemplateLocation());
			InputStream createResFormIs = getResourceInputStream(iwma, getCreateResponseFormTemplateLocation());
			
			ProcessDefinition pd = ProcessDefinition.parseXmlInputStream(pdIs);
			
//			TODO: create transaction rollback here. if any of deployings fails, rollback everything.
			pd.setName(getFormName()+" Process");
			ctx.getGraphSession().deployProcessDefinition(pd);
			
			DocumentManager documentManager = getDocumentManagerFactory().newDocumentManager(facesCtx);
			DocumentBuilder builder = FormManagerUtil.getDocumentBuilder();
			
			loadAndSaveForm(builder, documentManager, getFormName()+" request", createReqFormIs);
			loadAndSaveForm(builder, documentManager, getFormName()+" response", createResFormIs);
			
		} catch (IOException e) {
			setMessage("IO Exception occured");
			e.printStackTrace();
		} catch (Exception e) {
			setMessage("Exception occured");
			e.printStackTrace();
		} finally {
			ctx.close();
		}
		
//		gotoFormbuilder
		return null;
	}
	
	private void loadAndSaveForm(DocumentBuilder builder, DocumentManager documentManager, String formName, InputStream formIs) throws Exception {
		
		Document xformXml = builder.parse(formIs);
		PersistenceManager persistenceManager = getPersistenceManager();
		
		String formId = persistenceManager.generateFormId(formName);
		com.idega.documentmanager.business.Document form = documentManager.openForm(xformXml, formId);
		
		LocalizedStringBean title = form.getFormTitle();
		
		for (Locale titleLocale : title.getLanguagesKeySet())
			title.setString(titleLocale, formName);
		
		form.setFormTitle(title);
		form.save();
	}

	public String getMessage() {
		return message == null ? "" : message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	private InputStream getResourceInputStream(IWMainApplication iwma, String pathWithinBundle) throws IOException {

		IWBundle bundle = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		String workspaceDir = System.getProperty(DefaultIWBundle.SYSTEM_BUNDLES_RESOURCE_DIR);
		
		if(workspaceDir != null) {
			
			String bundleInWorkspace = new StringBuilder(workspaceDir).append("/").append(IWBundleStarter.IW_BUNDLE_IDENTIFIER).append("/").toString();
			return new FileInputStream(bundleInWorkspace + pathWithinBundle);
		}
						
		return bundle.getResourceInputStream(pathWithinBundle);
	}

	public String getProcessDefinitionTemplateLocation() {
		return processDefinitionTemplateLocation;
	}

	public void setProcessDefinitionTemplateLocation(
			String processDefinitionTemplateLocation) {
		this.processDefinitionTemplateLocation = processDefinitionTemplateLocation;
	}

	public String getCreateRequestFormTemplateLocation() {
		return createRequestFormTemplateLocation;
	}

	public void setCreateRequestFormTemplateLocation(
			String createRequestFormTemplateLocation) {
		this.createRequestFormTemplateLocation = createRequestFormTemplateLocation;
	}

	public String getCreateResponseFormTemplateLocation() {
		return createResponseFormTemplateLocation;
	}

	public void setCreateResponseFormTemplateLocation(
			String createResponseFormTemplateLocation) {
		this.createResponseFormTemplateLocation = createResponseFormTemplateLocation;
	}
}