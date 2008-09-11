package com.idega.jbpm.variables.impl;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.file.data.ExtendedFile;
import com.idega.core.file.util.FileInfo;
import com.idega.core.file.util.FileURIHandler;
import com.idega.core.file.util.FileURIHandlerFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.jbpm.variables.VariableDataType;
import com.idega.slide.business.IWSlideService;
import com.idega.slide.util.WebdavExtendedResource;
import com.idega.util.CoreConstants;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/09/11 18:17:39 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class BinaryVariablesHandlerImpl implements BinaryVariablesHandler {
	
	public static final String BPM_UPLOADED_FILES_PATH = "/files/bpm/uploadedFiles/";
	public static final String STORAGE_TYPE = "slide";
	public static final String BINARY_VARIABLE = "binaryVariable";
	
	private FileURIHandlerFactory fileURIHandlerFactory;
	
	public Map<String, Object> storeBinaryVariables(Object identifier, Map<String, Object> variables) {
		
		HashMap<String, Object> newVars = new HashMap<String, Object>(variables);
		
		for (Entry<String, Object> entry : newVars.entrySet()) {
			
			Object val = entry.getValue();
			
			if(val == null)
				continue;
			
			String key = entry.getKey();
			
			VariableDataType dataType = getDataType(key);
			
			if(dataType == VariableDataType.FILE) {

				if(val instanceof URI) {
					ArrayList<String> binaryVariables = new ArrayList<String>(1);
					BinaryVariable binaryVariable = storeFile((String)identifier, (URI)val);
					binaryVariables.add(convertToJSON(binaryVariable));
					
					entry.setValue(binaryVariables);
					
				} else {
					entry.setValue(null);
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Variable data type resolved: "+dataType+", but value data type didn't match ("+val.getClass().getName()+"), variable name: "+key);
				}
			} else if(dataType == VariableDataType.FILES) {
				
				if(val instanceof Collection) {
					
					@SuppressWarnings("unchecked")
					Collection<Object> files = (Collection<Object>)val;
					
					if(files.isEmpty()) {
						entry.setValue(null);
					} else {
						
						ArrayList<String> binaryVariables = new ArrayList<String>(files.size());
						
						for (Iterator<Object> iterator = files.iterator(); iterator.hasNext();) {
							Object o = iterator.next();
							
							if(o instanceof ExtendedFile) {
								
								ExtendedFile ef = (ExtendedFile)o;
								
								BinaryVariable binaryVariable = storeFile(identifier, ef.getFileUri());
								binaryVariable.setDescription(ef.getFileInfo());
								binaryVariables.add(convertToJSON(binaryVariable));
							} else if(o instanceof URI) {
								
								URI u = (URI)o;
								
								String fName = getFileURIHandlerFactory().getHandler(u).getFileInfo(u).getFileName();
								
								BinaryVariable binaryVariable = storeFile(identifier, u);
								binaryVariable.setDescription(fName);
								binaryVariables.add(convertToJSON(binaryVariable));
							}
						}
						
						entry.setValue(binaryVariables);
						/*
						if(files.iterator().next() instanceof ExtendedFile) {
							
							@SuppressWarnings("unchecked")
							Collection<ExtendedFile> afiles = (Collection<ExtendedFile>)files;
							ArrayList<String> binaryVariables = new ArrayList<String>(afiles.size());

							for (ExtendedFile file : afiles) {
								BinaryVariable binaryVariable = storeFile(identifier, file.getFile());
								binaryVariable.setDescription(file.getFileInfo());
								binaryVariables.add(convertToJSON(binaryVariable));
							}
							
							entry.setValue(binaryVariables);
							//entry.setValue(!binaryVariables.isEmpty() ? binaryVariables.iterator().next() : null);
							
						} else {
							entry.setValue(null);
							Logger.getLogger(getClass().getName()).log(Level.WARNING, "Variable data type resolved: "+dataType+", but value data type didn't match ("+val.getClass().getName()+"), variable name: "+key);
						}
						*/
					}
					
				} else {
					entry.setValue(null);
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Variable data type resolved: "+dataType+", but value data type didn't match ("+val.getClass().getName()+"), variable name: "+key);
				}
			}
		}
		
		return newVars;
	}
	
	protected VariableDataType getDataType(String mapping) {
		
		String strRepr = mapping.contains(CoreConstants.UNDER) ? mapping.substring(0, mapping.indexOf(CoreConstants.UNDER)) : "string";
		return VariableDataType.getByStringRepresentation(strRepr);
	}
	
	private String concPF(String path, String fileName) {
		return path+CoreConstants.SLASH+fileName;
	}
	
	protected BinaryVariable storeFile(final Object identifier, final URI fileUri) {
		
		String path = BPM_UPLOADED_FILES_PATH+identifier+"/files";
		
		final FileURIHandler fileURIHandler = getFileURIHandlerFactory().getHandler(fileUri);
		
		final FileInfo fileInfo = fileURIHandler.getFileInfo(fileUri);
		String fileName = fileInfo.getFileName();
		
		//replace windows absolute path filename with just filename
		if(fileName.contains(CoreConstants.BACK_SLASH)) {
			int lastBackSlashIndex = fileName.lastIndexOf(CoreConstants.BACK_SLASH);
			fileName = fileName.substring(lastBackSlashIndex + 1);
		}
		
		try {
			IWSlideService slideService = getIWSlideService();
			
			UsernamePasswordCredentials credentials = slideService.getRootUserCredentials();
			WebdavExtendedResource res = slideService.getWebdavExtendedResource(concPF(path, fileName), credentials);
			
			if(res.exists()) {
			
				boolean success = false;
				int i = 0;
				
				while(!success) {
				
					String p = path + (i++);
					res = slideService.getWebdavExtendedResource(concPF(p, fileName), credentials);
					
					if(!res.exists()) {
						path = p;
						success = true;
					}
				}
			}
			
			//FileInputStream is = new FileInputStream(fileUri);
			InputStream is = fileURIHandler.getFile(fileUri);
			
			try {
				slideService.uploadFileAndCreateFoldersFromStringAsRoot(path+CoreConstants.SLASH, fileName, is, null, false);
				
			} finally {
				
				if(is != null)
					is.close();
			}
			
			BinaryVariableImpl binaryVariable = new BinaryVariableImpl();
			binaryVariable.setFileName(fileName);
			binaryVariable.setIdentifier(concPF(path, fileName));
			binaryVariable.setStorageType(STORAGE_TYPE);
			binaryVariable.setContentLength(fileInfo.getContentLength());
			
			return binaryVariable;
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while storing binary variable. Path: "+path, e);
			return null;
		}
	}
	
	protected String convertToJSON(BinaryVariable binVar) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(BINARY_VARIABLE, BinaryVariableImpl.class);
		String jsonStr = xstream.toXML(binVar);
		return jsonStr;
	}
	
	protected BinaryVariable convertToBinaryVariable(String jsonStr) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(BINARY_VARIABLE, BinaryVariableImpl.class);
		BinaryVariable binVar = (BinaryVariable)xstream.fromXML(jsonStr);
		return binVar;
	}
	
	public Map<String, Object> resolveBinaryVariables(Map<String, Object> variables) {
	
//		TODO: shouldn't even be needed
		return null;
	}
	
	public List<BinaryVariable> resolveBinaryVariablesAsList(Map<String, Object> variables) {
		
		ArrayList<BinaryVariable> binaryVars = new ArrayList<BinaryVariable>(5);
		
		for (Object varVal : variables.values()) {
			
			if(varVal == null)
				continue;
			
			if(varVal instanceof BinaryVariable) {
				//binaryVars.add((BinaryVariable)varVal);
			}
			else if(varVal instanceof String) {
				BinaryVariable var;
				try {
					var = convertToBinaryVariable((String)varVal);
				} catch(StreamException e) {
					continue;
				}
				binaryVars.add(var);
			}
			else if(varVal instanceof Collection) {

				@SuppressWarnings("unchecked")
				Collection vals = (Collection)varVal;
				
				if(!vals.isEmpty() && vals.iterator().next() instanceof BinaryVariable) {
					
//					for (Object object : vals) {
//						binaryVars.add((BinaryVariable)object);
//					}
				}
				else if(!vals.isEmpty() && vals.iterator().next() instanceof String) {
					for (Object object : vals) {
						BinaryVariable var;
						try {
							var = convertToBinaryVariable((String)object);
						} catch(StreamException e) {
							continue;
						} catch(ClassCastException e) {
							continue;
						}
						binaryVars.add(var);
					}
				}
			}
		}
		
		return binaryVars;
	}
	
	public InputStream getBinaryVariableContent(BinaryVariable variable) {
		
		if(!STORAGE_TYPE.equals(variable.getStorageType()))
			throw new IllegalArgumentException("Unsupported binary variable storage type: "+variable.getStorageType());
		
		try {
			
			IWSlideService slideService = getIWSlideService();
			
			UsernamePasswordCredentials credentials = slideService.getRootUserCredentials();
			WebdavExtendedResource res = slideService.getWebdavExtendedResource(variable.getIdentifier(), credentials);
			
			if(!res.exists()) {
				
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "No webdav resource found for path provided: "+variable.getIdentifier());
				return null;
			}
			
			return res.getMethodData();
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving binary variable. Path: "+variable.getIdentifier(), e);
		}
		
		return null;
	}
	
	protected IWSlideService getIWSlideService() {
		
		try {
			return (IWSlideService) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
		} catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	public BinaryVariable getBinaryVariable(long taskInstanceId, int variableHash) {
		
		return null;
	}

	public FileURIHandlerFactory getFileURIHandlerFactory() {
		return fileURIHandlerFactory;
	}

	@Autowired
	public void setFileURIHandlerFactory(FileURIHandlerFactory fileURIHandlerFactory) {
		this.fileURIHandlerFactory = fileURIHandlerFactory;
	}
}