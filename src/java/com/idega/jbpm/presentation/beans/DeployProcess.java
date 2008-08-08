package com.idega.jbpm.presentation.beans;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.file.util.MimeTypeUtil;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.bundle.ProcessBundle;
import com.idega.jbpm.bundle.ProcessBundleFactory;
import com.idega.jbpm.bundle.ProcessBundleManager;
import com.idega.jbpm.bundle.ProcessBundleResources;
import com.idega.jbpm.bundle.ZippedBundleResourcesImpl;
import com.idega.presentation.IWContext;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/08/08 16:16:18 $ by $Author: civilis $
 *
 */
@Scope("request")
@Service("bpmDeployProcess")
public class DeployProcess {
	
	private UploadedFile processBundle;
	@Autowired
	private ProcessBundleFactory processBundleFactory;
	@Autowired
	private ProcessBundleManager processBundleManager;
	@Autowired
	private ProcessBundleResources processBundleResources;
	
	public void deploy() {
		
		UploadedFile processBundle = getProcessBundle();
		
		if(processBundle  != null && MimeTypeUtil.MIME_TYPE_ZIP.equals(processBundle.getContentType())) {
		
			InputStream is = null;
			
			try {
				is = getProcessBundle().getInputStream();
				IWMainApplication iwma = IWContext.getCurrentInstance().getIWMainApplication();
				
				ZipInputStream zis = new ZipInputStream(is);
				
				ZippedBundleResourcesImpl resources = (ZippedBundleResourcesImpl)getProcessBundleResources();
				resources.load(zis);
				
				ProcessBundle pb = getProcessBundleFactory().createProcessBundle(resources);
				getProcessBundleManager().createBundle(pb, iwma);

			} catch (IOException e) {
				
				Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while reading process while getting process definition input stream", e);
//				TODO: display err msg
				
			} catch (Exception e) {
				
				Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while deploying process definition", e);
//				TODO: display err msg				
				
			} finally {
				
				try { 
					if(is != null)
						is.close();
				} catch (IOException e) { }
			}
		}
	}

	public UploadedFile getProcessBundle() {
		return processBundle;
	}

	public void setProcessBundle(UploadedFile processBundle) {
		this.processBundle = processBundle;
	}
	
	public ProcessBundleFactory getProcessBundleFactory() {
		return processBundleFactory;
	}
	
	public ProcessBundleManager getProcessBundleManager() {
		return processBundleManager;
	}

	public ProcessBundleResources getProcessBundleResources() {
		return processBundleResources;
	}
}