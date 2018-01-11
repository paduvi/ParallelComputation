import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
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

public class LineChartAWT extends ApplicationFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LineChartAWT(String applicationTitle, String chartTitle) {
		super(applicationTitle);

		LogAxis xAxis = new LogAxis("X");
		xAxis.setBase(10);
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(createDataset("QuickSort"));
		dataset.addSeries(createDataset("LinearMpiQuickSort"));
		dataset.addSeries(createDataset("RecursiveMpiQuickSort"));
		dataset.addSeries(createDataset("CombineMpiQuickSort"));

		JFreeChart lineChart = ChartFactory.createXYLineChart(chartTitle, "Number of Elements", "Time", dataset,
				PlotOrientation.VERTICAL, true, true, false);

		XYPlot plot = lineChart.getXYPlot();
		// plot.setRangeAxis(xAxis);
		plot.setDomainAxis(xAxis);

		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		for (int i = 0; i < 4; i++) {
			renderer.setSeriesShapesVisible(i, true);
		}

		ChartPanel chartPanel = new ChartPanel(lineChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
		setContentPane(chartPanel);
	}

	private XYSeries createDataset(String title) {
		XYSeries series = new XYSeries(title);
		int temp = 10;
		Random r = new Random();
		for (int i = 0; i < 10; i++) {
			series.add(temp, r.nextInt(30));
			temp *= 10;
		}
		return series;
	}

	public static void main(String[] args) {
		LineChartAWT chart = new LineChartAWT("Sequential vs Parallel", "Sequential vs Parallel (OpenMPI)");

		chart.pack();
		RefineryUtilities.centerFrameOnScreen(chart);
		chart.setVisible(true);
	}
}