package com.idega.jbpm.def;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

import com.idega.jbpm.business.JbpmProcessBusinessBean;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2007/09/20 07:01:48 $ by $Author: alexis $
 *
 */
public class DeployProcess {
	
	private UploadedFile pd;
	
	public UploadedFile getProcessDefinition() {
		return pd;
	}
	
	public void setProcessDefinition(UploadedFile pd) {
		this.pd = pd;
	}
	
	public void upload() {
		
		InputStream is = null;
		
		try {
			is = getProcessDefinition().getInputStream();

		} catch (Exception e) {
			
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while reading process while getting process definition input stream", e);
//			TODO: display err msg
			return;
		}
		
		jbpmProcessBusiness.deployProcessDefinition(is);

	}
	
	private JbpmConfiguration cfg;
	private JbpmProcessBusinessBean jbpmProcessBusiness;
	
	public void setJbpmConfiguration(JbpmConfiguration cfg) {
		this.cfg = cfg;
	}
	
	public JbpmConfiguration getJbpmConfiguration() {
		return cfg;
	}

	public JbpmProcessBusinessBean getJbpmProcessBusiness() {
		return jbpmProcessBusiness;
	}

	public void setJbpmProcessBusiness(JbpmProcessBusinessBean jbpmProcessBusiness) {
		this.jbpmProcessBusiness = jbpmProcessBusiness;
	}
}