package org.jboss.jbpm;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import javax.faces.context.FacesContext;

import junit.framework.Assert;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import com.idega.presentation.IWBaseComponent;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/08/30 13:09:12 $ by $Author: civilis $
 *
 */
public class DeployProcess extends IWBaseComponent {
	
	public DeployProcess() {
		
		super();
		setRendererType(null);
	}
	
	@Override
	protected void initializeComponent(FacesContext context) {
		
		System.out.println("initializing DeployProcess");
		JbpmConfiguration cfg = JbpmConfiguration.getInstance();
		
		cfg.createSchema();
		
		JbpmContext ctx = cfg.createJbpmContext();
		System.out.println("cfg: "+cfg);
		System.out.println("ctx: "+ctx);
		
		ZipInputStream zis = null;
		
		FileInputStream is = null;
		try {

			is = new FileInputStream("/Users/civilis/dev/tmp/procedef.zip");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(is == null)
			return;
		
		try {
			zis = new ZipInputStream(is);
//			final ProcessDefinition processDefinition = ProcessDefinition.parseParZipInputStream(zis);
			
			final ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
					"<process-definition name='hello world'>" +
				      "  <start-state name='start'>" +
				      "    <transition to='s' />" +
				      "  </start-state>" +
				      "  <state name='s'>" +
				      "    <transition to='end' />" +
				      "  </state>" +
				      "  <end-state name='end' />" +
				      "</process-definition>");
			
			System.out.println("deploying: "+processDefinition);
			ctx.deployProcessDefinition(processDefinition);
			
			ProcessInstance processInstance = 
			      new ProcessInstance(processDefinition);
			  
			  // After construction, the process execution has one main path
			  // of execution (=the root token).
			  Token token = processInstance.getRootToken();
			  
			  System.out.println("asserting..");
			  Assert.assertSame(processDefinition.getStartState(), token.getNode());
			  
			  token.signal();
			  Assert.assertSame(processDefinition.getNode("s"), token.getNode());

			  token.signal();
			  
			  Assert.assertSame(processDefinition.getNode("end"), token.getNode());
			
			
		} finally {
				ctx.close();
				cfg.dropSchema();
				try {
                    zis.close();
                } catch (IOException e) {
                	e.printStackTrace();
                }
			}
	}
	
	@Override
	public boolean getRendersChildren() {
		return true;
	}
	
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		System.out.println("DeployProcess encode begin: ");
		super.encodeBegin(context);
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		
		super.encodeChildren(context);
	}
	
	@Override
	public String getFamily() {
		return "idegaweb";
	}
}