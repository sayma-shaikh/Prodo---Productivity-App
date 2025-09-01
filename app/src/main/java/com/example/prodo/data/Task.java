package com.example.prodo.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Task {
    private UUID mId;
    private long mDate;
    private String mTitle;
    private int pomodoroCount; // Field for pomodoro count
    private String mCategory;
    private String mNote;
    private boolean mIsDone;
    private boolean mIsFlagged;

    private List<Subtask> mSubtasks;

    private long mTotalTimeSpentMillis; // Field for total time spent

    // Constructor with all main fields
    public Task(String title, String category, long date, String note) {
        this.mId = UUID.randomUUID();
        this.mTitle = title;
        this.mCategory = category;
        this.mDate = date; // Expects 'date' as long (milliseconds)
        this.mNote = note;
        this.mIsDone = false;
        this.mIsFlagged = false;
        this.pomodoroCount = 0;          // Initialize to 0
        this.mTotalTimeSpentMillis = 0;  // Initialize here
        this.mSubtasks = new ArrayList<>();
    }

    // Optional constructor (e.g., for Firebase, deserialization)
    public Task(String title, String notes, boolean b, String date, String category) {
        this.mId = UUID.randomUUID();
        this.mTitle = title;
        this.mNote = notes; // Assuming 'notes' maps to mNote
        this.mIsDone = b;
        // this.mDate = ? // Consider parsing 'date' (String) to long for mDate if this constructor is used
        this.mCategory = category;
        this.pomodoroCount = 0;          // Initialize to 0
        this.mTotalTimeSpentMillis = 0;  // Initialize here
        this.mSubtasks = new ArrayList<>();
    }

    public void assignNewId() {
        if (this.mId == null) { // Only assign if it's actually null
            this.mId = UUID.randomUUID();
            Log.i("Task", "Assigned new UUID to task titled: " + (this.mTitle != null ? this.mTitle : "N/A"));
        }
    }

    // --- GETTERS ---
    public UUID getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getCategory() {
        return mCategory;
    }

    public long getDate() {
        return mDate;
    }

    public String getNote() {
        return mNote;
    }

    public boolean isDone() {
        return mIsDone;
    }

    public boolean isFlagged() {
        return mIsFlagged;
    }

    public List<Subtask> getSubtasks() {
        if (mSubtasks == null) {
            mSubtasks = new ArrayList<>();
        }
        return mSubtasks;
    }

    // Getter for pomodoroCount (Correctly named for StatsViewModel)
    public int getPomodoroCount() {
        return pomodoroCount;
    }

    // Getter for mTotalTimeSpentMillis (Correctly named for StatsViewModel)
    public long getTotalTimeSpentMillis() {
        return mTotalTimeSpentMillis;
    }

    // --- SETTERS ---
    public void setTitle(String title) {
        mTitle = title;
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public void setDate(long date) {
        this.mDate = date;
    }

    public void setNote(String note) {
        mNote = note;
    }

    public void setDone(boolean done) {
        mIsDone = done;
    }

    public void setFlagged(boolean flagged) {
        mIsFlagged = flagged;
    }

    public void setPomodoroCount(int pomodoroCount) {
        this.pomodoroCount = pomodoroCount;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        mSubtasks = subtasks;
    }

    public void setTotalTimeSpentMillis(long totalTimeSpentMillis) {
        this.mTotalTimeSpentMillis = totalTimeSpentMillis;
    }

    // --- BEHAVIORAL METHODS ---
    public void incrementPomodoroCount() {
        this.pomodoroCount++;
    }

    public void addDurationToTimeSpent(long durationMillis) {
        this.mTotalTimeSpentMillis += durationMillis;
    }

    public void recordPomodoroCompleted(long sessionDurationMillis) {
        incrementPomodoroCount();
        addDurationToTimeSpent(sessionDurationMillis);
    }

    // --- equals() and hashCode() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return mDate == task.mDate &&
                pomodoroCount == task.pomodoroCount &&
                mIsDone == task.mIsDone &&
                mIsFlagged == task.mIsFlagged &&
                mTotalTimeSpentMillis == task.mTotalTimeSpentMillis &&
                Objects.equals(mId, task.mId) &&
                Objects.equals(mTitle, task.mTitle) &&
                Objects.equals(mCategory, task.mCategory) &&
                Objects.equals(mNote, task.mNote) &&
                Objects.equals(mSubtasks, task.mSubtasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mDate, mTitle, pomodoroCount, mCategory, mNote, mIsDone, mIsFlagged, mSubtasks, mTotalTimeSpentMillis);
    }
}
