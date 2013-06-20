package org.zkoss.testing.jmeter;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jmeter.protocol.http.proxy.gui.ProxyControlGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;

public class ZKProxyControlPlugin extends ProxyControlGui {

	private static final long serialVersionUID = -7607963125510262131L;
	private static final String TITLE = "ZK HTTP Proxy Server";
	
	
	private JTextField auPathField;

	public String getStaticLabel() {
		return TITLE;
	}
	
	public ZKProxyControlPlugin() {
		init();
	}
	
	private void init() {

		Box box = getProxyPortArea();
		// Set a default value to the ProxyServer port. 
		((JTextField)box.getComponent(2)).setText("9999");
		
		
		// add zkau input field 
		auPathField = new JTextField("zkau", 5);
		// portField.setName(PORTFIELD);
		auPathField.addKeyListener(this);
		
		JLabel label = new JLabel("ZK update-uri: ");
		label.setLabelFor(auPathField);
		
		box.add(Box.createHorizontalStrut(10));//add space
		box.add(label);
		box.add(Box.createHorizontalStrut(5));//add space
		box.add(auPathField);

		// Set a default value to the excludeTable 
		JTable excludeTable = getExcludeTable();
		
		
		PowerTableModel model = (PowerTableModel) excludeTable.getModel();
		model.addRow(new String[] {".*/zkau/web/.*"});
	}
	




	private JTable getExcludeTable() {
		return (JTable) ((JViewport) ((JScrollPane) ((JPanel) ((Box) 
			((JPanel) getComponent(2)).getComponent(1)).getComponent(1))
			.getComponent(0)).getComponent(0)).getComponent(0);
	}

	private Box getProxyPortArea() {
		return (Box) ((JPanel) ((JPanel) ((Box) 
			((JPanel) getComponent(2)).getComponent(0))
			.getComponent(0)).getComponent(0)).getComponent(0);
	}

	protected ProxyControl makeProxyControl() {
		
		final String auPath = auPathField.getText();
		
		ProxyControl local = new ProxyControl() {
			@Override
			public synchronized void deliverSampler(HTTPSamplerBase sampler,
					TestElement[] subConfigs, SampleResult result) {
				
				
				super.deliverSampler(sampler, subConfigs, result);
				
				if (result.getContentType().startsWith("text/html")) {// zul page
					
					JMeterTreeModel treeModel = GuiPackage.getInstance().getTreeModel();
					JMeterTreeNode newNode = treeModel.getNodeOf(sampler);
					
					try {
						//add a RegexExtractor for retrieve the ZK dtid 
						RegexExtractor extr = new RegexExtractor();
						extr.setProperty(TestElement.GUI_CLASS, RegexExtractorGui.class.getName());
						extr.setName("Regular Expression Extractor");
						extr.setRefName("dtid");
						extr.setRegex("dt:\\'(.+?)\\',cu");
						extr.setTemplate("$1$");
						extr.setDefaultValue("not found");
						treeModel.addComponent(extr, newNode);
					} catch (IllegalUserActionException e) {
						e.printStackTrace();
					}
				} else if (!auPath.isEmpty()) { // zkau request
					
					
					String path = sampler.getPath();
					int i = path.indexOf("jsessionid");
					if (i > -1)
						path = path.substring(0, i - 1);
					
					if (path.endsWith(auPath)) {
						Arguments args = sampler.getArguments();
						Argument arg = args.getArgument(0);
						if ("dtid".equals(arg.getName())) {//replace dtid value to a EL variable.
							arg.setValue("${dtid}");
						}
					}
					
					
				}
			}
		};
		return local;
	}
}
