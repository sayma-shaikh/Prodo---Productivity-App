package com.example.prodo.ui.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
// Import ImageButton if you are using it, or ensure 'View' is sufficient for findViewById
// import android.widget.ImageButton;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prodo.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.chip.Chip; // Correct import for Chip

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";

    private StatsViewModel statsViewModel;

    private TextView tvCompletedCard, tvPendingCard;
    private TextView tvTotalPomodoros, tvTotalTimeSpent;
    private RecyclerView recyclerViewTaskAnalytics;
    private TaskAnalyticsAdapter taskAnalyticsAdapter;

    private PieChart pieChartTasks;
    private BarChart barChartPomodoro;
    private BarChartManager barChartManager;

    // --- UI elements for period selection and navigation ---
    private View btnPrevPeriod, btnNextPeriod; // Using View for flexibility with ImageButton
    private TextView tvPeriodLabel;
    private Chip chipWeek, chipMonth, chipYear;


    public StatsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statsViewModel = new ViewModelProvider(requireActivity()).get(StatsViewModel.class);
        Log.d(TAG, "onCreate called, ViewModel initialized");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View root = inflater.inflate(R.layout.fragment_stats, container, false);

        // Initialize existing UI elements based on the provided XML
        tvCompletedCard = root.findViewById(R.id.tvCompletedCard);
        tvPendingCard = root.findViewById(R.id.tvPendingCard);
        tvTotalPomodoros = root.findViewById(R.id.tvTotalPomodoros);
        tvTotalTimeSpent = root.findViewById(R.id.tvTotalTimeSpent);
        recyclerViewTaskAnalytics = root.findViewById(R.id.rvTaskAnalytics);
        pieChartTasks = root.findViewById(R.id.pieChartTasks);
        barChartPomodoro = root.findViewById(R.id.barChartPomodoro);

        // --- Initialize UI controls for period and date navigation from XML ---
        btnPrevPeriod = root.findViewById(R.id.btnPrevPeriod);
        btnNextPeriod = root.findViewById(R.id.btnNextPeriod);
        tvPeriodLabel = root.findViewById(R.id.tvPeriodLabel);
        chipWeek = root.findViewById(R.id.chipWeek);
        chipMonth = root.findViewById(R.id.chipMonth);
        chipYear = root.findViewById(R.id.chipYear);

        // Default selection for chips
        if (chipWeek != null) {
            chipWeek.setChecked(true);
        } else {
            Log.e(TAG, "chipWeek is null. Cannot set default checked state.");
        }

        // --- Listeners for period navigation ---
        if (btnPrevPeriod != null) {
            btnPrevPeriod.setOnClickListener(v -> {
                if (statsViewModel != null) {
                    statsViewModel.previousPeriod();
                } else {
                    Log.e(TAG, "btnPrevPeriod clicked but statsViewModel is null.");
                }
            });
        } else {
            Log.e(TAG, "btnPrevPeriod is null. Cannot set OnClickListener.");
        }

        if (btnNextPeriod != null) {
            btnNextPeriod.setOnClickListener(v -> {
                if (statsViewModel != null) {
                    statsViewModel.nextPeriod();
                } else {
                    Log.e(TAG, "btnNextPeriod clicked but statsViewModel is null.");
                }
            });
        } else {
            Log.e(TAG, "btnNextPeriod is null. Cannot set OnClickListener.");
        }

        // --- Listeners for period selection chips ---
        if (chipWeek != null) {
            chipWeek.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (statsViewModel != null) {
                        Log.d(TAG, "ChipWeek checked, setting period to WEEK");
                        statsViewModel.setPeriod(StatsViewModel.Period.WEEK);
                    } else {
                        Log.e(TAG, "chipWeek checked but statsViewModel is null.");
                    }
                }
            });
        } else {
            Log.e(TAG, "chipWeek is null. Cannot set OnCheckedChangeListener.");
        }

        if (chipMonth != null) {
            chipMonth.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (statsViewModel != null) {
                        Log.d(TAG, "ChipMonth checked, setting period to MONTH");
                        statsViewModel.setPeriod(StatsViewModel.Period.MONTH);
                    } else {
                        Log.e(TAG, "chipMonth checked but statsViewModel is null.");
                    }
                }
            });
        } else {
            Log.e(TAG, "chipMonth is null. Cannot set OnCheckedChangeListener.");
        }

        if (chipYear != null) {
            chipYear.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (statsViewModel != null) {
                        Log.d(TAG, "ChipYear checked, setting period to YEAR");
                        statsViewModel.setPeriod(StatsViewModel.Period.YEAR);
                    } else {
                        Log.e(TAG, "chipYear checked but statsViewModel is null.");
                    }
                }
            });
        } else {
            Log.e(TAG, "chipYear is null. Cannot set OnCheckedChangeListener.");
        }
        Log.d(TAG, "Period toggle and navigation listeners set up.");


        // Setup for existing charts and RecyclerView
        setupRecyclerView();
        setupPieChart();

        if (barChartPomodoro != null) {
            barChartManager = new BarChartManager(barChartPomodoro);
            barChartManager.setupBarChart();
            Log.d(TAG, "BarChartManager initialized and bar chart setup called.");
        } else {
            Log.e(TAG, "barChartPomodoro view is null! BarChartManager cannot be initialized.");
        }

        observeViewModel();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // If chipWeek is checked by default and its listener calls setPeriod->triggerStatsRefresh,
        // an explicit call here might be redundant or cause a double load.
        // This ensures data loads if the default chip state doesn't trigger it.
        if (statsViewModel != null && chipWeek != null && chipWeek.isChecked()) {
            // Check if viewmodel already loaded for this period to avoid double load.
            // This might require a flag in ViewModel or checking if LiveData already has fresh data.
            // For now, let's assume the ViewModel handles redundant calls gracefully or
            // setPeriod itself is the primary trigger.
            Log.d(TAG, "onViewCreated: Default period (WEEK) is set.");
            // statsViewModel.triggerStatsRefresh(); // Or rely on setPeriod to do this.
            // If chipWeek.setChecked(true) in onCreateView
            // already triggered setPeriod, this might be a double call.
            // The ViewModel should ideally fetch data when setPeriod is called.
        }
    }

    private void setupRecyclerView() {
        if (recyclerViewTaskAnalytics == null) {
            Log.e(TAG, "RecyclerView recyclerViewTaskAnalytics is null in setupRecyclerView!");
            return;
        }
        recyclerViewTaskAnalytics.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAnalyticsAdapter = new TaskAnalyticsAdapter(); // You need to have this adapter created
        recyclerViewTaskAnalytics.setAdapter(taskAnalyticsAdapter);
        Log.d(TAG, "RecyclerView setup complete.");
    }

    private void setupPieChart() {
        if (pieChartTasks == null) {
            Log.e(TAG, "PieChart pieChartTasks is null in setupPieChart!");
            return;
        }
        pieChartTasks.setUsePercentValues(true);
        Description pieDesc = new Description();
        pieDesc.setText("Task Status"); // Example description
        pieChartTasks.setDescription(pieDesc);
        pieChartTasks.setDrawHoleEnabled(true);
        pieChartTasks.setHoleColor(Color.WHITE);
        pieChartTasks.setTransparentCircleRadius(61f);
        pieChartTasks.setEntryLabelColor(Color.BLACK);
        pieChartTasks.setEntryLabelTextSize(12f);
        Log.d(TAG, "PieChart setup complete.");
    }

    private void observeViewModel() {
        if (statsViewModel == null) {
            Log.e(TAG, "observeViewModel: statsViewModel is null! Cannot observe LiveData.");
            return;
        }
        Log.d(TAG, "Setting up ViewModel observers.");

        statsViewModel.completedTasksCount.observe(getViewLifecycleOwner(), count -> {
            Log.d(TAG, "Completed tasks count updated: " + count);
            if (tvCompletedCard != null) tvCompletedCard.setText(String.valueOf(count != null ? count : 0));
            updatePieChartFromViewModel();
        });

        statsViewModel.pendingTasksCount.observe(getViewLifecycleOwner(), count -> {
            Log.d(TAG, "Pending tasks count updated: " + count);
            if (tvPendingCard != null) tvPendingCard.setText(String.valueOf(count != null ? count : 0));
            updatePieChartFromViewModel();
        });

        // Inside StatsFragment.java, within observeViewModel() method

        statsViewModel.taskAnalyticsList.observe(getViewLifecycleOwner(), analyticsItems -> {
            // MODIFIED/ADDED LOGGING HERE:
            if (analyticsItems == null) {
                Log.d(TAG, "Task analytics list updated: NULL list received.");
            } else {
                Log.d(TAG, "Task analytics list updated. Item count: " + analyticsItems.size());
                if (analyticsItems.isEmpty()) {
                    Log.d(TAG, "Task analytics list is empty.");
                } else {
                    // Optional: Log details of the first item to see if data is as expected
                    // TaskAnalyticItem firstItem = analyticsItems.get(0);
                    // Log.d(TAG, "First item title: " + firstItem.getTaskTitle() + ", Pomos: " + firstItem.getPomodoroCount());
                }
            }

            if (taskAnalyticsAdapter != null) {
                taskAnalyticsAdapter.submitList(analyticsItems != null ? new ArrayList<>(analyticsItems) : new ArrayList<>());
            } else {
                Log.e(TAG, "taskAnalyticsAdapter is null in observeViewModel for taskAnalyticsList.");
            }
        });

        statsViewModel.pomodoroBarData.observe(getViewLifecycleOwner(), barChartDataWrapper -> {
            Log.d(TAG, "Pomodoro bar data updated from ViewModel.");
            if (barChartManager != null && barChartDataWrapper != null) {
                String dataSetLabel = "Focus Hours";
                if (barChartDataWrapper.getEntries() != null && !barChartDataWrapper.getEntries().isEmpty() &&
                        barChartDataWrapper.getLabels() != null && !barChartDataWrapper.getLabels().isEmpty() &&
                        barChartDataWrapper.getEntries().size() == barChartDataWrapper.getLabels().size()) {
                    Log.d(TAG, "Populating bar chart with actual data. Entries: " + barChartDataWrapper.getEntries().size() + ", Labels: " + barChartDataWrapper.getLabels().size());
                    barChartManager.populateBarChart(barChartDataWrapper.getEntries(), barChartDataWrapper.getLabels(), dataSetLabel);
                } else {
                    Log.w(TAG, "Bar chart data from ViewModel is null, empty, or inconsistent. Displaying 'No data available'.");
                    barChartManager.populateBarChart(new ArrayList<>(), new ArrayList<>(), dataSetLabel); // Show empty chart
                }
            } else {
                Log.e(TAG, "barChartManager or barChartDataWrapper is null in observeViewModel for pomodoroBarData. Cannot populate chart.");
            }
        });

        statsViewModel.totalPomodorosOverall.observe(getViewLifecycleOwner(), count -> {
            Log.d(TAG, "Total Pomodoros overall updated: " + count);
            if (tvTotalPomodoros != null) {
                tvTotalPomodoros.setText(String.format(Locale.getDefault(), "Total Pomodoros: %d", count != null ? count : 0));
            }
        });

        statsViewModel.totalTimeSpentHoursOverall.observe(getViewLifecycleOwner(), hours -> {
            Log.d(TAG, "Total time spent overall updated: " + hours);
            if (tvTotalTimeSpent != null) {
                tvTotalTimeSpent.setText(String.format(Locale.getDefault(), "Total Time Spent: %.2f hours", hours != null ? hours : 0.0));
            }
        });

        statsViewModel.periodLabel.observe(getViewLifecycleOwner(), s -> {
            Log.d(TAG, "Period label updated from ViewModel: " + s);
            if (tvPeriodLabel != null) {
                tvPeriodLabel.setText(s);
            } else {
                Log.e(TAG, "tvPeriodLabel is null. Cannot set period label text.");
            }
        });
    }

    private void updatePieChartFromViewModel() {
        if (statsViewModel == null || pieChartTasks == null) {
            Log.e(TAG, "Cannot update pie chart, ViewModel or PieChart view is null.");
            return;
        }
        Integer completed = statsViewModel.completedTasksCount.getValue();
        Integer pending = statsViewModel.pendingTasksCount.getValue();
        Log.d(TAG, "Updating PieChart. Completed: " + completed + ", Pending: " + pending);

        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        if (completed != null && completed > 0) {
            pieEntries.add(new PieEntry(completed.floatValue(), "Completed"));
        }
        if (pending != null && pending > 0) {
            pieEntries.add(new PieEntry(pending.floatValue(), "Pending"));
        }
        populatePieChart(pieEntries);
    }

    private void populatePieChart(List<PieEntry> entries) {
        if (pieChartTasks == null) {
            Log.e(TAG, "PieChart pieChartTasks is null in populatePieChart!");
            return;
        }
        if (entries == null || entries.isEmpty()) {
            Log.w(TAG, "Pie chart entries are null or empty. Clearing chart.");
            pieChartTasks.clear();
            pieChartTasks.setData(null); // Important to clear old data object
            pieChartTasks.setNoDataText("No task data available.");
            pieChartTasks.invalidate();
            return;
        }

        Log.d(TAG, "Populating PieChart with " + entries.size() + " entries.");
        PieDataSet dataSet = new PieDataSet(entries, ""); // Label for dataset, often not shown if only one.
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS); // Use a predefined color scheme
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(2f); // Space between pie slices

        PieData data = new PieData(dataSet);
        pieChartTasks.setData(data);
        pieChartTasks.animateY(1000); // Animate the chart
        pieChartTasks.invalidate(); // Refresh the chart
        Log.d(TAG, "PieChart populated and invalidated.");
    }
}






