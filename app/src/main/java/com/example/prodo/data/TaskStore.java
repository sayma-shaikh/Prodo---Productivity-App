package com.example.prodo.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskStore {
    private static final String TAG = "TaskStore";
    private static final String FILENAME = "tasks.json";

    private static TaskStore sTaskStore;
    private List<Task> mTasks;
    private final MutableLiveData<List<Task>> mTasksLiveData = new MutableLiveData<>();
    private final Context mContext;

    public static synchronized TaskStore get(Context context) {
        if (sTaskStore == null) {
            sTaskStore = new TaskStore(context.getApplicationContext());
        }
        return sTaskStore;
    }

    private TaskStore(Context context) {
        mContext = context.getApplicationContext();
        mTasks = new ArrayList<>();
        loadTasks(); // Load from file
        updateTasksLiveData();
    }

    public LiveData<List<Task>> getTasksLiveData() {
        return mTasksLiveData;
    }

    private void updateTasksLiveData() {
        // Post a new copy to ensure observers are triggered correctly with a new list instance
        mTasksLiveData.postValue(new ArrayList<>(mTasks));
    }

    // Central notifier for LiveData + persistence
    private void notifyLiveDataObservers() {
        updateTasksLiveData(); // Posts to mTasksLiveData
        saveTasks();           // Saves to JSON file
    }

    public List<Task> getTasks() {
        return new ArrayList<>(mTasks); // Return a copy to prevent external modification
    }

    public Task getTaskById(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            Log.w(TAG, "getTaskById: Provided ID string is null or empty.");
            return null;
        }
        try {
            UUID taskId = UUID.fromString(idString);
            return getTask(taskId); // Delegate to the UUID version
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getTaskById: Invalid UUID string format: " + idString, e);
        }
        return null;
    }

    public Task getTask(UUID id) {
        if (id == null) {
            Log.w(TAG, "getTask: Provided UUID is null.");
            return null;
        }
        for (Task task : mTasks) {
            if (task != null && task.getId() != null && task.getId().equals(id)) {
                return task;
            }
        }
        Log.d(TAG, "getTask: Task with UUID " + id + " not found.");
        return null;
    }

    public void addTask(Task task) {
        if (task == null) {
            Log.e(TAG, "addTask: Attempted to add a null task.");
            return;
        }
        // Ensure task has an ID
        if (task.getId() == null) {
            Log.w(TAG, "addTask: Task '" + task.getTitle() + "' has null ID. Assigning a new one.");
            task.assignNewId(); // Assuming Task class has this method
        }

        // Optional: Check for duplicates before adding
        // for (Task existingTask : mTasks) {
        //     if (existingTask.getId().equals(task.getId())) {
        //         Log.w(TAG, "addTask: Task with ID " + task.getId() + " already exists. Not adding.");
        //         return;
        //     }
        // }

        mTasks.add(task);
        Log.d(TAG, "Task added: '" + task.getTitle() + "' with ID: " + task.getId());
        notifyLiveDataObservers();
    }

    // In TaskStore.java

    public void updateTask(Task taskToUpdate) {
        // This part you provided is correct:
        if (taskToUpdate == null || taskToUpdate.getId() == null) {
            Log.e(TAG, "updateTask: Attempted to update a null task or task with null ID.");
            return;
        }

        boolean found = false;
        for (int i = 0; i < mTasks.size(); i++) {
            Task existingTask = mTasks.get(i);
            // Ensure existingTask and its ID are not null before comparing
            if (existingTask != null && existingTask.getId() != null && existingTask.getId().equals(taskToUpdate.getId())) {
                mTasks.set(i, taskToUpdate); // Replace the old task instance with the updated one
                found = true;

                // **ADJUSTED LOGGING LINE for int pomodoroCount from Task.java**
                int currentPoms = taskToUpdate.getPomodoroCount(); // Get the int value
                Log.d(TAG, "Task updated in TaskStore: '" + taskToUpdate.getTitle() + "'" +
                        (currentPoms > 0 ? " Poms: " + currentPoms : "")); // Only show if > 0
                break;
            }
        }

        if (!found) {
            Log.w(TAG, "updateTask: Task with ID " + taskToUpdate.getId() + " not found for update. Title: '" + taskToUpdate.getTitle() + "'");
            // Optionally add the task if it's missing, though "update" usually implies it exists
            // mTasks.add(taskToUpdate);
        }
        notifyLiveDataObservers(); // This will trigger saveTasks() and update LiveData
    }


    public void deleteTask(Task taskToDelete) {
        if (taskToDelete == null || taskToDelete.getId() == null) {
            Log.w(TAG, "deleteTask: Attempted to delete a null task or task with null ID.");
            return;
        }

        boolean removed = false;
        // Iterate and remove by ID to ensure correct object removal
        for (int i = 0; i < mTasks.size(); i++) {
            if (mTasks.get(i) != null && mTasks.get(i).getId() != null && mTasks.get(i).getId().equals(taskToDelete.getId())) {
                mTasks.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            Log.d(TAG, "Task deleted: '" + taskToDelete.getTitle() + "' with ID: " + taskToDelete.getId());
        } else {
            Log.w(TAG, "deleteTask: Attempted to delete a task not found. Title: '" + taskToDelete.getTitle() + "' with ID: " + taskToDelete.getId());
        }
        notifyLiveDataObservers();
    }

    private void saveTasks() {
        if (mContext == null) {
            Log.e(TAG, "saveTasks: Context is null. Cannot save tasks.");
            return;
        }
        if (mTasks == null) {
            Log.e(TAG, "saveTasks: Task list (mTasks) is null. Cannot save tasks.");
            // Consider initializing mTasks here if this state is possible and undesirable:
            // mTasks = new ArrayList<>();
            return;
        }

        Gson gson = new Gson();
        String json = gson.toJson(mTasks);
        Log.d(TAG, "saveTasks: Saving " + mTasks.size() + " tasks to JSON.");

        try (FileOutputStream fos = mContext.openFileOutput(FILENAME, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes());
            Log.d(TAG, "Tasks saved successfully to " + FILENAME);
        } catch (Exception e) {
            Log.e(TAG, "Error saving tasks to " + FILENAME, e);
        }
    }

    private void loadTasks() {
        mTasks = new ArrayList<>(); // Ensure mTasks is initialized even if loading fails
        if (mContext == null) {
            Log.e(TAG, "loadTasks: Context is null. Cannot load tasks.");
            return;
        }

        try (FileInputStream fis = mContext.openFileInput(FILENAME);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader bufferedReader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString();
            if (json.isEmpty()) {
                Log.i(TAG, "loadTasks: " + FILENAME + " is empty. Starting with an empty task list.");
                return; // mTasks is already an empty list
            }

            Gson gson = new Gson();
            Type taskListType = new TypeToken<ArrayList<Task>>() {}.getType();
            List<Task> loadedTasks = gson.fromJson(json, taskListType);

            if (loadedTasks != null) {
                mTasks.clear(); // Clear existing tasks before adding loaded ones
                for (Task task : loadedTasks) {
                    if (task != null) { // Individual task object from JSON might be null if JSON is malformed
                        if (task.getId() == null) {
                            Log.w(TAG, "loadTasks: Task '" + (task.getTitle() != null ? task.getTitle() : "NO_TITLE") + "' has null ID. Assigning a new one.");
                            task.assignNewId(); // Ensure Task class has assignNewId()
                        }
                        mTasks.add(task);
                    } else {
                        Log.w(TAG, "loadTasks: Found a null task object in the loaded list. Skipping.");
                    }
                }
                Log.d(TAG, "Loaded " + mTasks.size() + " tasks from " + FILENAME);
            } else {
                Log.w(TAG, "loadTasks: Gson returned a null list from JSON, even though JSON was not empty. File might be corrupted or not a valid Task list JSON. JSON: " + json);
                // mTasks remains an empty list
            }

        } catch (java.io.FileNotFoundException e) {
            Log.i(TAG, "loadTasks: " + FILENAME + " not found. Starting with an empty task list.");
            // mTasks is already an empty list
        } catch (Exception e) {
            Log.e(TAG, "loadTasks: Error loading tasks from " + FILENAME, e);
            // mTasks remains an empty list as a fallback
        }
        // updateTasksLiveData() is called in the constructor after loadTasks()
    }
}
