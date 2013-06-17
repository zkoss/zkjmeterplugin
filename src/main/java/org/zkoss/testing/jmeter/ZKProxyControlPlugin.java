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
	private JTextField auPathField;

	public String getStaticLabel() {
		return "ZK HTTP Proxy Server";
	}
	
	public ZKProxyControlPlugin() {
		init();
	}
	
	private void init() {

		JPanel panel = (JPanel) getComponent(2);

		Box box = (Box) ((JPanel) ((JPanel) ((Box) panel.getComponent(0))
				.getComponent(0)).getComponent(0)).getComponent(0);

		auPathField = new JTextField("zkau", 5);
		// portField.setName(PORTFIELD);
		auPathField.addKeyListener(this);

		((JTextField)box.getComponent(2)).setText("9999");
		
		JLabel label = new JLabel("ZKau path:");
		label.setLabelFor(auPathField);
		box.add(Box.createHorizontalStrut(10));
		box.add(label);
		box.add(Box.createHorizontalStrut(5));
		box.add(auPathField);

		JTable excludeTable = (JTable) ((JViewport) ((JScrollPane) ((JPanel) ((Box) panel
				.getComponent(1)).getComponent(1)).getComponent(0))
				.getComponent(0)).getComponent(0);
		
		
		PowerTableModel model = (PowerTableModel) excludeTable.getModel();
		model.addRow(new String[] {".*/zkau/web/.*"});
	}
	




	protected ProxyControl makeProxyControl() {
		
		final String auPath = auPathField.getText();
		
		ProxyControl local = new ProxyControl() {
			@Override
			public synchronized void deliverSampler(HTTPSamplerBase sampler,
					TestElement[] subConfigs, SampleResult result) {
				
				
				super.deliverSampler(sampler, subConfigs, result);
				
				if (result.getContentType().startsWith("text/html")) {
					
					JMeterTreeModel treeModel = GuiPackage.getInstance().getTreeModel();
					
					JMeterTreeNode newNode = treeModel.getNodeOf(sampler);
					
					try {
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
				} else if (!auPath.isEmpty()) {
					String path = sampler.getPath();
					int i = path.indexOf("jsessionid");
					if (i > -1)
						path = path.substring(0, i - 1);
					
					if (path.endsWith(auPath)) {
						Arguments args = sampler.getArguments();
						Argument arg = args.getArgument(0);
						if ("dtid".equals(arg.getName())) {
							arg.setValue("${dtid}");
						}
					}
					
				}
			}
		};
		return local;
	}
}
