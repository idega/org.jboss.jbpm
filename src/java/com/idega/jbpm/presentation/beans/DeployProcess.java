package com.idega.jbpm.presentation.beans;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

import com.idega.jbpm.BPMContext;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/06/15 15:58:50 $ by $Author: civilis $
 *
 */
public class DeployProcess {
	
	private UploadedFile pd;
	private BPMContext idegaJbpmContext;
	
	public UploadedFile getProcessDefinition() {
		return pd;
	}
	
	public void setProcessDefinition(UploadedFile pd) {
		this.pd = pd;
	}
	
	public void upload() {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		InputStream is = null;
		
		try {
			is = getProcessDefinition().getInputStream();
			ctx.deployProcessDefinition(ProcessDefinition.parseXmlInputStream(is));

		} catch (IOException e) {
			
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while reading process while getting process definition input stream", e);
//			TODO: display err msg
			
		} catch (Exception e) {
			
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while deploying process definition", e);
//			TODO: display err msg				
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
}