package com.idega.jbpm.presentation;

import java.io.IOException;

import javax.faces.context.FacesContext;

import org.apache.myfaces.custom.htmlTag.HtmlTag;
import org.apache.myfaces.renderkit.html.util.AddResource;
import org.apache.myfaces.renderkit.html.util.AddResourceFactory;
import org.jboss.jbpm.IWBundleStarter;

import com.idega.facelets.ui.FaceletComponent;
import com.idega.presentation.IWBaseComponent;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2009/01/13 13:19:04 $ by $Author: civilis $
 *
 */
public class UIBPMIdentityMgmnt extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "com.idega.UIBPMIdentityMgmnt";

	private static final String containerFacet = "container";

	@Override
	protected void initializeComponent(FacesContext context) {
		super.initializeComponent(context);
	
		HtmlTag div = (HtmlTag)context.getApplication().createComponent(HtmlTag.COMPONENT_TYPE);
		div.setValue(divTag);
		
		FaceletComponent facelet = (FaceletComponent)context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI("/idegaweb/bundles/org.jboss.jbpm.bundle/facelets/UIBPMIdentityMgmnt.xhtml");
		
		div.getChildren().add(facelet);
		getFacets().put(containerFacet, div);
	}
	
	@Override
	public boolean getRendersChildren() {
		return true;
	}
	
	protected void addClientResources(FacesContext context) {
		
		try {
			AddResource resource = AddResourceFactory.getInstance(context);
			resource.addStyleSheet(context, AddResource.HEADER_BEGIN, getBundle(context, IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath()+"/style/egovBPM.css");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);
		
		addClientResources(context);
		renderChild(context, getFacet(containerFacet));
	}
}