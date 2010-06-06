package com.idega.jbpm.artifacts.presentation;

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
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2009/07/14 16:29:09 $ by $Author: valdas $
 */
public class AttachmentWriter extends DownloadWriter implements MediaWritable {

	private static final Logger LOGGER = Logger.getLogger(AttachmentWriter.class.getName());
	
	public static final String PARAMETER_TASK_INSTANCE_ID = ProcessConstants.TASK_INSTANCE_ID;
	public static final String PARAMETER_VARIABLE_HASH = "varHash";
	
	protected BinaryVariable binaryVariable;
	
	@Autowired
	private VariablesHandler variablesHandler;

	public AttachmentWriter() {
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		
		binaryVariable = resolveBinaryVariable(iwc);
		
		if(binaryVariable == null) {
			
			LOGGER.log(Level.SEVERE, "Failed to resolve binary variable");
			return;
		}
		
		setAsDownload(iwc, binaryVariable.getFileName(), binaryVariable.getContentLength() == null ? 0 : binaryVariable.getContentLength().intValue(),
				binaryVariable.getHash());
	}
	
	protected BinaryVariable resolveBinaryVariable(IWContext iwc) {
		
		String taskInstanceIdSR = iwc.getParameter(PARAMETER_TASK_INSTANCE_ID);
		String variableHashSR = iwc.getParameter(PARAMETER_VARIABLE_HASH);
		
		if(taskInstanceIdSR == null || variableHashSR == null) {
			
			LOGGER.log(Level.SEVERE, "Tried to download, but params not provided.\nTaskInstanceId: "+taskInstanceIdSR+", variableHash: "+variableHashSR);
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
		
		FileUtil.streamToOutputStream(is, out);
		
		out.flush();
		IOUtil.closeInputStream(is);
		IOUtil.closeOutputStream(out);
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