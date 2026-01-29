package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlTransient;

public class BPMAttachment implements Serializable {

	private static final long serialVersionUID = 5056538842789014352L;

	private String id, description, fileName, fileSize, downloadLink, source, date, fileToken;

	@XmlTransient
	private Date timestamp;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileSize() {
		return fileSize;
	}

	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}

	public String getDownloadLink() {
		return downloadLink;
	}

	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getFileToken() {
		return fileToken;
	}

	public void setFileToken(String fileToken) {
		this.fileToken = fileToken;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BPMAttachment) {
			String dLink1 = getDownloadLink();
			String dLink2 = ((BPMAttachment) obj).getDownloadLink();
			if (dLink1 != null && dLink2 != null && dLink1.equals(dLink2)) {
				return true;
			}

			String token1 = getFileToken();
			String token2 = ((BPMAttachment) obj).getFileToken();
			if (token1 != null && token2 != null && token1.equals(token2)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return "File name: " + getFileName() + ", ID: " + getId() + ", download link: " + getDownloadLink();
	}

}