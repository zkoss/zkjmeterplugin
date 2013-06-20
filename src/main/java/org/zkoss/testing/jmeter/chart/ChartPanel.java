package org.zkoss.testing.jmeter.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.math.BigDecimal;
import java.util.Map;

import javax.swing.JPanel;

import org.jCharts.axisChart.AxisChart;
import org.jCharts.axisChart.customRenderers.axisValue.renderers.ValueLabelPosition;
import org.jCharts.axisChart.customRenderers.axisValue.renderers.ValueLabelRenderer;
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

public class ChartPanel extends JPanel {
	
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

		
		ValueLabelRenderer valueLabelRenderer = 
			new ValueLabelRenderer( false, false, true, -1 );
		valueLabelRenderer.setValueLabelPosition( ValueLabelPosition.ON_TOP );
		valueLabelRenderer.useVerticalLabels( false );
		lineChartProperties.addPostRenderEventListener( valueLabelRenderer );
		
		
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
//            legendProperties.setPlacement(legendPlacement);
            legendProperties.setIconBorderPaint(Color.WHITE);
            legendProperties.setIconBorderStroke(
            		new BasicStroke(0f, BasicStroke.CAP_SQUARE, BasicStroke.CAP_SQUARE));
          
            // Manage legend placement
            legendProperties.setNumColumns(LegendAreaProperties.COLUMNS_FIT_TO_IMAGE);
            
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
