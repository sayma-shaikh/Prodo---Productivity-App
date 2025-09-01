package com.example.prodo.data;

import java.util.UUID;

public class Subtask {
    private String id;
    private String title;
    private boolean isDone;
    // Optional: Add parentTaskId if needed for denormalized data, but often not necessary
    // if subtasks are always nested within a Task object.

    // Default constructor for Gson
    public Subtask() {
        this.id = UUID.randomUUID().toString();
        this.isDone = false;
    }

    public Subtask(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.isDone = false;
    }

    public Subtask(String id, String title, boolean isDone) {
        this.id = id;
        this.title = title;
        this.isDone = isDone;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDone() {
        return isDone;
    }

    // Setters
    public void setId(String id) { // Typically ID is set once, but setter can be useful
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subtask subtask = (Subtask) o;
        return id.equals(subtask.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
