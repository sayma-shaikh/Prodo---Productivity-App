package com.example.prodo.ui.calender;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prodo.R;
import com.example.prodo.data.Task;
import com.example.prodo.data.TaskStore;
import com.example.prodo.ui.tasks.TaskAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarFragment extends Fragment implements TaskAdapter.Listener {

    private static final String TAG = "CalendarFragment";

    private CalendarView calendarView;
    private RecyclerView tasksRecyclerView;
    private TextView noTasksTextView;
    private TaskAdapter tasksAdapter;
    private TaskStore taskStore;

    private long currentlySelectedDateMillis = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskStore = TaskStore.get(requireActivity().getApplicationContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendar_view);
        tasksRecyclerView = view.findViewById(R.id.recycler_view_tasks_for_date);
        noTasksTextView = view.findViewById(R.id.text_no_tasks);

        setupRecyclerView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            long selectedMillis = calendar.getTimeInMillis();
            Log.d("CalendarFragment", "Date selected in CalendarView: " + year + "-" + (month + 1) + "-" + dayOfMonth + " (Millis: " + selectedMillis + ")");
            updateTasksForDate(selectedMillis);
        });

        // Initial load for the current date
        if (currentlySelectedDateMillis == -1) {
            Calendar today = Calendar.getInstance();
            today.setTimeInMillis(calendarView.getDate());
            currentlySelectedDateMillis = today.getTimeInMillis();
        }
        updateTasksForDate(currentlySelectedDateMillis);
    }

    private void setupRecyclerView() {
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksAdapter = new TaskAdapter(this);
        tasksRecyclerView.setAdapter(tasksAdapter);
    }

    private void updateTasksForDate(long selectedDateMillis) {
        List<Task> tasks = getTasksForDateInternal(selectedDateMillis);
        Log.d(TAG, "Tasks found for selected date: " + tasks.size());

        if (!tasks.isEmpty()) {
            tasksAdapter.submitList(tasks);
            tasksRecyclerView.setVisibility(View.VISIBLE);
            noTasksTextView.setVisibility(View.GONE);
        } else {
            tasksAdapter.submitList(new ArrayList<>());
            tasksRecyclerView.setVisibility(View.GONE);
            noTasksTextView.setVisibility(View.VISIBLE);
        }
    }

    private List<Task> getTasksForDateInternal(long selectedDateMillis) {
        List<Task> allTasks = taskStore.getTasks();
        List<Task> tasksForDate = new ArrayList<>();

        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTimeInMillis(selectedDateMillis);
        selectedCal.set(Calendar.HOUR_OF_DAY, 0);
        selectedCal.set(Calendar.MINUTE, 0);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);
        long startOfDayMillis = selectedCal.getTimeInMillis();

        selectedCal.add(Calendar.DAY_OF_YEAR, 1);
        long endOfDayMillis = selectedCal.getTimeInMillis();

        Log.d(TAG, "Filtering tasks for date range: " +
                new Date(startOfDayMillis) + " (inclusive) to " +
                new Date(endOfDayMillis) + " (exclusive)");

        for (Task task : allTasks) {
            long taskDateMillis = task.getDate();
            if (taskDateMillis != 0) {
                if (taskDateMillis >= startOfDayMillis && taskDateMillis < endOfDayMillis) {
                    tasksForDate.add(task);
                    Log.d(TAG, "  MATCH: Task '" + task.getTitle() + "' with date " + new Date(taskDateMillis));
                } else {
                    Log.d(TAG, "  NO MATCH: Task '" + task.getTitle() + "' with date " + new Date(taskDateMillis));
                }
            }
        }

        Log.d(TAG, "Found " + tasksForDate.size() + " tasks for the selected date range.");
        return tasksForDate;
    }

    // --- TaskAdapter.Listener methods ---
    @Override
    public void onToggleFlag(Task task) {
        Log.d(TAG, "onToggleFlag: " + task.getTitle());
        if (taskStore != null && task != null) {
            task.setFlagged(!task.isFlagged());
            taskStore.updateTask(task);
            updateTasksForDate(currentlySelectedDateMillis);
        }
    }

    @Override
    public void onTaskCompleted(Task task) {
        Log.d(TAG, "onTaskCompleted: " + task.getTitle());
        if (taskStore != null && task != null) {
            task.setDone(!task.isDone());
            taskStore.updateTask(task);
            updateTasksForDate(currentlySelectedDateMillis);
        }
    }

    @Override
    public void onDeleteTask(Task task) {
        Log.d(TAG, "onDeleteTask: " + task.getTitle());
        if (taskStore != null && task != null) {
            taskStore.deleteTask(task);
            updateTasksForDate(currentlySelectedDateMillis);
        }
    }
}
