package com.idega.jbpm.invitation;

import java.io.Serializable;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/06/12 18:31:27 $ by $Author: civilis $
 */
public class Message implements Serializable {

	private static final long serialVersionUID = -8656603555398778383L;
	private String subject;
	private String text;
	private String from;
	
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
}