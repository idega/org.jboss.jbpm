package com.idega.jbpm.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWBundle;
import com.idega.util.expression.ELUtil;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/07/19 20:41:08 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class ProcessBundleFactory {

	public ProcessBundle createProcessBundle(ProcessBundleResources resources) {
		
		try {
			InputStream propertiesIs = resources.getResourceIS("bundle.properties");
			
			if(propertiesIs == null)
				throw new RuntimeException("No bundle.properties found");
			
			final Properties properties = new Properties();
			properties.load(propertiesIs);
			
			String processBundleIdentifier = properties.getProperty("process_definition.processBundle.beanIndentifier");
			
			ProcessBundle procBundle = (ProcessBundle)ELUtil.getInstance().getBean(processBundleIdentifier);
			procBundle.setBundleResources(resources);
			
			return procBundle;
			
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Exception while creating process bundle from zip", e);
		}
		
		return null;
	}
	
	public ProcessBundle createProcessBundle(IWBundle bundle, Properties props, Integer version, String pathToPropsWithinBundle) {
		
		String processBundleIdentifier = props.getProperty("process_definition.processBundle.beanIndentifier");
		
		try {
			ProcessBundle procBundle = (ProcessBundle)ELUtil.getInstance().getBean(processBundleIdentifier);

			JarModuleBundleResourcesImpl resources = new JarModuleBundleResourcesImpl();
			resources.setBundle(bundle);
			resources.setBundlePropertiesLocationWithinBundle(pathToPropsWithinBundle);
			
			procBundle.setBundleResources(resources);
			
			ProcessDefinition pd = procBundle.getProcessDefinition();
			
			if(pd != null) {
			
				pd.setVersion(version);
				return procBundle;
			}
			
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "process definition not found", e);
		}
		
		return null;
	}
	
	/*
	private InputStream resolveISFromZIP(InputStream zipInputStream, String fileName) {
		
		ZipInputStream zipStream = null;
		
		try {
			ZipEntry entry;
			zipStream = new ZipInputStream(zipInputStream);
			
			while ((entry = zipStream.getNextEntry()) != null) {
				
				String entryName = entry.getName();
				System.out.println("entryname:"+entryName);
				
				if(fileName.equals(entryName)) {
					
					ZipInstaller zip = new ZipInstaller();
				
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					zip.writeFromStreamToStream(zipStream, os);
					InputStream is = new ByteArrayInputStream(os.toByteArray());
					zipStream.closeEntry();
					return is;
				}
				
				zipStream.closeEntry();
			}
			
		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving file from zip by file name="+fileName, e);
		}
		
		return null;
	}
	*/
}