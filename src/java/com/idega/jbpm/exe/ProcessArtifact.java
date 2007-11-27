package com.idega.jbpm.exe;

import java.util.Date;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/27 16:33:26 $ by $Author: civilis $
 */
public class ProcessArtifact {

	private String id;
	private String checkoutLink;
	private String name;
	private Date createDate;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCheckoutLink() {
		return checkoutLink;
	}
	public void setCheckoutLink(String checkoutLink) {
		this.checkoutLink = checkoutLink;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
}