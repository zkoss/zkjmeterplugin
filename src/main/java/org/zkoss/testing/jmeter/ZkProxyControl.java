package org.zkoss.testing.jmeter;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;

public class ZkProxyControl extends ProxyControl {
	private static final long serialVersionUID = 2851218983765186150L;

	private static String AU_PATH = "ZkProxyControlGui.auPath";

	private static final int DEFAULT_PORT = 9999;
	private static final String DEFAULT_AU_PATH = "/zkau";
	private static final String DEFAULT_EXCLUDED_PATTERN = ".*/zkau/web/.*";

	public ZkProxyControl() {
		super();
		this.setPort(DEFAULT_PORT);
		this.setAuPath(DEFAULT_AU_PATH);
		this.addExcludedPattern(DEFAULT_EXCLUDED_PATTERN);
	}

	public synchronized void deliverSampler(HTTPSamplerBase sampler,
											TestElement[] subConfigs, SampleResult result) {

		super.deliverSampler(sampler, subConfigs, result);

		final String auPath = getAuPath();

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

	public String getAuPath() {
		return this.getPropertyAsString(AU_PATH);
	}

	public void setAuPath(String auPath) {
		this.setProperty(AU_PATH, auPath, "");
	}
}
