package org.zkoss.testing.jmeter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
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
import org.apache.jmeter.visualizers.RespTimeGraphChart;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;
import org.apache.jmeter.visualizers.utils.Colors;
import org.apache.jorphan.gui.JLabeledTextField;
import org.jCharts.axisChart.AxisChart;
import org.jCharts.chartData.AxisChartDataSet;
import org.jCharts.chartData.ChartDataException;
import org.jCharts.chartData.DataSeries;
import org.jCharts.properties.AxisProperties;
import org.jCharts.properties.ChartProperties;
import org.jCharts.properties.DataAxisProperties;
import org.jCharts.properties.LabelAxisProperties;
import org.jCharts.properties.LegendAreaProperties;
import org.jCharts.properties.LegendProperties;
import org.jCharts.properties.LineChartProperties;
import org.jCharts.properties.PointChartProperties;
import org.jCharts.properties.PropertyException;
import org.jCharts.types.ChartType;

public class JMXVisualizer extends AbstractVisualizer {
	private static final long serialVersionUID = 7906471251449301718L;


	private ChartPanel graphPanel;

	private static final int DEFAULT_WIDTH = 400;
	private static final int DEFAULT_HEIGTH = 300;
	private static final int INTERVAL_DEFAULT = 500;
	private int intervalValue = INTERVAL_DEFAULT;

	private final JLabeledTextField serverIPField = new JLabeledTextField(
			"Server IP: ", 15);

	private final JLabeledTextField jmxPortField = new JLabeledTextField(
			"JMX Port: ", 6);

	private final JCheckBox doGC = new JCheckBox("GC");

	private final JLabeledTextField intervalField = new JLabeledTextField(
			"Interval (ms): ", 10);

	
	private Map<String, Long> result = new LinkedHashMap<String, Long>();
	private DateFormat dfmt = new SimpleDateFormat("HH:mm:ss.SSS");
	private Timer timer;
	
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

		// SortFilterModel mySortedModel =
		// new SortFilterModel(myStatTableModel);

		// myJTable = new JTable(new listm);
		// myJTable.getTableHeader().setDefaultRenderer(new
		// HeaderAsPropertyRenderer());
		// myJTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
		// RendererUtils.applyRenderers(myJTable, RENDERERS);
		//
		// myJTextPane = new JTextPane();
		// myScrollPane = new JScrollPane(myJTextPane);
		// this.add(mainPanel, BorderLayout.NORTH);
		// this.add(myScrollPane, BorderLayout.CENTER);
		// saveTable.addActionListener(this);
		// JPanel opts = new JPanel();
		// opts.add(useGroupName, BorderLayout.WEST);
		// opts.add(saveTable, BorderLayout.CENTER);
		// opts.add(saveHeaders, BorderLayout.EAST);
		// this.add(opts,BorderLayout.SOUTH);
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
				
