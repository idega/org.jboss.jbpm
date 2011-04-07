package com.idega.jbpm;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

//@Service("testHandlerForSpringIntegration")
public class HandlerForSpringIntegration implements ActionHandler {

	private static final long serialVersionUID = 1L;

	private String justString;

	private List<Object> expretionObject;

	public List<Object> getExpretionObject() {
		return expretionObject;
	}

	public void setExpretionObject(List<Object> expretionObject) {
		this.expretionObject = expretionObject;
	}

	public String getJustString() {
		return justString;
	}

	public void setJustString(String justString) {
		this.justString = justString;
	}

	public void execute(ExecutionContext executionContext) throws Exception {

		System.out.println("I'm alive!!!");
		System.out.println("justSring value is: " + justString
				+ " , expretionObject: " + expretionObject.iterator().next());
	}

}
