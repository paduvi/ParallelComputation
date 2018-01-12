package com.paduvi.stats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

	public LineChartAWT(String applicationTitle, String chartTitle, List<Tuple2<Integer, String>> rawDataset) {
		super(applicationTitle);

		LogAxis xAxis = new LogAxis("Number of Elements");
		xAxis.setBase(10);
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		JFreeChart lineChart = ChartFactory.createXYLineChart(chartTitle, "Number of Elements", "Time (s)",
				createDataset(rawDataset), PlotOrientation.VERTICAL, true, true, false);

		XYPlot plot = lineChart.getXYPlot();
		// plot.setRangeAxis(yAxis);
		plot.setDomainAxis(xAxis);

		File imageFile = new File("boyer-3.png");
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

	private XYSeriesCollection createDataset(List<Tuple2<Integer, String>> rawDataset) {
		Map<Integer, XYSeries> mapSeries = new HashMap<>();
		for (Tuple2<Integer, String> tuple : rawDataset) {
			if (!mapSeries.containsKey(tuple._1)) {
				mapSeries.put(tuple._1, new XYSeries(tuple._1));
			}
			String[] parts = tuple._2.split(" ");
			int n = Integer.parseInt(parts[0]);
			double t = Double.parseDouble(parts[1]) / 10;
			mapSeries.get(tuple._1).add(n, t);
		}
		XYSeriesCollection dataset = new XYSeriesCollection();
		for (Map.Entry<Integer, XYSeries> entry : mapSeries.entrySet()) {
			dataset.addSeries(entry.getValue());
		}
		return dataset;
	}

	public static void main(String[] args) {
		String master = "local[*]";

		SparkConf conf = new SparkConf().setAppName(LineChartAWT.class.getName()).setMaster(master);
		JavaSparkContext context = new JavaSparkContext(conf);
		List<Tuple2<Integer, String>> rawDataset = context.textFile("report/report-boyer.txt").mapToPair(line -> {
			String[] parts = line.trim().split("\t");
			return new Tuple2<String, Double>(parts[0] + " " + parts[1], Double.parseDouble(parts[2]));
		}).reduceByKey((a, b) -> a + b).mapToPair(tuple -> {
			String[] parts = tuple._1.split(" ");
			return new Tuple2<Integer, String>(Integer.parseInt(parts[0]), parts[1] + " " + tuple._2.toString());
		}).collect();

		LineChartAWT chart = new LineChartAWT("Sequential vs Parallel", "Same Pattern size", rawDataset);

		context.close();
		chart.pack();
		RefineryUtilities.centerFrameOnScreen(chart);
		chart.setVisible(true);
	}
}