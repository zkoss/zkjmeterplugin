package org.zkoss.testing.jmeter;

import java.awt.Component;
import java.awt.Container;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.tree.TreePath;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.gui.ReportGuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jmeter.protocol.http.proxy.gui.ProxyControlGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;

public class ZKProxyControlPlugin extends ProxyControlGui {

	private JTextField auPathField;


	


	public String getStaticLabel() {
		return "ZK HTTP Proxy Server";
	}

	
	private void printComps(Container c, int l) {
		for (Component c2 : c.getComponents()) {
			
			for (int i = 0; i < l; i++) {
				System.out.print("\t");
			}
			System.out.println(c2.getClass());
			
			if (c2 instanceof Container)
				printComps((Container) c2, l + 1);
		}
		
	}
	
	public ZKProxyControlPlugin() {
		super();

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
				} else if (!auPath.isEmpty() && sampler.getPath().endsWith(auPath)) {
					Arguments args = sampler.getArguments();
					Argument arg = args.getArgument(0);
					if ("dtid".equals(arg.getName())) {
						arg.setValue("${dtid}");
					}
				}
			}
		};
		return local;
	}
}
