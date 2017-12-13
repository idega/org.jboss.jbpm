package com.idega.jbpm.variables;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.idega.jbpm.utils.JSONUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $ Last modified: $Date: 2009/03/30 13:14:48 $ by $Author: civilis $
 */
public interface BinaryVariablesHandler {

	/**
	 * checks variables for binary variables types (e.g. file and files data type), stores them to
	 * some persistence (e.g. repository), puts the identifiers in the variables values, instead of File
	 * object(s)
	 *
	 * @param identifier
	 *            - any identifier, usually task instance id
	 * @param variables
	 * @return new map with binary variables values changed to string identifier(s)
	 */
	public abstract Map<String, Object> storeBinaryVariables(long taskInstanceId, Map<String, Object> variables);

	public abstract InputStream getBinaryVariableContent(BinaryVariable variable);

	public abstract Map<String, Object> resolveBinaryVariables(Map<String, Object> variables);

	public abstract List<BinaryVariable> resolveBinaryVariablesAsList(Map<String, Object> variables);
	public abstract List<BinaryVariable> resolveBinaryVariablesAsList(Long tiId, Map<String, Object> variables);

	public abstract void persistBinaryVariable(BinaryVariable binaryVariable, final URI fileUri);
	public void persistBinaryVariable(BinaryVariable binaryVariable, String path, String fileName, Long contentLength, InputStream stream,
			boolean overwrite);

	public String getFolderForBinaryVariable(Long taskInstanceId);

	/**
	 * @param variable
	 * @return resource for binaryVariable, which reflects the actual persistence method. The one
	 *         used now is WebdavExtendedResource. TODO: we should either use here the standard
	 *         filesystem resource api, or create our own
	 */
	public abstract Object getBinaryVariablePersistentResource(BinaryVariable variable);

	public BinaryVariable convertToBinaryVariable(String jsonStr);

	public List<String> convertToBinaryVariablesRepresentation(String jsonStr);

	public JSONUtil getBinVarJSONConverter();

}