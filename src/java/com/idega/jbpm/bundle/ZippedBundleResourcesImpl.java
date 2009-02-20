package com.idega.jbpm.bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.idega.core.file.tmp.TmpFileResolver;
import com.idega.core.file.tmp.TmpFileResolverType;
import com.idega.io.ZipInstaller;
import com.idega.util.CoreConstants;
import com.idega.util.xml.XmlUtil;

/**
 * TODO: close, and cleanup after deploy (new method on processbundle)
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 *          Last modified: $Date: 2009/02/20 14:24:52 $ by $Author: civilis $
 */
@Scope("prototype")
@Service
public class ZippedBundleResourcesImpl implements ProcessBundleResources {

	@Autowired
	@TmpFileResolverType("defaultResolver")
	private TmpFileResolver uploadedResourceResolver;
	private Document bundleConfigXML;
	private Integer rootIdx;
	private String uploadDir;
	private Set<String> zipEntries;
	@Autowired
	private ZipInstaller zipInstaller;

	public void load(ZipInputStream zipInputStream) {

		try {
			String folder = "procbund" + System.currentTimeMillis()
					+ CoreConstants.SLASH;

			TmpFileResolver uploadedResourceResolver = getUploadedResourceResolver();

			String uploadDir = uploadedResourceResolver.getTmpUploadDir(folder);

			setUploadDir(uploadDir);

			Set<String> zipEntries = getZipInstaller().extractZIP(
					zipInputStream, new File(uploadDir));
			setZipEntries(zipEntries);

		} catch (IOException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Exception while resolving file from zip", e);
		}
	}

	public void close() {
		// TODO: implement, (cleanup of tmp files) and use the method
	}

	protected Integer getRootIdx(String resourcePath) throws IOException {

		if (this.rootIdx == null) {

			Integer rootIdx = null;

			Set<String> zipEntries = getZipEntries();

			for (String entryPath : zipEntries) {

				if (entryPath.contains(resourcePath)) {

					Integer pathIdx = entryPath.indexOf(resourcePath);

					if (rootIdx == null || rootIdx > pathIdx) {
						rootIdx = pathIdx;
					}
				}
			}

			this.rootIdx = rootIdx;
		}

		return rootIdx;
	}

	public InputStream getResourceIS(String resourcePath) {

		try {
			// the idea here is to get rootIdx for the resources that are being
			// resolved
			// root idx defines index of where resource path should start in the
			// zip entry path (name)
			Integer rootIdx = getRootIdx(resourcePath);

			if (rootIdx == null) {

				// resource not resolved by the resource path
				return null;
			}

			Set<String> entries = getZipEntries();
			InputStream resourceIS = null;

			for (String entryPath : entries) {

				if (entryPath.indexOf(resourcePath) == rootIdx) {
					// entry path contained the resource path, and it was at
					// correct root index, that means we found it in zip
					// our boy
					String path = resolveEntryPathNoFileName(entryPath);
					String fileName = resolveFileName(entryPath);

					File file = getUploadedResourceResolver().getFile(
							getUploadDir() + path, fileName);
					resourceIS = new FileInputStream(file);
					break;
				}
			}

			return resourceIS;

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {

		}
	}

	public TmpFileResolver getUploadedResourceResolver() {
		return uploadedResourceResolver;
	}

	public Document getConfiguration() {

		if (bundleConfigXML == null) {

			String configFileName = "bundle.xml";
			InputStream bundleConfigIS = getResourceIS(configFileName);

			if (bundleConfigIS == null)
				bundleConfigXML = null;
			else
				bundleConfigXML = XmlUtil.getXMLDocument(bundleConfigIS);
		}

		return bundleConfigXML;
	}

	ZipInstaller getZipInstaller() {
		return zipInstaller;
	}

	protected Set<String> getZipEntries() {
		return zipEntries;
	}

	protected void setZipEntries(Set<String> zipEntries) {
		this.zipEntries = zipEntries;
	}

	protected String getUploadDir() {
		return uploadDir;
	}

	protected void setUploadDir(String uploadDir) {
		this.uploadDir = uploadDir;
	}

	private String resolveEntryPathNoFileName(String path) {

		int lastSlash = path.lastIndexOf(CoreConstants.SLASH);
		if (lastSlash != -1) {
			path = path.substring(0, lastSlash);
		} else {
			path = CoreConstants.EMPTY;
		}

		return path;
	}

	private String resolveFileName(String path) {

		int lastSlash = path.lastIndexOf(CoreConstants.SLASH);
		if (lastSlash != -1) {
			path = path.substring(lastSlash + 1, path.length());
		}

		return path;
	}
}