package com.example.prodo.ui.stats;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.prodo.data.Task;
import com.example.prodo.data.TaskStore;
import com.github.mikephil.charting.data.BarEntry;

import com.example.prodo.data.TaskAnalyticItem; // Ensure this is your modified class


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
// Map import was removed as it's no longer directly used for taskPomos/taskMillis in the breakdown
// import java.util.Map;

public class StatsViewModel extends AndroidViewModel {

    private static final String TAG = "StatsViewModel";
    private final TaskStore taskStore;

    private final MutableLiveData<Integer> _completedTasksCount = new MutableLiveData<>(0);
    public final LiveData<Integer> completedTasksCount = _completedTasksCount;

    private final MutableLiveData<Integer> _pendingTasksCount = new MutableLiveData<>(0);
    public final LiveData<Integer> pendingTasksCount = _pendingTasksCount;

    private final MutableLiveData<List<TaskAnalyticItem>> _taskAnalyticsList = new MutableLiveData<>();
    public final LiveData<List<TaskAnalyticItem>> taskAnalyticsList = _taskAnalyticsList;

    private final MutableLiveData<BarChartData> _pomodoroBarData = new MutableLiveData<>();
    public final LiveData<BarChartData> pomodoroBarData = _pomodoroBarData;

    private final MutableLiveData<Integer> _totalPomodorosOverall = new MutableLiveData<>(0);
    public final LiveData<Integer> totalPomodorosOverall = _totalPomodorosOverall;

    private final MutableLiveData<Double> _totalTimeSpentHoursOverall = new MutableLiveData<>(0.0);
    public final LiveData<Double> totalTimeSpentHoursOverall = _totalTimeSpentHoursOverall;

    public enum Period { WEEK, MONTH, YEAR }

    private final MutableLiveData<Period> _period = new MutableLiveData<>(Period.WEEK);
    public final LiveData<Period> period = _period;

    private final MutableLiveData<Long> _anchorUtcMillis = new MutableLiveData<>(System.currentTimeMillis());
    public final LiveData<Long> anchorUtcMillis = _anchorUtcMillis;

    private final MutableLiveData<String> _periodLabel = new MutableLiveData<>("");
    public final LiveData<String> periodLabel = _periodLabel;

    public StatsViewModel(@NonNull Application application) {
        super(application);
        taskStore = TaskStore.get(application.getApplicationContext());
        Log.d(TAG, "ViewModel initialized. Triggering initial stats refresh.");
        triggerStatsRefresh();
    }

    // This method might not be directly used if colors are handled by adapter or not at all
    private int colorForTask(String taskTitle) {
        if (taskTitle == null) return Color.GRAY;
        int h = Math.abs(taskTitle.hashCode()) % 360;
        float[] hsv = new float[]{h, 0.45f, 0.95f};
        return Color.HSVToColor(hsv);
    }

    public void setPeriod(Period p) {
        Log.d(TAG, "setPeriod called with: " + p);
        if (_period.getValue() != p) {
            _period.setValue(p);
            triggerStatsRefresh();
        } else {
            Log.d(TAG, "Period is already " + p + ". No refresh triggered by setPeriod.");
        }
    }

    public void previousPeriod() {
        Log.d(TAG, "previousPeriod called");
        shiftAnchor(-1);
    }

    public void nextPeriod() {
        Log.d(TAG, "nextPeriod called");
        shiftAnchor(1);
    }

