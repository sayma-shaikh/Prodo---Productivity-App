package com.example.prodo.data;  // adjust package if needed

import java.util.Objects;

public class TaskAnalyticItem {
    private final String taskTitle;
    private final int pomodoroCount;
    private final long totalTimeSpentMillis;
    private final String date; // <-- ADDED THIS FIELD

    public TaskAnalyticItem(String taskTitle, int pomodoroCount, long totalTimeSpentMillis, String date) {
        this.taskTitle = taskTitle;
        this.pomodoroCount = pomodoroCount;
        this.totalTimeSpentMillis = totalTimeSpentMillis;
        this.date = date; // <-- INITIALIZED THIS FIELD
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public int getPomodoroCount() {
        return pomodoroCount;
    }

    public long getTotalTimeSpentMillis() {
        return totalTimeSpentMillis;
    }

    public String getDate() { // <-- ADDED GETTER FOR DATE
        return date;
    }

    // Helper: convert total time to hours (as float with 1 decimal precision)
    public float getTotalHoursSpent() {
        return totalTimeSpentMillis / (1000f * 60f * 60f);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskAnalyticItem)) return false;
        TaskAnalyticItem that = (TaskAnalyticItem) o;
        return pomodoroCount == that.pomodoroCount &&
                totalTimeSpentMillis == that.totalTimeSpentMillis &&
                Objects.equals(taskTitle, that.taskTitle) &&
                Objects.equals(date, that.date); // <-- ADDED DATE TO EQUALS
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskTitle, pomodoroCount, totalTimeSpentMillis, date); // <-- ADDED DATE TO HASHCODE
    }
}
