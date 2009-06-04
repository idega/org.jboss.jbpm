package com.idega.jbpm.artifacts.presentation;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2009/06/04 12:30:52 $ by $Author: valdas $
 */
public class AttachmentWriter extends DownloadWriter implements MediaWritable {

	protected BinaryVariable binaryVariable;
	@Autowired
	private VariablesHandler variablesHandler;

	public AttachmentWriter() {
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		
		binaryVariable = resolveBinaryVariable(iwc);
		
		if(binaryVariable == null) {
			
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to resolve binary variable");
			return;
		}
		
		setAsDownload(iwc, binaryVariable.getFileName(), binaryVariable.getContentLength() == null ? 0 : binaryVariable.getContentLength().intValue(),
				binaryVariable.getHash());
	}
	
	protected BinaryVariable resolveBinaryVariable(IWContext iwc) {
		
		String taskInstanceIdSR = iwc.getParameter("taskInstanceId");
		String variableHashSR = iwc.getParameter("varHash");
		
		if(taskInstanceIdSR == null || variableHashSR == null) {
			
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Tried to download, but params not provided.\nTaskInstanceId: "+taskInstanceIdSR+
					", variableHash: "+variableHashSR);
			return null;
		}
		
		Long taskInstanceId = new Long(taskInstanceIdSR);
		Integer variableHash = new Integer(variableHashSR);
		
		VariablesHandler variablesHandler = getVariablesHandler();
		
		binaryVariable = getBinVar(variablesHandler, taskInstanceId, variableHash);
		return binaryVariable;
	}

	@Override
	public String getMimeType() {
		
		if(binaryVariable != null && binaryVariable.getMimeType() != null)
			return binaryVariable.getMimeType();
		
		return super.getMimeType();
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		
		if(binaryVariable == null)
			return;
		
		InputStream is = getVariablesHandler().getBinaryVariablesHandler().getBinaryVariableContent(binaryVariable);
		
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
	
	public VariablesHandler getVariablesHandler() {
		
		if(variablesHandler == null)
			ELUtil.getInstance().autowire(this);
		
		return variablesHandler;
	}
}