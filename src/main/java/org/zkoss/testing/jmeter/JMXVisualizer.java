package org.zkoss.testing.jmeter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;
import org.apache.jorphan.gui.JLabeledTextField;
import org.zkoss.testing.jmeter.chart.ChartPanel;

public class JMXVisualizer extends AbstractVisualizer {
	private static final long serialVersionUID = 7906471251449301718L;
	private ChartPanel graphPanel;

	private static final String TITLE = "JMX Visualizer";
	
	private static final int DEFAULT_WIDTH = 400;
	private static final int DEFAULT_HEIGTH = 300;
	private static final int INTERVAL_DEFAULT = 500;
	private int intervalValue = INTERVAL_DEFAULT;

	private final JLabeledTextField serverIPField = 
		new JLabeledTextField("Server IP: ", 15);

	private final JLabeledTextField jmxPortField = 
		new JLabeledTextField("JMX Port: ", 6);

	private final JLabeledTextField intervalField = 
		new JLabeledTextField("Interval (ms): ", 10);
	
	private final JCheckBox doGC = new JCheckBox("Garbage Collection");


	
	private Map<String, Long> result = new LinkedHashMap<String, Long>();
	private DateFormat dfmt = new SimpleDateFormat("HH:mm:ss.SSS");
	private Timer jmxRecorderTimer;
	
	
	public String getStaticLabel() {
		return TITLE;
	}
	
	public String getLabelResource() {
		return "jmx_visualizer_title";
	}
	
	public JMXVisualizer() {
		init();
	}

	private void init() {
		this.setLayout(new BorderLayout());

		JPanel northPane = new JPanel(new BorderLayout());

		JPanel settingsPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		settingsPane.add(serverIPField);
		settingsPane.add(jmxPortField);
		settingsPane.add(intervalField);
		intervalField.setText(intervalValue + "");
		doGC.setSelected(true);
		settingsPane.add(doGC);

		northPane.add(makeTitlePanel(), BorderLayout.NORTH);
		northPane.add(settingsPane, BorderLayout.CENTER);

		graphPanel = new ChartPanel();
		graphPanel.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGTH));

		// this.add(makeTitlePanel());
		// this.add(settingsPane);
		this.add(northPane, BorderLayout.NORTH);
		this.add(graphPanel, BorderLayout.CENTER);
	}

	public void add(SampleResult sample) {
	}

	public void clearData() {
	}

	public TestElement createTestElement() {
		collector = new ResultCollector(new Summariser() {

			private JMXConnector connector;

			public void testStarted(String host) {
				super.testStarted(host);

				if (!"*local*".equals(host))
					return;
				
				
				try {
					connector = createJMXConnector();
					if (connector != null)
						startMonitorHeap(connector);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			

			public void testEnded(String host) {
				super.testEnded(host);

				if (!"*local*".equals(host))
					return;

				if (connector == null) return;
				
				JMeterUtils.runSafe(new Runnable() {

					public void run() {
						try {
							//do not stop JMX recorder immediately, record 5 sec memory data.
							Thread.sleep(5000);
							
							stopMonitorHeap(connector);
							makeGraph();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							try {
								connector.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					
				 });
				
				
			}
		});
		return super.createTestElement();
	}
	
	private boolean isEmpty(String text) {
		return text == null || text.trim().length() == 0;
	}

	
	private JMXConnector createJMXConnector() throws MalformedURLException, IOException {
		String ipText = serverIPField.getText();
		String portText = jmxPortField.getText();
		
		if (isEmpty(ipText) || isEmpty(portText))
			return null;
		
		int port = 0;
		
		try {
			port = Integer.parseInt(portText);
		} catch (NumberFormatException e) {
			return null;
		}
		
		return JMXConnectorFactory.connect(new JMXServiceURL(
				"service:jmx:rmi:///jndi/rmi://"+ipText+":"+port+"/jmxrmi"), null);
	}
	
	private void startMonitorHeap(JMXConnector connector) throws Exception {
		String intervalText = intervalField.getText();
		int interval = 0;
		try {
			interval = Integer.parseInt(intervalText);
		} catch (NumberFormatException e) {
			interval = 500;
		}
			
		result.clear();
		
		jmxRecorderTimer = new Timer();
		MBeanServerConnection connection = connector.getMBeanServerConnection();
		if (doGC.isSelected())
			doGarbageCollection(connection);
				
		addData(getMemoryUsage(connection).getUsed());
		jmxRecorderTimer.schedule(new JMXTask(connection, this), 0, interval);
	}
	
	private void stopMonitorHeap(JMXConnector connector) throws Exception {
		if (jmxRecorderTimer != null) {
			jmxRecorderTimer.cancel();
			jmxRecorderTimer = null;
			MBeanServerConnection connection = connector.getMBeanServerConnection();
			if (doGC.isSelected())
				doGarbageCollection(connection);
			addData(getMemoryUsage(connection).getUsed());
		}
	}
	
	public void addData(long used) {
		result.put(dfmt.format(new Date()), used / 1024);
	}

	private void makeGraph() {
		// canvas size
		Dimension size = graphPanel.getSize();

		// graphPanel.setShowGrouping(false);
		graphPanel.setHeight((int) size.getHeight());
		graphPanel.setWidth((int) size.getWidth());
		// Draw the graph
		
		graphPanel.setResult(result);
		graphPanel.repaint();
	}

	private static class JMXTask extends TimerTask  {
		private MBeanServerConnection connection;
		private JMXVisualizer visualizer;

		public JMXTask(MBeanServerConnection connection,
				JMXVisualizer visualizer) {
			super();
			this.connection = connection;
			this.visualizer = visualizer;
		}

		public void run() {
			try {
				visualizer.addData(
					getMemoryUsage(connection).getUsed());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	private static void doGarbageCollection(MBeanServerConnection connection) throws Exception {
		ObjectName memoryMXBean = new ObjectName("java.lang:type=Memory");
		connection.invoke(memoryMXBean, "gc", null, null);
	}

	private static MemoryUsage getMemoryUsage(MBeanServerConnection connection) throws Exception {
		ObjectName heapObjName = new ObjectName("java.lang:type=Memory"); 
        MemoryUsage heapMemoryUsage = MemoryUsage.from((CompositeDataSupport) connection
        		.getAttribute(heapObjName, "HeapMemoryUsage")); 
        
        return heapMemoryUsage;
	}
}