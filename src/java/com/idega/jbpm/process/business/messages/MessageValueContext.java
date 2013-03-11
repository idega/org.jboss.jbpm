package com.idega.jbpm.process.business.messages;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $ Last modified: $Date: 2009/03/02 15:33:00 $ by $Author: civilis $
 */
public class MessageValueContext {

	// standard values
	public static final TypeRef bpmUserBean = new TypeRef("bean", "bpmUser");
	public static final TypeRef userBean = new TypeRef("bean", "user");
	public static final TypeRef updBean = new TypeRef("bean", "upd");
	public static final TypeRef tokenBean = new TypeRef("bean", "token");
	public static final TypeRef piwBean = new TypeRef("bean", "piw");
	public static final TypeRef iwcBean = new TypeRef("bean", "iwc");

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
	public <Z> Z getValue(String handlerType, String ref) {
		Z val = (Z) getValue(new TypeRef(handlerType, ref));
		return val;
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(TypeRef tr) {
		return (T) ctx.get(tr);
	}

	public boolean contains(String handlerType, String ref) {
		return contains(new TypeRef(handlerType, ref));
	}

	public boolean contains(TypeRef tr) {
		return ctx.containsKey(tr);
	}

	public Map<String, Object> getScriptInputMap(String typeQualifier) {

		HashMap<String, Object> scriptInputs = new HashMap<String, Object>(ctx
		        .size());

		for (Entry<TypeRef, Object> entry : ctx.entrySet()) {

			if (entry.getKey().getHandlerType().equals(typeQualifier)) {
				scriptInputs.put(entry.getKey().getRef(), entry.getValue());
			}
		}

		return scriptInputs;
	}

	@Override
	public String toString() {
		return "Context: " + ctx;
	}
}