package com.idega.jbpm.variables.impl;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpURL;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.WebdavResources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.file.util.FileInfo;
import com.idega.core.file.util.FileURIHandler;
import com.idega.core.file.util.FileURIHandlerFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.utils.JSONUtil;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.slide.business.IWSlideService;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.StringHandler;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.28 $ Last modified: $Date: 2009/06/30 13:57:15 $ by $Author: valdas $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BinaryVariablesHandlerImpl implements BinaryVariablesHandler {
	
	private static final Logger LOGGER = Logger.getLogger(BinaryVariablesHandlerImpl.class.getName());
	
	public static final String BPM_UPLOADED_FILES_PATH = JBPMConstants.BPM_PATH + "/attachments/";
	public static final String STORAGE_TYPE = "slide";
	public static final String BINARY_VARIABLE = "binaryVariable";
	public static final String VARIABLE = "variable";
	
	@Autowired
	private FileURIHandlerFactory fileURIHandlerFactory;
	
	/**
	 * stores binary file if needed, converts to binary variable json format, and puts to variables
	 * return variables ready map to be stored to process variables map
	 */
	public Map<String, Object> storeBinaryVariables(long taskInstanceId, Map<String, Object> variables) {
		Map<String, Object> newVars = new HashMap<String, Object>(variables);
		
		for (Entry<String, Object> entry : newVars.entrySet()) {
			Object val = entry.getValue();
			if (val == null)
				continue;
			
			String key = entry.getKey();
			
			Variable variable = Variable.parseDefaultStringRepresentation(key);
			
			JSONUtil json = getBinVarJSONConverter();
			
			if (variable.getDataType() == VariableDataType.FILE || variable.getDataType() == VariableDataType.FILES) {
				
				@SuppressWarnings("unchecked")
				Collection<BinaryVariable> binVars = (Collection<BinaryVariable>) val;
				List<String> binaryVariables = new ArrayList<String>(binVars.size());
				
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
		String strRepr = mapping.contains(CoreConstants.UNDER) ? mapping.substring(mapping.indexOf(CoreConstants.UNDER) + 1) : CoreConstants.EMPTY;
		return strRepr;
	}
	
	private String concPF(String path, String fileName) {
		return path.concat(CoreConstants.SLASH).concat(fileName);
	}
	
	public void persistBinaryVariable(BinaryVariable binaryVariable, final URI fileUri) {
		String date = IWTimestamp.RightNow().getDateString(IWTimestamp.DATE_PATTERN);
		String path = BPM_UPLOADED_FILES_PATH.concat(date).concat(CoreConstants.SLASH).concat(String.valueOf(binaryVariable.getTaskInstanceId())).concat("/files");
		
		final FileURIHandler fileURIHandler = getFileURIHandlerFactory().getHandler(fileUri);
		
		final FileInfo fileInfo = fileURIHandler.getFileInfo(fileUri);
		String fileName = fileInfo.getFileName();
		
		try {
			IWSlideService slideService = getIWSlideService();
			
			int index = 0;
			String tmpUri = path;
			while (slideService.getExistence(concPF(tmpUri, fileName))) {	//	File by the same name already exists! Renaming this file not to overwrite existing file
				tmpUri = path.concat(String.valueOf((index++)));
			}
			path = tmpUri;
			
			// This should be changed, temporal fix. user will not be able submit a form if exception will be thrown here
			InputStream stream = null;
			try {
				String uploadPath = path.concat(CoreConstants.SLASH);
				for (int i = 0; i < 5; i++) {
					try {
						stream = fileURIHandler.getFile(fileUri);
						if (!slideService.uploadFile(uploadPath, fileName, null, stream)) {
							throw new RuntimeException("Unable to upload file to " + uploadPath.concat(fileName));
						}
					} catch (Exception e) {
						if (i < 4) {
							Thread.sleep(500);
							continue;
						}
						throw e;
					}
					break;
				}
			} finally {
				IOUtil.close(stream);
			}
			
			binaryVariable.setFileName(fileName);
			binaryVariable.setIdentifier(concPF(path, fileName));
			binaryVariable.setStorageType(STORAGE_TYPE);
			binaryVariable.setContentLength(fileInfo.getContentLength());
			
			if (binaryVariable.getDescription() == null)
				binaryVariable.setDescription(fileName);
		} catch (Exception e) {
			String message = "Exception while storing binary variable. Path: " + path;
			LOGGER.log(Level.SEVERE, message, e);
			throw new RuntimeException(message, e);
		}
	}
	
	protected String convertToJSON(BinaryVariable binVar) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(BINARY_VARIABLE, BinaryVariableImpl.class);
		xstream.alias(VARIABLE, Variable.class);
		String jsonStr = xstream.toXML(binVar);
		return jsonStr;
	}
	
	public List<String> convertToBinaryVariablesRepresentation(String jsonStr) {
		return getBinVarJSONConverter().convertToObject(jsonStr);
	}
	
	public BinaryVariable convertToBinaryVariable(String jsonStr) {
		return getBinVarJSONConverter().convertToObject(jsonStr);
	}
	
	public Map<String, Object> resolveBinaryVariables(Map<String, Object> variables) {
		
		// TODO: shouldn't even be needed
		return null;
	}
	
	public List<BinaryVariable> resolveBinaryVariablesAsList(Map<String, Object> variables) {
		List<BinaryVariable> binaryVars = new ArrayList<BinaryVariable>();
		
		for (Entry<String, Object> entry : variables.entrySet()) {
			Object val = entry.getValue();
			
			if (val == null)
				continue;
			
			Variable variable = Variable.parseDefaultStringRepresentation(entry.getKey());
			if (variable.getDataType() == VariableDataType.FILE || variable.getDataType() == VariableDataType.FILES) {
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
						BinaryVariable binaryVariable = (BinaryVariable) json.convertToObject(binVarJSON);
						
						if (binaryVariable != null)
							binaryVars.add(binaryVariable);
						else {
							LOGGER.log(Level.WARNING, "Null returned from json.convertToObject by json="+ binVarJSON+ ". All json expression = "+ binVarsInJSON);
						}
						
					} catch (StreamException e) {
						LOGGER.log(Level.WARNING, "Exception while parsing binary variable json=" + binVarJSON);
					}
				}
			}
		}
		
		return binaryVars;
	}
	
	private String getDecodedUri(String uri) {
		if (uri == null) {
			return null;
		}
		
		try {
			return URLDecoder.decode(uri, CoreConstants.ENCODING_UTF8);
		} catch (UnsupportedEncodingException e) {
			LOGGER.log(Level.WARNING, "Error while decoding: " + uri, e);
		}
		
		return null;
	}
	
	public InputStream getBinaryVariableContent(BinaryVariable variable) {
		if (!STORAGE_TYPE.equals(variable.getStorageType()))
			throw new IllegalArgumentException("Unsupported binary variable storage type: " + variable.getStorageType());
		
		try {
			IWSlideService slideService = getIWSlideService();
			
			String fileUri = variable.getIdentifier();
			InputStream stream = slideService.getInputStream(fileUri);
			if (stream == null) {
				String decodedFileUri = getDecodedUri(fileUri);
				stream = slideService.getInputStream(decodedFileUri);
				if (stream == null) {
					LOGGER.warning("Input stream can not be opened to: " + fileUri + " nor to decoded path: " + decodedFileUri +
							". Will try to retrieve WebdavResource");
					
					WebdavResource res = null;
					try {
						res = slideService.getWebdavExtendedResource(fileUri, slideService.getRootUserCredentials(), false);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error getting resource via HTTP: " + fileUri, e);
					}
					if (res == null || !res.exists()) {
						LOGGER.warning("Unable to load WebdavResource '" + fileUri + "' using Slide via HTTP");
					} else {
						stream = slideService.getInputStream(res);
					}
				}
			}
			if (stream == null) {
				WebdavResource attachment = getResource(variable, slideService);
				if (attachment == null) {
					LOGGER.warning("Unable to get persistent object for " + fileUri);
				} else {
					LOGGER.info("Resolved persistent object for resource: " + fileUri);
					try {
						stream = attachment.getMethodData();
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Error getting input stream for: " + attachment.getPath(), e);
					}
					if (stream == null) {
						try {
							stream = slideService.getInputStream(attachment.getPath());
						} catch (Exception e) {
							LOGGER.log(Level.WARNING, "Error getting input stream for: " + attachment.getPath(), e);
						}
					}
				}
			}
			if (stream == null)
				stream = getFromURL(slideService, fileUri);
			
			if (stream == null)
				LOGGER.severe("Unable to get input stream for resource: " + fileUri);
			return stream;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while resolving binary variable. Path: " + variable.getIdentifier(), e);
			return null;
		}
	}
	
	private InputStream getFromURL(IWSlideService slideService, String fileUri) {
		String link = null;
		try {
			HttpURL serverInfo = slideService.getWebdavServerURL();
			String scheme = serverInfo.getScheme();
			String host = serverInfo.getHost();
			int port = serverInfo.getPort();
			User admin = IWMainApplication.getDefaultIWMainApplication().getAccessController().getAdministratorUser();
			String query = LoginBusinessBean.PARAM_LOGIN_BY_UNIQUE_ID.concat("=").concat(admin.getUniqueId());
			query = query.concat("&").concat(LoginBusinessBean.LoginStateParameter).concat("=").concat(LoginBusinessBean.LOGIN_EVENT_LOGIN);
			URI uri = new URI(scheme, null, host, port, CoreConstants.WEBDAV_SERVLET_URI.concat(fileUri), query, null);
			link = uri.toASCIIString();
			URL url = new URL(link);
			return url.openStream();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting input stream via URL: " + link, e);
		}
		return null;
	}
	
	private WebdavResource getResource(BinaryVariable variable, IWSlideService slideService) {
		LOGGER.warning("No webdav resource found for path provided: " + variable.getIdentifier() + ". Will try to remove non-Latin letters to resolve the resource");
		
		WebdavResource res = null;
		try {
			char[] exceptions = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '/'};
			String fileNameInEnglish = StringHandler.stripNonRomanCharacters(variable.getFileName(), exceptions);
			String folder = variable.getIdentifier().substring(0, variable.getIdentifier().lastIndexOf(CoreConstants.SLASH));
			WebdavResource attachmentFolder = slideService.getWebdavResourceAuthenticatedAsRoot(folder);
			if (attachmentFolder == null || !attachmentFolder.exists()) {
				LOGGER.warning("Folder '" + folder + "' does not exist!");
				return null;
			}
			WebdavResources files = attachmentFolder.getChildResources();
			if (files == null) {
				LOGGER.warning("No files found in the folder: " + folder);
				return null;
			}
			Enumeration<?> attachments = files.getResources();
			if (attachments == null || !attachments.hasMoreElements()) {
				LOGGER.warning("No files found in the folder: " + folder);
				return null;
			}
			
			boolean resourceFound = false;
			List<?> attachmentsInFolder = Collections.list(attachments);
			for (Iterator<?> attachmentsIter = attachmentsInFolder.iterator(); (attachmentsIter.hasNext() && !resourceFound);) {
				Object tmp = attachmentsIter.next();
				if (!(tmp instanceof WebdavResource))
					continue;
				
				res = (WebdavResource) tmp;
				String attachmentPath = StringHandler.stripNonRomanCharacters(res.getPath(), exceptions);
				resourceFound = attachmentPath.endsWith(fileNameInEnglish) || attachmentsInFolder.size() == 1;
				if (!resourceFound) {
					String[] fileParts = fileNameInEnglish.split(CoreConstants.MINUS);
					attachmentPath = attachmentPath.substring(attachmentPath.lastIndexOf(CoreConstants.SLASH) + 1);
					String[] pathParts = attachmentPath.split(CoreConstants.MINUS);
					if (ArrayUtil.isEmpty(fileParts) || ArrayUtil.isEmpty(pathParts) || fileParts.length != pathParts.length)
						continue;
					
					int sameParts = 0;
					for (int i = 0; i < fileParts.length; i++) {
						if (fileParts[i].contains(pathParts[i]) || pathParts[i].contains(fileParts[i]))
							sameParts++;
					}
					resourceFound = sameParts == fileParts.length;
				}
			}
			if (!resourceFound) {
				LOGGER.warning("Unable to find resource '" + variable.getIdentifier() + "' in the folder: " + attachmentsInFolder);
				res = null;
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unable to resolve resource for: " + variable.getIdentifier(), e);
		}
		
		return res;
	}
	
	public Object getBinaryVariablePersistentResource(BinaryVariable variable) {
		if (!STORAGE_TYPE.equals(variable.getStorageType()))
			throw new IllegalArgumentException("Unsupported binary variable storage type: "+ variable.getStorageType());
		
		try {
			IWSlideService slideService = getIWSlideService();
			
			WebdavResource res = null;
			try {
				res = slideService.getWebdavResourceAuthenticatedAsRoot(variable.getIdentifier());
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Unable to get persistent object for resource: " + variable.getIdentifier(), e);
			}
			if (res != null && res.exists())
				return res;
			
			res = getResource(variable, slideService);
			
			return res;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while resolving binary variable. Path: " + variable.getIdentifier(), e);
		}
		
		return null;
	}
	
	protected IWSlideService getIWSlideService() {
		try {
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
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
		Map<String, Class<?>> binVarAliasMap = new HashMap<String, Class<?>>(2);
		binVarAliasMap.put(BINARY_VARIABLE, BinaryVariableImpl.class);
		binVarAliasMap.put(VARIABLE, Variable.class);
		
		JSONUtil json = new JSONUtil(binVarAliasMap);
		return json;
	}
}