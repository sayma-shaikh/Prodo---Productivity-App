package com.example.prodo.ui.stats;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.List;

public class BarChartManager {

    private static final String TAG = "BarChartManager";
    private BarChart barChart;

    public BarChartManager(BarChart barChart) {
        if (barChart == null) {
            Log.e(TAG, "BarChart cannot be null in BarChartManager constructor!");
            return;
        }
        this.barChart = barChart;
    }

    public void setupBarChart() {
        if (barChart == null) {
            Log.e(TAG, "BarChart is null in setupBarChart! Cannot proceed.");
            return;
        }

        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setHighlightFullBarEnabled(false);

        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);

        Description chartDescription = new Description();
        chartDescription.setText("Weekly Pomodoro Focus (Hours)");
        chartDescription.setTextSize(12f);
        barChart.setDescription(chartDescription);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setCenterAxisLabels(false); // Bars will be centered on their x-position, not the label
        xAxis.setDrawLabels(true); // Ensure labels are drawn

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawLabels(true); // Ensure Y-axis labels are drawn

        barChart.getAxisRight().setEnabled(false);

        barChart.invalidate();
        Log.d(TAG, "BarChart basic setup complete.");
    }

    public void populateBarChart(List<BarEntry> entries, List<String> labels, String dataSetLabel) {
        if (barChart == null) {
            Log.e(TAG, "BarChart is null in populateBarChart! Cannot proceed.");
            return;
        }

        if (entries == null || entries.isEmpty()) {
            Log.w(TAG, "Entries list is null or empty. Clearing chart.");
            barChart.clear();
            barChart.setData(null);
            barChart.setNoDataText("No data available to display.");
            barChart.invalidate();
            return;
        }
        Log.d(TAG, "Populating BarChart with " + entries.size() + " entries.");

        BarDataSet dataSet = new BarDataSet(entries, dataSetLabel);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        // barData.setBarWidth(0.7f); // This might be overridden by setFitBars(true)

        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        if (labels != null && labels.size() == entries.size()) {
            // Set label count and then the formatter
            xAxis.setLabelCount(labels.size(), false); // <--- THIS IS THE CRUCIAL CHANGE
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            Log.d(TAG, "XAxis labels set with " + labels.size() + " labels.");
        } else {
            xAxis.setValueFormatter(null);
            if (labels == null) {
                Log.w(TAG, "Labels list is null. XAxis labels will be numeric.");
            } else {
                Log.w(TAG, "Labels size (" + labels.size() + ") does not match entries size (" + entries.size() + "). XAxis labels may not be set correctly.");
            }
        }

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        barChart.setFitBars(true);
        barChart.animateY(1200);
        barChart.invalidate();

        Log.d(TAG, "BarChart populated and invalidated successfully.");
    }
}