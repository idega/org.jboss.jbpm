package com.idega.jbpm.exe.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.def.VariableDataType;
import com.idega.jbpm.exe.BinaryVariablesHandler;
import com.idega.slide.business.IWSlideService;
import com.idega.slide.util.WebdavExtendedResource;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/04/25 00:05:26 $ by $Author: laddi $
 */
@Scope("singleton")
@Service
public class BinaryVariablesHandlerImpl implements BinaryVariablesHandler {
	
	public static final String BPM_UPLOADED_FILES_PATH = "/files/bpm/uploadedFiles/";
	public static final String STORAGE_TYPE = "slide";

	@SuppressWarnings("cast")
	public Map<String, Object> storeBinaryVariables(Object identifier, Map<String, Object> variables) {
		
		HashMap<String, Object> newVars = new HashMap<String, Object>(variables);
		
		for (Entry<String, Object> entry : newVars.entrySet()) {
			
			Object val = entry.getValue();
			
			if(val == null)
				continue;
			
			String key = entry.getKey();
			
			VariableDataType dataType = getDataType(key);
			
			if(dataType == VariableDataType.FILE) {

				if(val instanceof File) {
					
					BinaryVariable binaryVariable = storeFile((String)identifier, (File)val);
					entry.setValue(binaryVariable);
					
				} else {
					entry.setValue(null);
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Variable data type resolved: "+dataType+", but value data type didn't match ("+val.getClass().getName()+"), variable name: "+key);
				}
			} else if(dataType == VariableDataType.FILES) {
				
				if(val instanceof Collection) {
					Collection files = (Collection)val;
					
					if(files.isEmpty()) {
						entry.setValue(null);
					} else {
						
						if(files.iterator().next() instanceof File) {
							Collection<File> afiles = (Collection<File>)files;
							ArrayList<BinaryVariable> binaryVariables = new ArrayList<BinaryVariable>(afiles.size());

							for (File file : afiles) {
								BinaryVariable binaryVariable = storeFile(identifier, file);
								binaryVariables.add(binaryVariable);
							}
							
							entry.setValue(binaryVariables);
							//entry.setValue(!binaryVariables.isEmpty() ? binaryVariables.iterator().next() : null);
							
						} else {
							entry.setValue(null);
							Logger.getLogger(getClass().getName()).log(Level.WARNING, "Variable data type resolved: "+dataType+", but value data type didn't match ("+val.getClass().getName()+"), variable name: "+key);
						}
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
		
		String strRepr = mapping.contains(CoreConstants.COLON) ? mapping.substring(0, mapping.indexOf(CoreConstants.COLON)) : "string";
		return VariableDataType.getByStringRepresentation(strRepr);
	}
	
	private String concPF(String path, String fileName) {
		return path+CoreConstants.SLASH+fileName;
	}
	
	protected BinaryVariable storeFile(Object identifier, File file) {
		
		String path = BPM_UPLOADED_FILES_PATH+identifier+"/files";
		String fileName = file.getName();
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
			
			FileInputStream is = new FileInputStream(file);
			slideService.uploadFileAndCreateFoldersFromStringAsRoot(path+CoreConstants.SLASH, fileName, is, null, false);
			is.close();
			
			BinaryVariableImpl binaryVariable = new BinaryVariableImpl();
			binaryVariable.setFileName(fileName);
			binaryVariable.setIdentifier(concPF(path, fileName));
			binaryVariable.setStorageType(STORAGE_TYPE);
			binaryVariable.setContentLength(file.length());
			
			return binaryVariable;
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while storing binary variable. Path: "+path, e);
			return null;
		}
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
			
			if(varVal instanceof BinaryVariable)
				binaryVars.add((BinaryVariable)varVal);
			else if(varVal instanceof Collection) {
				Collection vals = (Collection)varVal;
				
				if(!vals.isEmpty() && vals.iterator().next() instanceof BinaryVariable) {
					
					for (Object object : vals) {
						binaryVars.add((BinaryVariable)object);
					}
				}
			}
		}
		
		return binaryVars;
	}
	
//	public InputStream getBinaryVariableContent(BinaryVariable variable) {
//		
//		return getBinaryVariableContent(null, variable);
//	}
	
	public InputStream getBinaryVariableContent(BinaryVariable variable) {
		
		if(!STORAGE_TYPE.equals(variable.getStorageType()))
			throw new IllegalArgumentException("Unsupported binary variable storage type: "+variable.getStorageType());
		
		try {
			
//			if(iwc == null)
//				iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
			
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
}