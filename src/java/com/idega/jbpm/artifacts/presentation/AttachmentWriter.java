package com.idega.jbpm.artifacts.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.file.util.MimeTypeUtil;
import com.idega.io.DownloadWriter;
import com.idega.io.MediaWritable;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.StringUtil;
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

	@Autowired
	private BPMUserFactory bpmUserFactory;

	private BPMUserFactory getBPMUserFactory() {
		if (bpmUserFactory == null)
			ELUtil.getInstance().autowire(this);
		return bpmUserFactory;
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		if (iwc.isLoggedOn()) {
			resolveBinaryVariable(iwc);
		} else {
			BPMUser bpmUser = getBPMUserFactory().getCurrentBPMUser();
			if (bpmUser == null)
				LOGGER.warning("User is not logged in! Also, BPM user can not be determined");
			else
				resolveBinaryVariable(iwc);
		}

		if (binaryVariable == null) {
			LOGGER.log(Level.SEVERE, "Failed to resolve binary variable. Probably user can not be identified");
			return;
		}

		Long fileSize = binaryVariable.getContentLength();
		int size = fileSize == null ? 0 : fileSize.intValue();
		setAsDownload(iwc, binaryVariable.getFileName(), size, binaryVariable.getHash());
	}

	protected BinaryVariable resolveBinaryVariable(IWContext iwc) {
		String taskInstanceIdSR = iwc.getParameter(PARAMETER_TASK_INSTANCE_ID);
		String variableHashSR = iwc.getParameter(PARAMETER_VARIABLE_HASH);

		if (taskInstanceIdSR == null || variableHashSR == null) {
			LOGGER.warning("Tried to download, but params not provided. TaskInstanceId: "+taskInstanceIdSR+", variableHash: "+variableHashSR);
			return null;
		}

		Serializable taskInstanceId = taskInstanceIdSR;
		Integer variableHash = new Integer(variableHashSR);

		VariablesHandler variablesHandler = getVariablesHandler();

		binaryVariable = getBinVar(variablesHandler, taskInstanceId, variableHash);
		return binaryVariable;
	}

	@Override
	public String getMimeType() {
		if (binaryVariable == null) {
			LOGGER.warning("Variable is not resolved! Can not determine MIME type");
			return super.getMimeType();
		}

		String mimeType = binaryVariable.getMimeType();
		if (!StringUtil.isEmpty(mimeType))
			return mimeType;

		mimeType = MimeTypeUtil.resolveMimeTypeFromFileName(binaryVariable.getFileName());
		return mimeType;
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		if (binaryVariable == null) {
			LOGGER.warning("Binary variable is undefined!");
			return;
		}

		boolean success = Boolean.TRUE;
		InputStream stream = null;
		try {
			stream = getVariablesHandler().getBinaryVariablesHandler().getBinaryVariableContent(binaryVariable);
			FileUtil.streamToOutputStream(stream, out);
		} catch (IOException e) {
			success = Boolean.FALSE;
			IOUtil.closeInputStream(stream);
			LOGGER.log(Level.WARNING, "Error downloading file: " + binaryVariable.getIdentifier(), e);
		}

		if (success) {
			out.flush();
			IOUtil.closeOutputStream(out);
			return;
		}

		setFile(getFileFromRepository(binaryVariable.getIdentifier()));
		super.writeTo(out);
	}

	protected BinaryVariable getBinVar(VariablesHandler variablesHandler, Serializable taskInstanceId, int binaryVariableHash) {
		List<BinaryVariable> variables = variablesHandler.resolveBinaryVariables(taskInstanceId);
		for (BinaryVariable binaryVariable : variables) {
			Integer hash = binaryVariable == null ? null : binaryVariable.getHash();
			if (hash == null) {
				continue;
			}

			if (hash.equals(binaryVariableHash)) {
				return binaryVariable;
			}
		}

		return null;
	}

	public VariablesHandler getVariablesHandler() {
		if (variablesHandler == null)
			ELUtil.getInstance().autowire(this);

		return variablesHandler;
	}
}