    private void shiftAnchor(int direction) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(_anchorUtcMillis.getValue() != null ? _anchorUtcMillis.getValue() : System.currentTimeMillis());
        Period p = _period.getValue() != null ? _period.getValue() : Period.WEEK;
        Log.d(TAG, "shiftAnchor: current period=" + p + ", direction=" + direction);
        switch (p) {
            case WEEK:  c.add(Calendar.WEEK_OF_YEAR, direction); break;
            case MONTH: c.add(Calendar.MONTH, direction); break;
            case YEAR:  c.add(Calendar.YEAR, direction); break;
        }
        _anchorUtcMillis.setValue(c.getTimeInMillis());
        triggerStatsRefresh();
    }

    private static class Range { long startMillis; long endMillis; }

    private Range currentRange() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        Long anchor = _anchorUtcMillis.getValue();
        if (anchor == null) anchor = System.currentTimeMillis();
        start.setTimeInMillis(anchor);
        end.setTimeInMillis(start.getTimeInMillis());

        Period p = _period.getValue() != null ? _period.getValue() : Period.WEEK;
        Log.d(TAG, "Calculating currentRange for period: " + p + " with anchor: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(anchor));

        switch (p) {
            case WEEK:
                start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
                end.setTimeInMillis(start.getTimeInMillis());
                end.add(Calendar.DAY_OF_YEAR, 7);
                break;
            case MONTH:
                start.set(Calendar.DAY_OF_MONTH, 1);
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
                end.setTimeInMillis(start.getTimeInMillis());
                end.add(Calendar.MONTH, 1);
                break;
            case YEAR:
                start.set(Calendar.DAY_OF_YEAR, 1);
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
                end.setTimeInMillis(start.getTimeInMillis());
                end.add(Calendar.YEAR, 1);
                break;
        }
        Range r = new Range();
        r.startMillis = start.getTimeInMillis();
        r.endMillis = end.getTimeInMillis();
        Log.d(TAG, "currentRange calculated: Start=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(r.startMillis) +
                ", End=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(r.endMillis));
        return r;
    }

    private void updatePeriodLabel(Range r) {
        if (r == null) {
            Log.e(TAG, "Cannot update period label, range is null.");
            _periodLabel.setValue("Error: Date range not set");
            return;
        }
        SimpleDateFormat dfW = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        Period p = _period.getValue() != null ? _period.getValue() : Period.WEEK;
        String label;
        if (p == Period.WEEK) {
            Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(r.startMillis);
            Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(r.endMillis - 1);
            label = dfW.format(c1.getTime()) + " ~ " + dfW.format(c2.getTime());
        } else if (p == Period.MONTH) {
            SimpleDateFormat m = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            label = m.format(new java.util.Date(r.startMillis));
        } else { // YEAR
            SimpleDateFormat y = new SimpleDateFormat("yyyy", Locale.getDefault());
            label = y.format(new java.util.Date(r.startMillis));
        }
        Log.d(TAG, "Updating period label to: " + label);
        _periodLabel.setValue(label);
    }


    public void triggerStatsRefresh() {
        Log.d(TAG, "triggerStatsRefresh called");

        List<Task> allTasks = taskStore.getTasks();
        if (allTasks == null) {
            Log.w(TAG, "TaskStore.getTasks() returned null. Initializing to empty list.");
            allTasks = new ArrayList<>();
        }
        Log.d(TAG, "Number of allTasks from TaskStore: " + allTasks.size());

        int completed = 0, pending = 0, totalPomosOverall = 0;
        long totalMillisOverall = 0;
        for (Task t : allTasks) {
            if (t == null) {
                Log.w(TAG, "Encountered a null task in allTasks list.");
                continue;
            }
            if (t.isDone()) completed++; else pending++;
            totalPomosOverall += t.getPomodoroCount();
            totalMillisOverall += t.getTotalTimeSpentMillis();
        }
        _completedTasksCount.setValue(completed);
        _pendingTasksCount.setValue(pending);
        _totalPomodorosOverall.setValue(totalPomosOverall);
        _totalTimeSpentHoursOverall.setValue(totalMillisOverall / 3600000.0);
        Log.d(TAG, "Overall stats: Completed=" + completed + ", Pending=" + pending + ", TotalPomos=" + totalPomosOverall);

        Range r = currentRange();
        updatePeriodLabel(r);

        SharedPreferences prefs = getApplication().getSharedPreferences("ProdoStats", Context.MODE_PRIVATE);
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> barLabels = new ArrayList<>();
        SimpleDateFormat keyDf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // For SharedPreferences keys
        SimpleDateFormat labelDf = new SimpleDateFormat("EEE", Locale.getDefault()); // For BarChart X-axis labels

        Calendar iter = Calendar.getInstance();
        if (r != null && r.startMillis != 0) {
            iter.setTimeInMillis(r.startMillis);
        } else {
            Log.e(TAG, "Date range 'r' is null or startMillis is 0 for BarChart. Defaulting to current time.");
            iter.setTimeInMillis(System.currentTimeMillis());
        }

        int index = 0;
        while (r!= null && iter.getTimeInMillis() < r.endMillis) {
            String dateKeyForTotalPomos = keyDf.format(iter.getTime()); // e.g., "2023-01-15"
            int pomos = prefs.getInt("pomos_" + dateKeyForTotalPomos, 0); // Key for daily *total* pomos
            float hours = pomos * (25f / 60f); // Assuming 25 min pomodoros
            barEntries.add(new com.github.mikephil.charting.data.BarEntry(index, hours));
            barLabels.add(labelDf.format(iter.getTime())); // e.g., "Mon"
            iter.add(Calendar.DAY_OF_YEAR, 1);
            index++;
        }
        _pomodoroBarData.setValue(new BarChartData(barEntries, barLabels));
        Log.d(TAG, "Bar chart data prepared. Entries: " + barEntries.size());

        // Build Task Breakdown: Items per task, per day
        List<TaskAnalyticItem> breakdown = new ArrayList<>();
        SimpleDateFormat displayDateFormatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); // For display in list

        Calendar walk = Calendar.getInstance(); // Calendar to iterate through days in the period
        if (r != null && r.startMillis != 0) {
            walk.setTimeInMillis(r.startMillis);
            Log.d(TAG, "Starting task breakdown scan from: " + keyDf.format(walk.getTime()) + " to " + keyDf.format(r.endMillis-1));
        } else {
            Log.e(TAG, "Date range 'r' is null or startMillis is 0. Cannot scan for task breakdown.");
            _taskAnalyticsList.setValue(new ArrayList<>()); // Set empty list and avoid further processing
            Log.d(TAG, "Final stats refreshed log after setting empty task breakdown.");
            return; // Exit if range is bad
        }

        while (walk.getTimeInMillis() < r.endMillis) {
            String currentDateKeyPart = keyDf.format(walk.getTime()); // "yyyy-MM-dd" for SharedPreferences key
            String displayDate = displayDateFormatter.format(walk.getTime()); // "dd MMM yyyy" for display in TaskAnalyticItem

            Log.v(TAG, "Scanning for tasks on day: " + currentDateKeyPart);

            if (allTasks.isEmpty()) {
                Log.v(TAG, "allTasks list is empty, skipping prefs scan for day " + currentDateKeyPart);
            } else {
                for (Task t : allTasks) {
                    if (t == null || t.getTitle()  == null || t.getTitle().trim().isEmpty()) {
                        Log.w(TAG, "Skipping a null task or task with null/empty title for day " + currentDateKeyPart);
                        continue;
                    }
                    String taskTitle = t.getTitle();
                    String taskSafeKey = safeKey(taskTitle);
                    String fullPrefsKey = ("pomos_" + currentDateKeyPart + "_" + taskSafeKey); // Key for task-specific pomos on a given day
                    Log.v(TAG, "Checking SharedPreferences for key: " + fullPrefsKey);

                    int pomosForTaskOnDate = prefs.getInt(fullPrefsKey, 0);
                    if (pomosForTaskOnDate > 0) {
                        Log.i(TAG, "Found " + pomosForTaskOnDate + " pomos for task '" + taskTitle + "' (key: " + fullPrefsKey + ") on date " + currentDateKeyPart);
                        long millisForTaskOnDate = (long) (pomosForTaskOnDate * 25L * 60L * 1000L); // Assuming 25 min pomos

                        // ---- THIS IS THE CORRECTED CONSTRUCTOR CALL ----
                        TaskAnalyticItem item = new TaskAnalyticItem(
                                taskTitle,
                                pomosForTaskOnDate,
                                millisForTaskOnDate,
                                displayDate // Pass the formatted date string
                        );
                        // ----------------------------------------------
                        // If your TaskAnalyticItem needs a color, and you have a color field/setter:
                        // item.setTaskColor(colorForTask(taskTitle)); // Example
                        breakdown.add(item);
                        Log.d(TAG, "Added to breakdown: Title=" + taskTitle + ", Pomos=" + pomosForTaskOnDate + ", Millis=" + millisForTaskOnDate + ", Date=" + displayDate);
                    } else {
                        Log.v(TAG, "No pomos (or 0) found for key: " + fullPrefsKey);
                    }
                }
            }
            walk.add(Calendar.DAY_OF_YEAR, 1);
        }
        Log.d(TAG, "Finished scanning days for task breakdown. Total items created: " + breakdown.size());

        _taskAnalyticsList.setValue(breakdown);
        Log.d(TAG, "Stats refreshed for period. Task breakdown items: " + breakdown.size());
    }

    private String safeKey(String title) {
        if (title == null || title.trim().isEmpty()) {
            Log.w(TAG, "safeKey received null or empty title, returning 'untitled'");
            return "untitled";
        }
        return title.trim().toLowerCase(Locale.getDefault()).replaceAll("[^a-z0-9_.-]+", "_");
    }
}
