package com.idega.jbpm.bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.core.file.tmp.TmpFilesManager;
import com.idega.io.ZipInstaller;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/07/21 08:53:30 $ by $Author: civilis $
 */
@Scope("prototype")
@Service
public class ZippedBundleResourcesImpl implements ProcessBundleResources {
	
	@Autowired	
	private TmpFilesManager fileUploadManager;
	@Autowired
	@TmpFileResolverType("defaultResolver")
	private TmpFileResolver uploadedResourceResolver;
	private String folder;

	public void load(ZipInputStream zipInputStream) {

		try {
			ZipEntry entry;
			ZipInstaller zip = new ZipInstaller();
			TmpFilesManager fileUploadManager = getFileUploadManager();
			TmpFileResolver uploadedResourceResolver = getUploadedResourceResolver();
			String folder = "procbund"+System.currentTimeMillis()+CoreConstants.SLASH;
			setFolder(folder);
			
			while ((entry = zipInputStream.getNextEntry()) != null) {
				
				if(!entry.isDirectory()) {
				
					String entryName = entry.getName();
					
					entryName = resolveFileName(entryName);
					
//					for now putting all files from all subfolders into one folder
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					zip.writeFromStreamToStream(zipInputStream, os);
					InputStream is = new ByteArrayInputStream(os.toByteArray());

					fileUploadManager.uploadToTmpDir(folder, entryName, is, uploadedResourceResolver);
				}
			}
			
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving file from zip", e);
		}
	}
	
	private String resolveFileName(String fileName) {
		
		int lastSlash = fileName.lastIndexOf(CoreConstants.SLASH);
		if (lastSlash != -1) {
			fileName = fileName.substring(lastSlash + 1, fileName.length());
		}
		
		return fileName;
	}
	
	public void close() {
//		TODO: implement, (cleanup of tmp files) and use the method
	}
	
	public InputStream getResourceIS(String path) {
		
		Collection<URI> uris = getFileUploadManager().getFilesUris(getFolder(), null, getUploadedResourceResolver());
		String fileName = resolveFileName(path);
		
		for (URI uri : uris) {
			
			File f = new File(uri);
			
			if(f.getName().equals(fileName)) {
				
				try {
					return new FileInputStream(f);
					
				} catch (FileNotFoundException e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Tmp file not found by uri="+uri, e);
				}
			}
		}
		
		return null;
	}

	public TmpFilesManager getFileUploadManager() {
		return fileUploadManager;
	}

	public TmpFileResolver getUploadedResourceResolver() {
		return uploadedResourceResolver;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}
}