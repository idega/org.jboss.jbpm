package com.idega.jbpm.bean;

public class XStreamAlias {

	private String name;
	private Class<?> theClass;

	public XStreamAlias(String name, Class<?> theClass) {
		this.name = name;
		this.theClass = theClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getTheClass() {
		return theClass;
	}

	public void setTheClass(Class<?> theClass) {
		this.theClass = theClass;
	}

	@Override
	public String toString() {
		return getName() + ": " + getTheClass();
	}

}