package com.idega.jbpm.variables.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.httpclient.HttpURL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.variables.Variable;
import com.idega.block.process.variables.VariableDataType;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.file.util.FileInfo;
import com.idega.core.file.util.FileURIHandler;
import com.idega.core.file.util.FileURIHandlerFactory;
import com.idega.core.persistence.Param;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.utils.JSONUtil;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.repository.RepositoryService;
import com.idega.repository.bean.RepositoryItem;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.28 $ Last modified: $Date: 2009/06/30 13:57:15 $ by $Author: valdas $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BinaryVariablesHandlerImpl extends DefaultSpringBean implements BinaryVariablesHandler {

	public static final String BPM_UPLOADED_FILES_PATH = JBPMConstants.BPM_PATH + "/attachments/";
	public static final String STORAGE_TYPE = CoreConstants.REPOSITORY;
	public static final String BINARY_VARIABLE = "binaryVariable";
	public static final String VARIABLE = "variable";

	@Autowired
	private FileURIHandlerFactory fileURIHandlerFactory;

	@Autowired
	private BPMFactory bpmFactory;

	/**
	 * stores binary file if needed, converts to binary variable json format, and puts to variables
	 * return variables ready map to be stored to process variables map
	 */
	@Override
	public Map<String, Object> storeBinaryVariables(long taskInstanceId, Map<String, Object> variables) {
		Map<String, Object> newVars = new HashMap<>(variables);

		JSONUtil json = getBinVarJSONConverter();
		for (Entry<String, Object> entry: newVars.entrySet()) {
			Object val = entry.getValue();
			if (val == null) {
				continue;
			}

			String key = entry.getKey();
			Variable variable = Variable.parseDefaultStringRepresentation(key);
			if (variable.getDataType() == VariableDataType.FILE || variable.getDataType() == VariableDataType.FILES) {
				if (val instanceof Collection<?>) {
					@SuppressWarnings("unchecked")
					Collection<BinaryVariable> binVars = (Collection<BinaryVariable>) val;
					List<String> binaryVariables = new ArrayList<>(binVars.size());

					for (BinaryVariable binVar: binVars) {
						binVar.setTaskInstanceId(taskInstanceId);
						binVar.setVariable(variable);

						if (!binVar.isPersisted()) {
							binVar.persist();
						}

						String jsonString = json.convertToJSON(binVar);
						binaryVariables.add(jsonString);
					}

					String binVarArrayJSON = json.convertToJSON(binaryVariables);
					entry.setValue(binVarArrayJSON);
				} else {
					getLogger().warning("Do not know how to handle value " + val + " (class: " + val.getClass().getName() + "). Task inst. ID: " + taskInstanceId);
				}
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
		if (!path.endsWith(CoreConstants.SLASH)) {
			path = path.concat(CoreConstants.SLASH);
		}
		return path.concat(fileName);
	}

	@Override
	public <T extends Serializable> String getFolderForBinaryVariable(T taskInstanceId) {
		String date = IWTimestamp.RightNow().getDateString(IWTimestamp.DATE_PATTERN);
		return BPM_UPLOADED_FILES_PATH.concat(date).concat(CoreConstants.SLASH).concat(String.valueOf(taskInstanceId)).concat("/files");
	}

	@Override
	public void persistBinaryVariable(BinaryVariable binaryVariable, final URI fileUri) {
		FileURIHandler fileURIHandler = getFileURIHandlerFactory().getHandler(fileUri);

		FileInfo fileInfo = fileURIHandler.getFileInfo(fileUri);
		String fileName = fileInfo.getFileName();

		String path = getFolderForBinaryVariable(binaryVariable.getTaskInstanceId());
		try {
			persistBinaryVariable(binaryVariable, path, fileName, fileInfo.getContentLength(), fileURIHandler.getFile(fileUri), true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void persistBinaryVariable(BinaryVariable binaryVariable, String path, String fileName, Long contentLength, InputStream stream,
			boolean overwrite) {
		try {
			String normalizedName = null;
			RepositoryService repository = getRepositoryService();
			if (overwrite || !repository.getExistence(concPF(path, fileName))) {
				normalizedName = StringHandler.stripNonRomanCharacters(fileName,
						new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_', '.'});
				normalizedName = StringHandler.removeWhiteSpace(normalizedName);

				int index = 0;
				String tmpUri = path;
				//	File by the same name already exists! Renaming this file not to overwrite existing file
				while (repository.getExistence(concPF(tmpUri, normalizedName))) {
					tmpUri = path.concat(String.valueOf((index++)));
				}
				path = tmpUri;

				String uploadPath = path;
				if (!uploadPath.endsWith(CoreConstants.SLASH)) {
					uploadPath = uploadPath.concat(CoreConstants.SLASH);
				}
				try {
					if (!repository.uploadFile(uploadPath, normalizedName, null, stream)) {
						throw new RuntimeException("Unable to upload file to " + uploadPath.concat(normalizedName));
					}
				} catch (Exception e) {
					doSendErrorNotification(stream, normalizedName, uploadPath, e);
					throw e;
				} finally {
					IOUtil.close(stream);
				}
			} else {
				IOUtil.close(stream);
				normalizedName = fileName;
			}

			binaryVariable.setFileName(fileName);
			binaryVariable.setIdentifier(concPF(path, normalizedName));
			binaryVariable.setStorageType(STORAGE_TYPE);
			binaryVariable.setContentLength(contentLength);

			if (binaryVariable.getDescription() == null) {
				binaryVariable.setDescription(fileName);
			}
		} catch (Exception e) {
			String message = "Exception while storing binary variable. Path: " + path;
			getLogger().log(Level.SEVERE, message, e);
			throw new RuntimeException(message, e);
		}
	}

	private void doSendErrorNotification(InputStream stream, String name, String path, Throwable error) {
		try {
			File tmp = new File(name);
			FileUtil.streamToFile(stream, tmp);
			CoreUtil.sendExceptionNotification("Unable to upload file: " + path.concat(name), error, tmp.exists() ? new File[] {tmp} : null);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while sending notification about upload failure to " + path + " of " + name, e);
		} finally {
			IOUtil.close(stream);
		}
	}

	protected String convertToJSON(BinaryVariable binVar) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(BINARY_VARIABLE, BinaryVariableImpl.class);
		xstream.alias(VARIABLE, Variable.class);
		String jsonStr = xstream.toXML(binVar);
		return jsonStr;
	}

	@Override
	public List<String> convertToBinaryVariablesRepresentation(String jsonStr) {
		return getBinVarJSONConverter().convertToObject(jsonStr);
	}

	@Override
	public BinaryVariable convertToBinaryVariable(String jsonStr) {
		return getBinVarJSONConverter().convertToObject(jsonStr);
	}

	@Override
	public Map<String, Object> resolveBinaryVariables(Map<String, Object> variables) {

		// TODO: shouldn't even be needed
		return null;
	}

	@Override
	public List<BinaryVariable> resolveBinaryVariablesAsList(Map<String, Object> variables) {
		return resolveBinaryVariablesAsList(null, variables);
	}

	@Autowired
	private BPMDAO bpmDAO;
	private BPMDAO getBPMDAO() {
		if (bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmDAO;
	}

	@Override
	public List<BinaryVariable> getVariables(String name, Object val, Long tiId) {
		List<BinaryVariable> binaryVars = new ArrayList<>();
		if (StringUtil.isEmpty(name) || val == null) {
			return binaryVars;
		}

		JSONUtil json = getBinVarJSONConverter();

		Collection<Object> binVarsInJSON = null;
		if (val instanceof String) {
			try {
				String jsonValue = (String) val;
				if (jsonValue.startsWith(CoreConstants.CURLY_BRACKET_LEFT)) {
					binVarsInJSON = json.convertToObject(jsonValue);
				} else {
					getLogger().warning("Value '" + val + "' of variable " + name + " is not JSON format, ignorring");
				}
			} catch (Exception e) {
				String message = "Error converting '" + val + "' with " + json.getClass().getName() + " into the object (Collection.class)";
				getLogger().log(Level.WARNING, message, e);
				CoreUtil.sendExceptionNotification(message, e);
			}
		} else {
			@SuppressWarnings("unchecked")
			Collection<Object> f = (Collection<Object>) val;
			binVarsInJSON = f;
		}

		if (ListUtil.isEmpty(binVarsInJSON)) {
			return binaryVars;
		}

		for (Object binVarJSON: binVarsInJSON) {
			BinaryVariable binaryVariable = null;
			if (binVarJSON instanceof String) {
				try {
					binaryVariable = (BinaryVariable) json.convertToObject((String) binVarJSON);
				} catch (StreamException e) {
					getLogger().log(Level.WARNING, "Exception while parsing binary variable json=" + binVarJSON);
				}
			} else if (binVarJSON instanceof BinaryVariable) {
				binaryVariable = (BinaryVariable) binVarJSON;
			}

			if (binaryVariable != null) {
				if (tiId != null) {
					if (binaryVariable.getTaskInstanceId() == tiId.longValue()) {
						binaryVars.add(binaryVariable);
					}
				} else {
					binaryVars.add(binaryVariable);
				}
			} else {
				getLogger().log(Level.WARNING, "Null returned from json.convertToObject by json="+ binVarJSON+ ". All json expression = "+ binVarsInJSON);
			}
		}

		return binaryVars;
	}

	@Override
	public List<BinaryVariable> resolveBinaryVariablesAsList(Serializable taskInstanceId, Map<String, Object> variables) {
		List<BinaryVariable> binaryVars = new ArrayList<>();

		if (taskInstanceId instanceof Long || StringHandler.isNumeric(taskInstanceId.toString())) {
			Long tiId = Long.valueOf(taskInstanceId.toString());
			Map<String, Boolean> addedVars = new HashMap<>();

			if (!MapUtil.isEmpty(variables)) {
				for (Entry<String, Object> entry: variables.entrySet()) {
					Object val = entry.getValue();
					if (val == null) {
						continue;
					}

					Variable variable = Variable.parseDefaultStringRepresentation(entry.getKey());
					if (variable.getDataType() == VariableDataType.FILE || variable.getDataType() == VariableDataType.FILES) {
						List<BinaryVariable> tmp = getVariables(entry.getKey(), val, tiId);
						if (!ListUtil.isEmpty(tmp)) {
							addedVars.put(entry.getKey(), Boolean.TRUE);
							binaryVars.addAll(tmp);
						}
					}
				}
			}

			if (getApplication().getSettings().getBoolean("bpm.bin_vars_load_for_task", Boolean.FALSE)) {
				List<com.idega.jbpm.data.Variable> vars = getBPMDAO().getResultListByInlineQuery(
						"select v from " + com.idega.jbpm.data.Variable.class.getName() + " v where v.taskInstance = :tiId and v.classType = 'S' and v.name like 'file%'",
						com.idega.jbpm.data.Variable.class,
						new Param("tiId", tiId)
				);
				if (!ListUtil.isEmpty(vars)) {
					for (com.idega.jbpm.data.Variable var: vars) {
						if (addedVars.containsKey(var.getName())) {
							continue;
						}

						List<BinaryVariable> tmp = getVariables(var.getName(), var.getValue(), tiId);
						if (!ListUtil.isEmpty(tmp)) {
							addedVars.put(var.getName(), Boolean.TRUE);
							binaryVars.addAll(tmp);
						}
					}
				}
			}
		} else if (taskInstanceId != null) {
			TaskInstanceW tiW = bpmFactory.getProcessManagerByTaskInstanceId(taskInstanceId).getTaskInstance(taskInstanceId);
			return tiW.getAttachments();
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
			getLogger().log(Level.WARNING, "Error while decoding: " + uri, e);
		}

		return null;
	}

	@Override
	public InputStream getBinaryVariableContent(BinaryVariable variable) {
		String storageType = variable.getStorageType();
		if (!STORAGE_TYPE.equals(storageType) && !"slide".equals(storageType)) {
			throw new IllegalArgumentException("Unsupported binary variable storage type: " + variable.getStorageType());
		}

		try {
			RepositoryService repository = getRepositoryService();

			String fileUri = variable.getIdentifier();
			InputStream stream = repository.getInputStreamAsRoot(fileUri);
			if (stream == null) {
				String decodedFileUri = getDecodedUri(fileUri);
				stream = repository.getInputStreamAsRoot(decodedFileUri);
				if (stream == null) {
					getLogger().warning("Input stream can not be opened to: " + fileUri + " nor to decoded path: " + decodedFileUri +
							". Will try to retrieve resource via HTTP(S)");

					RepositoryItem res = null;
					try {
						res = repository.getRepositoryItemAsRootUser(fileUri);
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error getting resource via HTTP: " + fileUri, e);
					}
					if (res == null || !res.exists()) {
						getLogger().warning("Unable to load resource '" + fileUri + "' from repository via HTTP");
					} else {
						stream = repository.getInputStreamAsRoot(res.getPath());
					}
				}
			}
			if (stream == null) {
				RepositoryItem attachment = getResource(variable, repository);
				if (attachment == null) {
					getLogger().warning("Unable to get persistent object for " + fileUri);
				} else {
					getLogger().info("Resolved persistent object for resource: " + fileUri);
					try {
						stream = attachment.getInputStream();
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error getting input stream for: " + attachment.getPath(), e);
					}
					if (stream == null) {
						try {
							stream = repository.getInputStreamAsRoot(attachment.getPath());
						} catch (Exception e) {
							getLogger().log(Level.WARNING, "Error getting input stream for: " + attachment.getPath(), e);
						}
					}
				}
			}

			if (stream == null) {
				File tmp = CoreUtil.getFileFromRepository(fileUri);
				if (tmp != null && tmp.exists() && tmp.canRead()) {
					stream = new FileInputStream(tmp);
				} else {
					getLogger().warning("Unable to get file " + fileUri + " from files system. " + tmp == null ?
							"It (" + fileUri + ") does not exist" :
							"It (" + tmp + ") either does not exist (" +!tmp.exists() + " or is not readable (" + !tmp.canRead() + "))");
				}
			}

			if (stream == null) {
				stream = getFromURL(repository, fileUri);
			}

			if (stream == null) {
				getLogger().severe("Unable to get input stream for resource: " + fileUri);
			}
			return stream;
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving binary variable. Path: " + variable.getIdentifier(), e);
			return null;
		}
	}

	private InputStream getFromURL(RepositoryService repository, String fileUri) {
		String link = null;
		try {
			HttpURL serverInfo = new HttpURL(CoreUtil.getHost() + repository.getWebdavServerURL());
			String scheme = serverInfo.getScheme();
			String host = serverInfo.getHost();
			int port = serverInfo.getPort();
			com.idega.user.data.bean.User admin = IWMainApplication.getDefaultIWMainApplication().getAccessController().getAdministratorUser();
			String query = LoginBusinessBean.PARAM_LOGIN_BY_UNIQUE_ID.concat("=").concat(admin.getUniqueId());
			query = query.concat("&").concat(LoginBusinessBean.LoginStateParameter).concat("=").concat(LoginBusinessBean.LOGIN_EVENT_LOGIN);
			URI uri = new URI(scheme, null, host, port, CoreConstants.WEBDAV_SERVLET_URI.concat(fileUri), query, null);
			link = uri.toASCIIString();
			URL url = new URL(link);
			return url.openStream();
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Error getting input stream via URL: " + link, e);
		}
		return null;
	}

	private RepositoryItem getResource(BinaryVariable variable, RepositoryService repository) {
		getLogger().warning("No resource found for path provided: " + variable.getIdentifier() + ". Will try to remove non-Latin letters to resolve the resource");

		RepositoryItem res = null;
		try {
			char[] exceptions = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '/'};
			String fileNameInEnglish = StringHandler.stripNonRomanCharacters(variable.getFileName(), exceptions);
			String folder = variable.getIdentifier().substring(0, variable.getIdentifier().lastIndexOf(CoreConstants.SLASH));
			RepositoryItem attachmentFolder = repository.getRepositoryItemAsRootUser(folder);
			if (attachmentFolder == null || !attachmentFolder.exists()) {
				getLogger().warning("Folder '" + folder + "' does not exist!");
				return null;
			}
			Collection<RepositoryItem> files = attachmentFolder.getChildResources();
			if (ListUtil.isEmpty(files)) {
				getLogger().warning("No files found in the folder: " + folder);
				return null;
			}

			boolean resourceFound = false;
			for (Iterator<RepositoryItem> attachmentsIter = files.iterator(); (attachmentsIter.hasNext() && !resourceFound);) {
				res = attachmentsIter.next();
				String attachmentPath = StringHandler.stripNonRomanCharacters(res.getPath(), exceptions);
				resourceFound = attachmentPath.endsWith(fileNameInEnglish) || files.size() == 1;
				if (!resourceFound) {
					String[] fileParts = fileNameInEnglish.split(CoreConstants.MINUS);
					attachmentPath = attachmentPath.substring(attachmentPath.lastIndexOf(CoreConstants.SLASH) + 1);
					String[] pathParts = attachmentPath.split(CoreConstants.MINUS);
					if (ArrayUtil.isEmpty(fileParts) || ArrayUtil.isEmpty(pathParts) || fileParts.length != pathParts.length) {
						continue;
					}

					int sameParts = 0;
					for (int i = 0; i < fileParts.length; i++) {
						if (fileParts[i].contains(pathParts[i]) || pathParts[i].contains(fileParts[i])) {
							sameParts++;
						}
					}
					resourceFound = sameParts == fileParts.length;
				}
			}
			if (!resourceFound) {
				getLogger().warning("Unable to find resource '" + variable.getIdentifier() + "' in the folder: " + files);
				res = null;
			}
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Unable to resolve resource for: " + variable.getIdentifier(), e);
		}

		return res;
	}

	@Override
	public Object getBinaryVariablePersistentResource(BinaryVariable variable) {
		if (!STORAGE_TYPE.equals(variable.getStorageType())) {
			throw new IllegalArgumentException("Unsupported binary variable storage type: "+ variable.getStorageType());
		}

		try {
			RepositoryService repository = getRepositoryService();

			RepositoryItem res = null;
			try {
				res = repository.getRepositoryItemAsRootUser(variable.getIdentifier());
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Unable to get persistent object for resource: " + variable.getIdentifier(), e);
			}
			if (res != null && res.exists()) {
				return res;
			}

			res = getResource(variable, repository);

			return res;
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while resolving binary variable. Path: " + variable.getIdentifier(), e);
		}

		return null;
	}

	public FileURIHandlerFactory getFileURIHandlerFactory() {
		return fileURIHandlerFactory;
	}

	public void setFileURIHandlerFactory(FileURIHandlerFactory fileURIHandlerFactory) {
		this.fileURIHandlerFactory = fileURIHandlerFactory;
	}

	@Override
	public JSONUtil getBinVarJSONConverter() {
		Map<String, Class<?>> binVarAliasMap = new HashMap<>(2);
		binVarAliasMap.put(BINARY_VARIABLE, BinaryVariableImpl.class);
		binVarAliasMap.put(VARIABLE, Variable.class);

		JSONUtil json = new JSONUtil(binVarAliasMap);
		return json;
	}
}