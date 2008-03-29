package com.idega.jbpm.exe;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.jbpm.exe.impl.BinaryVariable;
import com.idega.presentation.IWContext;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/29 20:28:24 $ by $Author: civilis $
 */
public class AttachmentWriter extends DownloadWriter implements MediaWritable {

	private BinaryVariable binaryVariable;

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
		
		binaryVariable = getBinVar(taskInstanceId, variableHash);
		
		if(binaryVariable == null) {
			
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to resolve binary variable for: \nTaskInstanceId: "+taskInstanceIdSR+", variableHash: "+variableHashSR);
			return;
		}
		setAsDownload(iwc, binaryVariable.getFileName(), binaryVariable.getContentLength().intValue());
	}

	public String getMimeType() {
		
		if(binaryVariable != null && binaryVariable.getMimeType() != null)
			return binaryVariable.getMimeType();
		
		return super.getMimeType();
	}

	public void writeTo(OutputStream out) throws IOException {
		
		if(binaryVariable == null)
			return;
		
		InputStream is = getVariablesHandler().getBinaryVariablesHandler().getBinaryVariableContent(binaryVariable);
		BufferedInputStream fis = new BufferedInputStream(is);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (fis.available() > 0) {
			baos.write(fis.read());
		}
		baos.writeTo(out);
		baos.flush();
		baos.close();
		fis.close();
	}
	
	protected BinaryVariable getBinVar(long taskInstanceId, int binaryVariableHash) {
		
		VariablesHandler variablesHandler = getVariablesHandler();
		
		List<BinaryVariable> variables = variablesHandler.resolveBinaryVariables(taskInstanceId);
		
		for (BinaryVariable binaryVariable : variables) {

			if(binaryVariable.getHash().equals(binaryVariableHash)) {
				
				return binaryVariable;
			}
		}
		
		return null;
	}
	
	public VariablesHandler getVariablesHandler() {
		
		return (VariablesHandler)WFUtil.getBeanInstance("bpmVariablesHandler");
	}
}