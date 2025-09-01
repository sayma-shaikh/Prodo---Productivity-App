package com.example.prodo.viewmodels; // Or your chosen ViewModel package

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.prodo.data.Task;
import com.example.prodo.data.TaskStore; // Assuming TaskStore is your data source

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private TaskStore taskStore;
    private MutableLiveData<List<Task>> tasksLiveData;
    private MutableLiveData<String> currentCategoryFilter = new MutableLiveData<>(null); // null means all tasks

    // To hold the original full list from TaskStore for filtering
    private List<Task> allTasksFromStore = new ArrayList<>();

    public TaskViewModel(@NonNull Application application) {
        super(application);
        taskStore = TaskStore.get(application.getApplicationContext());
        tasksLiveData = new MutableLiveData<>();
        loadInitialTasks();
    }

    private void loadInitialTasks() {
        allTasksFromStore = new ArrayList<>(taskStore.getTasks()); // Get a mutable copy
        filterAndPostTasks();
    }

    public LiveData<List<Task>> getTasks() {
        return tasksLiveData;
    }

    public void refreshTasks() {
        // Re-fetch from store and re-apply filter
        loadInitialTasks();
    }

    public void addTask(Task task) {
        taskStore.addTask(task);
        refreshTasks(); // Re-fetch and re-filter
    }

    public void updateTask(Task task) {
        taskStore.updateTask(task);
        refreshTasks(); // Re-fetch and re-filter
    }

    public void deleteTask(Task task) {
        taskStore.deleteTask(task);
        refreshTasks(); // Re-fetch and re-filter
    }

    public void filterTasksByCategory(String category) {
        currentCategoryFilter.setValue(category);
        filterAndPostTasks();
    }

    private void filterAndPostTasks() {
        List<Task> filteredTasks = new ArrayList<>();
        String filter = currentCategoryFilter.getValue();

        if (filter == null || "All Tasks".equalsIgnoreCase(filter)) { // "All Tasks" is often a UI string
            filteredTasks.addAll(allTasksFromStore);
        } else {
            for (Task task : allTasksFromStore) {
                if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(filter)) {
                    filteredTasks.add(task);
                }
            }
        }
        sortTasks(filteredTasks); // Sort before posting
        tasksLiveData.postValue(filteredTasks);
    }

    private void sortTasks(List<Task> tasksToSort) {
        // Sort by 'done' status first (undone tasks first), then perhaps by date or title
        Collections.sort(tasksToSort, (t1, t2) -> {
            int doneCompare = Boolean.compare(t1.isDone(), t2.isDone());
            if (doneCompare != 0) {
                return doneCompare;
            }
            // Add secondary sort criteria if needed, e.g., by date or title
            // For now, just by done status
            return 0; // Or compare by another field if done status is the same
        });
    }

    // Optional: If you need to batch update categories for tasks (e.g., when a category is renamed/deleted)
    public void updateTasksCategoryBatch(String oldCategoryName, String newCategoryName) {
        List<Task> tasksToUpdate = new ArrayList<>();
        boolean changed = false;
        for (Task task : taskStore.getTasks()) { // Operate on the source list
            if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(oldCategoryName)) {
                task.setCategory(newCategoryName); // newCategoryName could be null or Task.UNCATEGORIZED
                tasksToUpdate.add(task);
                changed = true;
            }
        }
        if (changed) {
            for (Task task : tasksToUpdate) {
                taskStore.updateTask(task); // Update each modified task in the store
            }
            refreshTasks(); // Refresh the LiveData
        }
    }
}
