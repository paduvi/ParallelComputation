package com.paduvi.stats;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import scala.Tuple2;

public class LineChartAWT extends ApplicationFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LineChartAWT(String applicationTitle, String chartTitle, Map<Integer, String> rawDataset) {
		super(applicationTitle);

		LogAxis xAxis = new LogAxis("Number of Elements");
		xAxis.setBase(10);
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		JFreeChart lineChart = ChartFactory.createXYLineChart(chartTitle, "Number of Elements", "Time (ms)",
				createDataset(rawDataset), PlotOrientation.VERTICAL, true, true, false);

		XYPlot plot = lineChart.getXYPlot();
		// plot.setRangeAxis(yAxis);
		plot.setDomainAxis(xAxis);

		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		for (int i = 0; i < 4; i++) {
			renderer.setSeriesShapesVisible(i, true);
		}

		File imageFile = new File("boyer-2.png");
		int width = 640;
		int height = 480;
		try {
			ChartUtilities.saveChartAsPNG(imageFile, lineChart, width, height);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ChartPanel chartPanel = new ChartPanel(lineChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
		setContentPane(chartPanel);
	}

	private XYSeriesCollection createDataset(Map<Integer, String> rawDataset) {
		XYSeries series1 = new XYSeries("Sequential");
		XYSeries series2 = new XYSeries("Parallel");
		for (Map.Entry<Integer, String> entry : rawDataset.entrySet()) {
			int k = entry.getKey();
			String v = entry.getValue();

			String[] parts = v.split(" ");
			double v1 = Double.parseDouble(parts[0]);
			double v2 = Double.parseDouble(parts[1]);
			series1.add(k, v1);
			series2.add(k, v2);
		}
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series1);
		dataset.addSeries(series2);
		return dataset;
	}

	public static void main(String[] args) {
		String master = "local[*]";

		SparkConf conf = new SparkConf().setAppName(LineChartAWT.class.getName()).setMaster(master);
		JavaSparkContext context = new JavaSparkContext(conf);
		Map<Integer, String> rawDataset = context.textFile("report/report-boyer2.txt").mapToPair(line -> {
			String[] parts = line.trim().split("\t");

			return new Tuple2<Integer, String>(Integer.parseInt(parts[1]), parts[3] + " " + parts[4]);
		}).reduceByKey((a, b) -> {
			double v1 = Double.parseDouble(a.split(" ")[1]);
			double v2 = Double.parseDouble(b.split(" ")[1]);
			return a.split(" ")[0] + " " + Math.min(v1, v2);
		}).collectAsMap();

		LineChartAWT chart = new LineChartAWT("Sequential vs Parallel", "Increasing in number of Elements", rawDataset);

		context.close();
		chart.pack();
		RefineryUtilities.centerFrameOnScreen(chart);
		chart.setVisible(true);
	}
}