package com.idega.jbpm.variables.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.webdav.lib.WebdavResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.file.util.FileInfo;
import com.idega.core.file.util.FileURIHandler;
import com.idega.core.file.util.FileURIHandlerFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.utils.JSONUtil;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.slide.business.IWSlideService;
import com.idega.slide.util.WebdavExtendedResource;
import com.idega.util.CoreConstants;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.27 $ Last modified: $Date: 2009/06/11 12:35:59 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class BinaryVariablesHandlerImpl implements BinaryVariablesHandler {
	
	public static final String BPM_UPLOADED_FILES_PATH = JBPMConstants.BPM_PATH
	        + "/uploadedFiles/";
	public static final String STORAGE_TYPE = "slide";
	public static final String BINARY_VARIABLE = "binaryVariable";
	public static final String VARIABLE = "variable";
	
	@Autowired
	private FileURIHandlerFactory fileURIHandlerFactory;
	
	/**
	 * stores binary file if needed, converts to binary variable json format, and puts to variables
	 * return variables ready map to be stored to process variables map
	 */
	public Map<String, Object> storeBinaryVariables(long taskInstanceId,
	        Map<String, Object> variables) {
		
		HashMap<String, Object> newVars = new HashMap<String, Object>(variables);
		
		for (Entry<String, Object> entry : newVars.entrySet()) {
			
			Object val = entry.getValue();
			
			if (val == null)
				continue;
			
			String key = entry.getKey();
			
			Variable variable = Variable.parseDefaultStringRepresentation(key);
			
			JSONUtil json = getBinVarJSONConverter();
			
			if (variable.getDataType() == VariableDataType.FILE
			        || variable.getDataType() == VariableDataType.FILES) {
				
				@SuppressWarnings("unchecked")
				Collection<BinaryVariable> binVars = (Collection<BinaryVariable>) val;
				ArrayList<String> binaryVariables = new ArrayList<String>(
				        binVars.size());
				
				for (BinaryVariable binVar : binVars) {
					
					binVar.setTaskInstanceId(taskInstanceId);
					binVar.setVariable(variable);
					
					if (!binVar.isPersisted()) {
						
						binVar.persist();
					}
					
					binaryVariables.add(json.convertToJSON(binVar));
				}
				
				String binVarArrayJSON = json.convertToJSON(binaryVariables);
				entry.setValue(binVarArrayJSON);
			}
		}
		
		return newVars;
	}
	
	public static void main(String[] args) {
		Variable var = new Variable("testVar", VariableDataType.FILES);
		BinaryVariable binVar = new BinaryVariableImpl();
		binVar.setVariable(var);
		
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(BINARY_VARIABLE, BinaryVariableImpl.class);
		xstream.alias(VARIABLE, Variable.class);
		String jsonStr = xstream.toXML(binVar);
		System.out.println(jsonStr);
	}
	
	protected String getDataName(String mapping) {
		
		String strRepr = mapping.contains(CoreConstants.UNDER) ? mapping
		        .substring(mapping.indexOf(CoreConstants.UNDER) + 1) : "";
		return strRepr;
	}
	
	private String concPF(String path, String fileName) {
		return path + CoreConstants.SLASH + fileName;
	}
	
	public void persistBinaryVariable(BinaryVariable binaryVariable,
	        final URI fileUri) {
		
		String path = BPM_UPLOADED_FILES_PATH
		        + binaryVariable.getTaskInstanceId() + "/files";
		
		final FileURIHandler fileURIHandler = getFileURIHandlerFactory()
		        .getHandler(fileUri);
		
		final FileInfo fileInfo = fileURIHandler.getFileInfo(fileUri);
		String fileName = fileInfo.getFileName();
		
		try {
			IWSlideService slideService = getIWSlideService();
			
			UsernamePasswordCredentials credentials = slideService
			        .getRootUserCredentials();
			WebdavExtendedResource res = slideService
			        .getWebdavExtendedResource(concPF(path, fileName),
			            credentials);
			
			if (res.exists()) {
				
				boolean success = false;
				int i = 0;
				
				while (!success) {
					
					String p = path + (i++);
					res = slideService.getWebdavExtendedResource(concPF(p,
					    fileName), credentials);
					
					if (!res.exists()) {
						path = p;
						success = true;
					}
				}
			}
			
			// This should be changed, temporal fix. user will not be able submit a form if
			// exception will be thrown here
			for (int i = 0; i < 5; i++) {
				try {
					InputStream is = fileURIHandler.getFile(fileUri);
					slideService.uploadFileAndCreateFoldersFromStringAsRoot(
					    path + CoreConstants.SLASH, fileName, is, null, false);
					
				} catch (Exception e) {
					if (i < 4) {
						Thread.sleep(500);
						continue;
					}
					
					throw e;
				}
				break;
			}
			
			binaryVariable.setFileName(fileName);
			binaryVariable.setIdentifier(concPF(path, fileName));
			binaryVariable.setStorageType(STORAGE_TYPE);
			binaryVariable.setContentLength(fileInfo.getContentLength());
			
			if (binaryVariable.getDescription() == null)
				binaryVariable.setDescription(fileName);
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
			    "Exception while storing binary variable. Path: " + path, e);
			throw new RuntimeException(e);
		}
	}
	
	protected String convertToJSON(BinaryVariable binVar) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(BINARY_VARIABLE, BinaryVariableImpl.class);
		xstream.alias(VARIABLE, Variable.class);
		String jsonStr = xstream.toXML(binVar);
		return jsonStr;
	}
	
	protected BinaryVariable convertToBinaryVariable(String jsonStr) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(BINARY_VARIABLE, BinaryVariableImpl.class);
		xstream.alias(VARIABLE, Variable.class);
		BinaryVariable binVar = (BinaryVariable) xstream.fromXML(jsonStr);
		return binVar;
	}
	
	public Map<String, Object> resolveBinaryVariables(
	        Map<String, Object> variables) {
		
		// TODO: shouldn't even be needed
		return null;
	}
	
	public List<BinaryVariable> resolveBinaryVariablesAsList(
	        Map<String, Object> variables) {
		
		ArrayList<BinaryVariable> binaryVars = new ArrayList<BinaryVariable>();
		
		for (Entry<String, Object> entry : variables.entrySet()) {
			
			Object val = entry.getValue();
			
			if (val == null)
				continue;
			
			Variable variable = Variable.parseDefaultStringRepresentation(entry
			        .getKey());
			
			if (variable.getDataType() == VariableDataType.FILE
			        || variable.getDataType() == VariableDataType.FILES) {
				
				JSONUtil json = getBinVarJSONConverter();
				
				Collection<String> binVarsInJSON;
				if (val instanceof String) {
					binVarsInJSON = json.convertToObject(String.valueOf(val));
				} else {
					@SuppressWarnings("unchecked")
					Collection<String> f = (Collection<String>) val;
					binVarsInJSON = f;
				}
				
				for (String binVarJSON : binVarsInJSON) {
					
					try {
						
						BinaryVariable binaryVariable = (BinaryVariable) json
						        .convertToObject(binVarJSON);
						
						if (binaryVariable != null)
							binaryVars.add(binaryVariable);
						else {
							
							Logger.getLogger(getClass().getName()).log(
							    Level.WARNING,
							    "Null returned from json.convertToObject by json="
							            + binVarJSON
							            + ". All json expression = "
							            + binVarsInJSON);
						}
						
					} catch (StreamException e) {
						Logger.getLogger(getClass().getName()).log(
						    Level.WARNING,
						    "Exception while parsing binary variable json="
						            + binVarJSON);
					}
				}
			}
		}
		
		return binaryVars;
	}
	
	public InputStream getBinaryVariableContent(BinaryVariable variable) {
		
		if (!STORAGE_TYPE.equals(variable.getStorageType()))
			throw new IllegalArgumentException(
			        "Unsupported binary variable storage type: "
			                + variable.getStorageType());
		
		try {
			
			IWSlideService slideService = getIWSlideService();
			
			UsernamePasswordCredentials credentials = slideService
			        .getRootUserCredentials();
			WebdavExtendedResource res = slideService
			        .getWebdavExtendedResource(variable.getIdentifier(),
			            credentials);
			
			if (!res.exists()) {
				res = slideService.getWebdavExtendedResource(URLDecoder.decode(variable.getIdentifier(), CoreConstants.ENCODING_UTF8), credentials);
				
				if (!res.exists()) {
					Logger.getLogger(getClass().getName()).log(
					    Level.WARNING,
					    "No webdav resource found for path provided: "
					            + variable.getIdentifier());
					return null;
				}
			}
			
			return res.getMethodData();
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "Exception while resolving binary variable. Path: "
			            + variable.getIdentifier(), e);
		}
		
		return null;
	}
	
	public Object getBinaryVariablePersistentResource(BinaryVariable variable) {
		
		if (!STORAGE_TYPE.equals(variable.getStorageType()))
			throw new IllegalArgumentException(
			        "Unsupported binary variable storage type: "
			                + variable.getStorageType());
		
		try {
			IWSlideService slideService = getIWSlideService();
			
			WebdavResource res = slideService
			        .getWebdavResourceAuthenticatedAsRoot(variable
			                .getIdentifier());
			
			if (!res.exists()) {
				
				Logger.getLogger(getClass().getName()).log(
				    Level.WARNING,
				    "No webdav resource found for path provided: "
				            + variable.getIdentifier());
				return null;
			}
			
			return res;
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "Exception while resolving binary variable. Path: "
			            + variable.getIdentifier(), e);
		}
		
		return null;
	}
	
	protected IWSlideService getIWSlideService() {
		
		try {
			return (IWSlideService) IBOLookup.getServiceInstance(
			    IWMainApplication.getDefaultIWApplicationContext(),
			    IWSlideService.class);
		} catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	public FileURIHandlerFactory getFileURIHandlerFactory() {
		return fileURIHandlerFactory;
	}
	
	public void setFileURIHandlerFactory(
	        FileURIHandlerFactory fileURIHandlerFactory) {
		this.fileURIHandlerFactory = fileURIHandlerFactory;
	}
	
	private JSONUtil getBinVarJSONConverter() {
		HashMap<String, Class<?>> binVarAliasMap = new HashMap<String, Class<?>>(
		        2);
		binVarAliasMap.put(BINARY_VARIABLE, BinaryVariableImpl.class);
		binVarAliasMap.put(VARIABLE, Variable.class);
		
		JSONUtil json = new JSONUtil(binVarAliasMap);
		return json;
	}
}