package com.idega.jbpm.presentation.beans;

import java.io.IOException;
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
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/09/26 07:33:19 $ by $Author: alexis $
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
		JbpmContext ctx = null;
		
		try {
			is = getProcessDefinition().getInputStream();
			
			JbpmConfiguration cfg = getJbpmConfiguration();
			ctx = cfg.createJbpmContext();
			ctx.deployProcessDefinition(ProcessDefinition.parseXmlInputStream(is));

		} catch (IOException e) {
			
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while reading process while getting process definition input stream", e);
//			TODO: display err msg
			
		} catch (Exception e) {
			
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while deploying process definition", e);
//			TODO: display err msg				
			
		} finally {
			if(ctx != null)
				ctx.close();
		}
	}
	
	private JbpmProcessBusinessBean jbpmProcessBusiness;
	private JbpmConfiguration cfg;
	
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