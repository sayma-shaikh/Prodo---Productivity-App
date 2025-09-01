package com.example.prodo.ui.stats; // Or your preferred package, e.g., com.example.prodo.data

import com.github.mikephil.charting.data.BarEntry;
import java.util.List;

public class BarChartData {
    private final List<BarEntry> entries;
    private final List<String> labels;

    public BarChartData(List<BarEntry> entries, List<String> labels) {
        this.entries = entries;
        this.labels = labels;
    }

    public List<BarEntry> getEntries() {
        return entries;
    }

    public List<String> getLabels() {
        return labels;
    }
}