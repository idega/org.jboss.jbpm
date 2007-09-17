package com.idega.jbpm.def;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/09/17 13:33:39 $ by $Author: civilis $
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

		JbpmConfiguration cfg = getJbpmConfiguration();
		JbpmContext ctx = cfg.createJbpmContext();
		
		try {
			
			ctx.deployProcessDefinition(ProcessDefinition.parseXmlInputStream(is));
			
		} catch (Exception e) {
			
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while deploying process definition", e);
//			TODO: display err msg				
			
		} finally {
				ctx.close();
		}
	}
	
	private JbpmConfiguration cfg;
	
	public void setJbpmConfiguration(JbpmConfiguration cfg) {
		this.cfg = cfg;
	}
	
	public JbpmConfiguration getJbpmConfiguration() {
		return cfg;
	}
}