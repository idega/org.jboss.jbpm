package com.idega.jbpm.process.business.messages;

import java.util.HashMap;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/08/08 16:16:55 $ by $Author: civilis $
 */
public class MessageValueContext {

//	standard values
	public static final TypeRef userBean 			= new TypeRef("bean", "user");
	public static final TypeRef tokenBean 			= new TypeRef("bean", "token");
	
	private final HashMap<TypeRef, Object> ctx;
	
	public MessageValueContext() {
		ctx = new HashMap<TypeRef, Object>();
	}
	
	public MessageValueContext(int cnt) {
		ctx = new HashMap<TypeRef, Object>(cnt);
	}
	
	public void setValue(String handlerType, String ref, Object value) {
		setValue(new TypeRef(handlerType, ref), value);
	}
	
	public void setValue(TypeRef tr, Object value) {
		ctx.put(tr, value);
	}

	@SuppressWarnings("unchecked")
	public <Z>Z getValue(String handlerType, String ref) {
		Z val = (Z)getValue(new TypeRef(handlerType, ref));
		return val;
	}
	
	@SuppressWarnings("unchecked")
	public <T>T getValue(TypeRef tr) {
		return (T)ctx.get(tr);
	}
	
	public boolean contains(String handlerType, String ref) {
		return contains(new TypeRef(handlerType, ref));
	}
	
	@SuppressWarnings("unchecked")
	public boolean contains(TypeRef tr) {
		return ctx.containsKey(tr);
	}
}