				connector = startMonitorHeap();
			}

			public void testEnded(String host) {
				super.testEnded(host);

				if (!"*local*".equals(host))
					return;

				JMeterUtils.runSafe(new Runnable() {

					public void run() {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						
						if (timer != null) {
							timer.cancel();
							timer = null;
							MBeanServerConnection connection;
							try {
								connection = connector.getMBeanServerConnection();
								if (doGC.isSelected())
									doGarbageCollection(connection);
										
								addData(getMemoryUsage(connection).getUsed());
								
								
							} catch (IOException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								if (connector != null)
									try {
										connector.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
							}
							
							
							
							makeGraph();
						}
					}
					
				 });
				
				
			}
		});
		return super.createTestElement();
	}
	
	private JMXConnector startMonitorHeap() {
		String ipText = serverIPField.getText();
		String portText = jmxPortField.getText();
		String intervalText = intervalField.getText();
		
		if (isEmpty(ipText) || isEmpty(ipText) || isEmpty(ipText) )
			return null;
		
		int port = 0;
		int interval = 0;
		try {
			port = Integer.parseInt(portText);
			interval = Integer.parseInt(intervalText);
		} catch (NumberFormatException e) {
			return null;
		}
		
		try {
			JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(
					"service:jmx:rmi:///jndi/rmi://"+ipText+":"+port+"/jmxrmi"), null);
			
			result.clear();
			
			
			timer = new Timer();
			MBeanServerConnection connection = connector.getMBeanServerConnection();
			if (doGC.isSelected())
				doGarbageCollection(connection);
					
			addData(getMemoryUsage(connection).getUsed());
			timer.schedule(new JMXTask(connection, this), 0, interval);
			
			return connector;
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private boolean isEmpty(String ipText) {
		return ipText == null || ipText.trim().length() == 0;
	}

	public String getLabelResource() {
		return "jmx_visualizer_title";
	}

	@Override
	public String getStaticLabel() {
		return "JMX Visualizer";
	}

	private void makeGraph() {
		Dimension size = graphPanel.getSize();
		// canvas size
		int width = (int) size.getWidth();
		int height = (int) size.getHeight();


		// graphPanel.setShowGrouping(false);
		graphPanel.setHeight(height);
		graphPanel.setWidth(width);
		// Draw the graph
		
		graphPanel.setResult(result);
		graphPanel.repaint();

	}
	
	public void addData(long used) {
		result.put(dfmt.format(new Date()), used / 1024);
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
					MemoryUsage heapMemoryUsage = getMemoryUsage(connection);
					visualizer.addData(heapMemoryUsage.getUsed());
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

	private static class ChartPanel extends JPanel {
		private Map<String, Long> result;
		private int width;
		private int height;
		
		public void setResult(Map<String, Long> result) {
			this.result = result;
		}
		
		private double[][] getData() {
			double[][] data = new double[1][result.size()];
			int i = 0;
			for (Long d : result.values()) {
				data[0][i] = d.doubleValue();
				i++;
			}
			return data;
		}

		@Override
		public void paintComponent(Graphics g) {
			if (result == null)
				return;

			this.setPreferredSize(new Dimension(width, height));

			DataSeries dataSeries = new DataSeries(result.keySet().toArray(
					new String[result.size()]), null,
					"Memory Usage (K bytes)", "Heap");

			LineChartProperties lineChartProperties = new LineChartProperties(
					new Stroke[] { new BasicStroke(3.5f, BasicStroke.CAP_ROUND,
							BasicStroke.JOIN_ROUND, 5f) },
					new Shape[] { PointChartProperties.SHAPE_CIRCLE });

			double[][] data = getData();
			try {
				
				
				// Define chart type (line)
				AxisChartDataSet axisChartDataSet = new AxisChartDataSet(
					data, new String[] { "Used heap" },
					new Paint[] { Color.BLUE }, ChartType.LINE,
					lineChartProperties);
				
				dataSeries.addIAxisPlotDataSet(axisChartDataSet);
				ChartProperties chartProperties = new ChartProperties();
				LabelAxisProperties xaxis = new LabelAxisProperties();
				DataAxisProperties yaxis = new DataAxisProperties();
				yaxis.setUseCommas(true);
				
				// Y Axis ruler
				double max = findMax(data);
				int period = 5000;
				
				BigDecimal round = new BigDecimal(max  / period);
                round = round.setScale(0, BigDecimal.ROUND_UP);
                double topValue = round.doubleValue() * period;
                yaxis.setUserDefinedScale(0, period);
                yaxis.setNumItems((int) (topValue / period)+1);
                yaxis.setShowGridLines(1);

	            AxisProperties axisProperties= new AxisProperties(xaxis, yaxis);
	            axisProperties.setXAxisLabelsAreVertical(true);
	            LegendProperties legendProperties= new LegendProperties();
	            legendProperties.setBorderStroke(null);
//	            legendProperties.setPlacement(legendPlacement);
	            legendProperties.setIconBorderPaint(Color.WHITE);
	            legendProperties.setIconBorderStroke(
	            		new BasicStroke(0f, BasicStroke.CAP_SQUARE, BasicStroke.CAP_SQUARE));
	            // Manage legend placement
	          
	            // Manage legend placement
	            legendProperties.setNumColumns(LegendAreaProperties.COLUMNS_FIT_TO_IMAGE);
//	            if (legendPlacement == LegendAreaProperties.RIGHT || legendPlacement == LegendAreaProperties.LEFT) {
//	                legendProperties.setNumColumns(1);
//	            }
//	            if (legendFont != null) {
//	                legendProperties.setFont(legendFont);
//	            }
	            AxisChart axisChart = new AxisChart(
	                    dataSeries, chartProperties, axisProperties,
	                    legendProperties, width, height);
	            axisChart.setGraphics2D((Graphics2D) g);
	            axisChart.render();
	            
			} catch (ChartDataException e) {
				e.printStackTrace();
			} catch (PropertyException e) {
				e.printStackTrace();
			}
		}
		
		public void setWidth(int w) {
			this.width = w;
		}

		public void setHeight(int h) {
			this.height = h;
		}

		private double findMax(double[][] datas) {
			double max = 0;
	        for (int i = 0; i < datas.length; i++) {
	            for (int j = 0; j < datas[i].length; j++) {
	                final double value = datas[i][j]; 
	                if ((!Double.isNaN(value)) && (value > max)) {
	                    max = value;
	                }
	            }
	        }
	        return max;
		}

	}
	
}
