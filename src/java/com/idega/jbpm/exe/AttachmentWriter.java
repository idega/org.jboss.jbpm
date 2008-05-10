package com.idega.jbpm.exe;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.idega.business.SpringBeanLookup;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.presentation.IWContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/05/10 18:08:07 $ by $Author: civilis $
 */
public class AttachmentWriter extends DownloadWriter implements MediaWritable {

	private BinaryVariable binaryVariable;
	private VariablesHandler variablesHandler;

	public AttachmentWriter() {
	}

	public void init(HttpServletRequest req, IWContext iwc) {
		
		String taskInstanceIdSR = iwc.getParameter("taskInstanceId");
		String variableHashSR = iwc.getParameter("varHash");
		
		if(taskInstanceIdSR == null || variableHashSR == null) {
			
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Tried to download, but params not provided.\nTaskInstanceId: "+taskInstanceIdSR+", variableHash: "+variableHashSR);
			return;
		}
		
		Long taskInstanceId = new Long(taskInstanceIdSR);
		Integer variableHash = new Integer(variableHashSR);
		
		variablesHandler = getVariablesHandler(iwc.getServletContext());
		
		binaryVariable = getBinVar(variablesHandler, taskInstanceId, variableHash);
		
		if(binaryVariable == null) {
			
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to resolve binary variable for: \nTaskInstanceId: "+taskInstanceIdSR+", variableHash: "+variableHashSR);
			return;
		}
		
		setAsDownload(iwc, binaryVariable.getFileName(), binaryVariable.getContentLength() == null ? 0 : binaryVariable.getContentLength().intValue());
	}

	public String getMimeType() {
		
		if(binaryVariable != null && binaryVariable.getMimeType() != null)
			return binaryVariable.getMimeType();
		
		return super.getMimeType();
	}

	public void writeTo(OutputStream out) throws IOException {
		
		if(binaryVariable == null)
			return;
		
		InputStream is = variablesHandler.getBinaryVariablesHandler().getBinaryVariableContent(binaryVariable);
		
		BufferedInputStream fis = new BufferedInputStream(is);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		byte buffer[] = new byte[1024];
		int noRead = 0;
		noRead = fis.read(buffer, 0, 1024);
		//Write out the stream to the file
		while (noRead != -1) {
			baos.write(buffer, 0, noRead);
			noRead = fis.read(buffer, 0, 1024);
		}
		
		baos.writeTo(out);
		baos.flush();
		baos.close();
		fis.close();
	}
	
	protected BinaryVariable getBinVar(VariablesHandler variablesHandler, long taskInstanceId, int binaryVariableHash) {
		
		List<BinaryVariable> variables = variablesHandler.resolveBinaryVariables(taskInstanceId);
		
		for (BinaryVariable binaryVariable : variables) {

			if(binaryVariable.getHash().equals(binaryVariableHash)) {
				
				return binaryVariable;
			}
		}
		
		return null;
	}
	
	public VariablesHandler getVariablesHandler(ServletContext ctx) {
		
		return (VariablesHandler)SpringBeanLookup.getInstance().getSpringBean(ctx, "bpmVariablesHandler");
	}
}