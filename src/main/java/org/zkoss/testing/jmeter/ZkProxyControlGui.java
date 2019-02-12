package org.zkoss.testing.jmeter;

import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jmeter.protocol.http.proxy.gui.ProxyControlGui;
import org.apache.jmeter.testelement.TestElement;

import javax.swing.*;
import java.awt.*;

public class ZkProxyControlGui extends ProxyControlGui {
	private static final long serialVersionUID = -8728372266544596465L;

	private static final String TITLE = "ZK HTTP Proxy Server";

	private ZkProxyControl model;
	private JTextField auPathField;

	public ZkProxyControlGui() {
		init();
		configureDefaults();
	}

	private void init() {
		auPathField = new JTextField(20);
		auPathField.addKeyListener(this);

		JLabel label = new JLabel("ZK update-uri:");
		label.setLabelFor(auPathField);

		JPanel zkSettingsPanel = new JPanel();
		zkSettingsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "ZK Settings"));
		zkSettingsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		zkSettingsPanel.add(label);
		zkSettingsPanel.add(Box.createHorizontalStrut(5)); //add space
		zkSettingsPanel.add(auPathField);

		add(zkSettingsPanel, "South");
	}

	private void configureDefaults() {
		configure(makeProxyControl());
	}

	@Override
	public String getStaticLabel() {
		return TITLE;
	}

	@Override
	public ProxyControl makeProxyControl() {
		return new ZkProxyControl();
	}

	@Override
	public TestElement createTestElement() {
		this.model = (ZkProxyControl) super.createTestElement();
		return this.model;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		super.modifyTestElement(element);
		if(element instanceof ZkProxyControl) {
			this.model = (ZkProxyControl) element;
			this.model.setAuPath(auPathField.getText());
		}
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		this.model = (ZkProxyControl) element;
		this.auPathField.setText(this.model.getAuPath());
	}
}
