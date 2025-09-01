package com.example.prodo.ui.tasks; // Or a dialogs package

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.prodo.R;
import com.example.prodo.data.Task;
import com.example.prodo.data.TaskStore;
import com.example.prodo.ui.pomodoro.PomodoroFragment;

import java.util.ArrayList;
import java.util.List;

public class TaskListDialogFragment extends DialogFragment {
    public static final String TAG = "TaskListDialogFragment";

    // --- ADD THIS newInstance() METHOD ---
    public static TaskListDialogFragment newInstance() {
        Bundle args = new Bundle();
        // If you ever need to pass arguments to this dialog, put them in the bundle here.
        // For example: args.putString("some_key", "some_value");
        TaskListDialogFragment fragment = new TaskListDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }
    // --- END OF newInstance() METHOD ---


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_task_list, null);

        ListView listViewTasks = view.findViewById(R.id.listViewTasks);
        List<Task> tasks = TaskStore.get(getContext()).getTasks();
        List<String> taskTitles = new ArrayList<>();

        if (tasks != null) { // Safety check
            for (Task task : tasks) {
                if (task != null && task.getTitle() != null) { // More safety checks
                    taskTitles.add(task.getTitle());
                }
            }
        } else {
            // Handle case where tasks list is null, maybe show a message in the dialog
            // or ensure TaskStore.getTasks() never returns null (returns empty list instead)
            Log.w(TAG, "Task list from TaskStore is null.");
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, taskTitles);
        listViewTasks.setAdapter(adapter);

        listViewTasks.setOnItemClickListener((parent, itemView, position, id) -> {
            if (tasks != null && position >= 0 && position < tasks.size()) { // Boundary check
                Task selectedTask = tasks.get(position);
                if (selectedTask != null && selectedTask.getId() != null) { // Null check for task and ID
                    Bundle result = new Bundle();
                    // Ensure PomodoroFragment.BUNDLE_KEY_SELECTED_TASK_ID and
                    // PomodoroFragment.REQUEST_KEY_SELECT_TASK are defined in PomodoroFragment
                    result.putString(PomodoroFragment.BUNDLE_KEY_SELECTED_TASK_ID, selectedTask.getId().toString());
                    getParentFragmentManager().setFragmentResult(PomodoroFragment.REQUEST_KEY_SELECT_TASK, result);
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Error: Selected task or its ID is null.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Selected task or its ID is null at position: " + position);
                }
            } else {
                Toast.makeText(getContext(), "Error selecting task from list.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error in setOnItemClickListener - tasks list null or position out of bounds. Position: " + position);
            }
        });

        builder.setView(view)
                .setTitle("Select a Task")
                .setNegativeButton("Cancel", (dialog, which) -> dismiss());

        return builder.create();
    }
}

