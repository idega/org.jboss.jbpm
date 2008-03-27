package com.idega.jbpm.exe.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.jbpm.def.VariableDataType;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.slide.util.WebdavExtendedResource;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/27 16:18:15 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class BinaryVariablesHandlerImpl {

	public Map<String, Object> storeBinaryVariables(Object identifier, Map<String, Object> variables) {
		
		HashMap<String, Object> newVars = new HashMap<String, Object>(variables);
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		for (Entry<String, Object> entry : newVars.entrySet()) {
			
			Object val = entry.getValue();
			
			if(val == null)
				continue;
			
			String key = entry.getKey();
			
			VariableDataType dataType = getDataType(key);
			
			if(dataType == VariableDataType.FILE) {

				if(val instanceof File) {
					
					String fileIdentifier = storeFile(iwc, (String)identifier, (File)val);
					entry.setValue(fileIdentifier);
					
				} else {
					entry.setValue(null);
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Variable data type resolved: "+dataType+", but value data type didn't match ("+val.getClass().getName()+"), variable name: "+key);
				}
			} else if(dataType == VariableDataType.FILES) {
				
				if(val instanceof Collection) {
					
					@SuppressWarnings("unchecked")
					Collection files = (Collection)val;
					
					if(files.isEmpty()) {
						entry.setValue(null);
					} else {
						
						if(files.iterator().next() instanceof File) {
							
							@SuppressWarnings("unchecked")
							Collection<File> afiles = (Collection<File>)files;

							for (File file : afiles) {
								String fileIdentifier = storeFile(iwc, identifier, file);
								entry.setValue(fileIdentifier);
							}
							
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
	
	public static final String BPM_UPLOADED_FILES_PATH = "/files/bpm/uploadedFiles/";
	
	private String concPF(String path, String fileName) {
		return path+CoreConstants.SLASH+fileName;
	}
	
	public String storeFile(IWContext iwc, Object identifier, File file) {
		
		String path = BPM_UPLOADED_FILES_PATH+identifier+"/files";
		String fileName = file.getName();
		try {
			IWSlideService slideService = getIWSlideService(iwc);
			
			UsernamePasswordCredentials credentials = slideService.getRootUserCredentials();
			WebdavExtendedResource res = slideService.getWebdavExtendedResource(concPF(path, fileName), credentials);
			
			if(res.exists()) {
			
				boolean success = false;
				int i = 0;
				
				while(!success) {
				
					path += (i++);
					res = slideService.getWebdavExtendedResource(concPF(path, fileName), credentials);
					
					if(!res.exists())
						success = true;
				}
			}
			
			path = res.getPath();
			System.out.println("path: "+path);
			System.out.println("parent path: "+res.getParentPath());
			
			InputStream is = null;
			//slideService.uploadFileAndCreateFoldersFromStringAsRoot("path", "file_name", is, "text/xml", false);
			
			//String fileIdentifier = path +
			
			return path;
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while storing binary variable. Path: "+path, e);
			return null;
		}
	}
	
	public Map<String, Object> resolveBinaryVariables(Map<String, Object> variables) {
	
		return null;
	}
	
	public InputStream getBinaryVariableContent(String variableValue) {
		
		return null;
	}
	
	protected IWSlideService getIWSlideService(IWContext iwc) {
		
		try {
			return (IWSlideService) IBOLookup.getServiceInstance(iwc, IWSlideService.class);
		} catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
}