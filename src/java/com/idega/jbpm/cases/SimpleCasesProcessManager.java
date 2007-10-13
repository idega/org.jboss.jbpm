package com.idega.jbpm.cases;

import javax.faces.context.FacesContext;

import org.jboss.jbpm.IWBundleStarter;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/10/13 10:43:08 $ by $Author: civilis $
 *
 */
public class SimpleCasesProcessManager {
	
	private JbpmConfiguration jbpmConfiguration;
	private String formName;
	private String message;

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
		
		JbpmConfiguration cfg = getJbpmConfiguration();
		JbpmContext ctx = cfg.createJbpmContext();
		
		try {
			ProcessDefinition pd = ctx.getGraphSession().findLatestProcessDefinition("new name");
			
			IWMainApplication iwma = IWMainApplication.getIWMainApplication(FacesContext.getCurrentInstance());
			IWBundle bundle = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
			
			pd.setName("xxx1");
			ctx.getGraphSession().deployProcessDefinition(pd);
			
		} finally {
			ctx.close();
		}
		
		setMessage("you pressed action: "+getFormName());
//		gotoFormbuilder
		return null;
	}

	public String getMessage() {
		return message == null ? "" : message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}