package com.idega.jbpm.presentation;

import java.io.IOException;

import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;

import com.idega.facelets.ui.FaceletComponent;
import com.idega.presentation.IWBaseComponent;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/04/26 02:48:31 $ by $Author: civilis $
 *
 */
public class UIRegProcessParticipant extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "com.idega.UIRegProcessParticipant";

	private static final String containerFacet = "container";

	@Override
	@SuppressWarnings("unchecked")
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
	
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue(divTag);
		
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI("/idegaweb/bundles/org.jboss.jbpm.bundle/facelets/UIRegProcessParticipant.xhtml");
		
		div.getChildren().add(facelet);
		getFacets().put(containerFacet, div);
	}
	
	@Override
	public boolean getRendersChildren() {
		return true;
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);
		
		renderChild(context, getFacet(containerFacet));
	}
}