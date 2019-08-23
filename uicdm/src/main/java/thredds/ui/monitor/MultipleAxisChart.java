/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ui.monitor;

import java.awt.*;
import javax.swing.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * This demo shows a time series chart that has multiple range axes.
 */
public class MultipleAxisChart extends JPanel {

  JFreeChart chart;

  /**
   * A demonstration application showing how to create a time series chart
   * with multiple axes.
   *
   * @param title1 the frame title.
   * @param xaxis1 xaxis title
   * @param yaxis1 yaxis title
   * @param series1 the data
   */
  public MultipleAxisChart(String title1, String xaxis1, String yaxis1, TimeSeries series1) {

    TimeSeriesCollection dataset1 = new TimeSeriesCollection();
    dataset1.addSeries(series1);

    chart = ChartFactory.createTimeSeriesChart(title1, xaxis1, yaxis1, dataset1, true, true, false);

    // chart.addSubtitle(new TextTitle("Four datasets and four range axes."));
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setOrientation(PlotOrientation.VERTICAL);
    plot.getRangeAxis().setFixedDimension(15.0);

    this.setLayout(new BorderLayout());
    this.add(new ChartPanel(chart), BorderLayout.CENTER);
  }

  private int axisNum = 1;

  public void addSeries(String yaxisName, TimeSeries series) {
    NumberAxis axis2 = new NumberAxis(yaxisName);
    axis2.setFixedDimension(10.0);
    axis2.setAutoRangeIncludesZero(false);

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setRangeAxis(axisNum, axis2);
    plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);

    TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries(series);

    plot.setDataset(axisNum, dataset);
    plot.mapDatasetToRangeAxis(axisNum, axisNum);
    XYItemRenderer renderer2 = new StandardXYItemRenderer();
    plot.setRenderer(axisNum, renderer2);

    axisNum++;
  }

  private static final Color[] colors = new Color[] {Color.black, Color.red, Color.blue, Color.green};

  public void finish(java.awt.Dimension preferredSize) {
    ChartUtilities.applyCurrentTheme(chart);

    XYPlot plot = (XYPlot) chart.getPlot();
    for (int i = 0; i < axisNum; i++) {
      XYItemRenderer renderer = plot.getRenderer(i);
      if (renderer == null)
        continue;

      renderer.setSeriesPaint(0, colors[i]);

      ValueAxis axis = plot.getRangeAxis(i);
      axis.setLabelPaint(colors[i]);
      axis.setTickLabelPaint(colors[i]);
    }

    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(preferredSize);
    chartPanel.setDomainZoomable(true);
    chartPanel.setRangeZoomable(true);
  }

}

