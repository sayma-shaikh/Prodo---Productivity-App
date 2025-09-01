package com.example.prodo.ui.tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prodo.R;
import com.example.prodo.data.Subtask;
import com.example.prodo.data.Task;
import com.example.prodo.data.TaskStore;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TaskDetailActivity extends AppCompatActivity implements SubtaskAdapter.SubtaskListener {

    private static final String TAG = "TaskDetailActivity";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String RESULT_EXTRA_TASK_ID_FOR_POMODORO = "RESULT_EXTRA_TASK_ID_FOR_POMODORO";

    private TextInputEditText editTitle;
    private TextInputEditText editNote;
    private TextView textStatusValue;
    private TextView textDateValue;
    private Chip chipCategoryValue;
    private RecyclerView recyclerSubtasks;
    private Button buttonAddSubtask;
    //private Button buttonStartPomodoro;
    private Button buttonSave;
    private Button buttonDeleteTask;

    private SubtaskAdapter subtaskAdapter;
    private Task currentTask;
    private Calendar selectedDateCalendar = Calendar.getInstance();
    private TaskStore taskStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskStore = TaskStore.get(getApplicationContext());

        initViews();
        setupSubtasksRecyclerView();
        loadTaskData();
        setupClickListeners();
    }

    private void initViews() {
        editTitle = findViewById(R.id.edit_detail_title);
        editNote = findViewById(R.id.edit_detail_note);
        textStatusValue = findViewById(R.id.text_detail_status_value);
        textDateValue = findViewById(R.id.text_detail_date_value);
        chipCategoryValue = findViewById(R.id.chip_detail_category_value);
        recyclerSubtasks = findViewById(R.id.recycler_detail_subtasks);
        buttonAddSubtask = findViewById(R.id.button_add_subtask);
        //buttonStartPomodoro = findViewById(R.id.button_detail_start_pomodoro);
        buttonSave = findViewById(R.id.button_detail_save);
        buttonDeleteTask = findViewById(R.id.button_detail_delete_task);
    }

    private void setupSubtasksRecyclerView() {
        subtaskAdapter = new SubtaskAdapter(this);
        recyclerSubtasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerSubtasks.setAdapter(subtaskAdapter);
    }

    private void loadTaskData() {
        String taskIdString = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskIdString != null && !taskIdString.isEmpty()) {
            try {
                UUID taskId = UUID.fromString(taskIdString);
                currentTask = taskStore.getTask(taskId);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid UUID for task ID", e);
                Toast.makeText(this, "Invalid task ID format.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (currentTask == null) {
                Toast.makeText(this, "Task not found.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            populateTaskDetails();
        } else {
            Toast.makeText(this, "No task ID provided.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void populateTaskDetails() {
        editTitle.setText(currentTask.getTitle());
        editNote.setText(currentTask.getNote() != null ? currentTask.getNote() : "");
        textStatusValue.setText((currentTask.isDone() ? "Completed" : "Pending") + (currentTask.isFlagged() ? " ⭐" : ""));
        if (currentTask.getDate() != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            textDateValue.setText(sdf.format(new java.util.Date(currentTask.getDate())));
        } else {
            textDateValue.setText("No date set");
        }
        chipCategoryValue.setText(currentTask.getCategory() != null ? currentTask.getCategory() : "None");

        List<Subtask> subtasks = currentTask.getSubtasks();
        if (subtasks == null) {
            subtasks = new ArrayList<>();
            currentTask.setSubtasks(subtasks);
        }
        subtaskAdapter.submitList(new ArrayList<>(subtasks));
    }

    private void setupClickListeners() {
        // 1. When 'textDateValue' is clicked (Same as before):
        textDateValue.setOnClickListener(v -> showDatePickerDialog());
        // - Calls 'showDatePickerDialog()' to let the user pick a date.

        // 2. When 'chipCategoryValue' is clicked (Same as before):
        chipCategoryValue.setOnClickListener(v -> Toast.makeText(this, "Select Category (Not Implemented)", Toast.LENGTH_SHORT).show());
        // - Shows a Toast indicating the feature isn't implemented.

        // 3. When 'buttonStartPomodoro' is clicked (CORRECTED LOGIC):
//        buttonStartPomodoro.setOnClickListener(v -> {
//            Log.d(TAG, "'Start Pomodoro' button clicked."); // Log: Button click is registered.
//            // - Checks if 'currentTask' and its ID are valid.
//            if (currentTask != null && currentTask.getId() != null) {
//                String taskIdString = currentTask.getId().toString(); // Gets the task's ID as a String.
//                Log.d(TAG, "Task ID to return for Pomodoro: " + taskIdString); // Log: The ID that will be sent back.
//
//                Intent resultIntent = new Intent(); // Creates an Intent to hold the result data.
//                // Puts the task ID string into the Intent with a specific key.
//                // RESULT_EXTRA_TASK_ID_FOR_POMODORO is a public static final String constant
//                // that the calling Fragment will use to retrieve this ID.
//                resultIntent.putExtra(RESULT_EXTRA_TASK_ID_FOR_POMODORO, taskIdString);
//
//                setResult(Activity.RESULT_OK, resultIntent); // Sets the result of this Activity to RESULT_OK
//                // and provides the resultIntent containing the taskId.
//                finish(); // Closes TaskDetailActivity. This action sends the result back
//                // to the Fragment that launched it (via ActivityResultLauncher).
//            } else {
//                // If currentTask or its ID is null:
//                Log.e(TAG, "Start Pomodoro clicked but currentTask or its ID is null."); // Log: Error condition.
//                Toast.makeText(this, "Task data is not available to start Pomodoro.", Toast.LENGTH_SHORT).show(); // Shows an error message.
//            }
//        });

        // 4. When 'buttonSave' is clicked (Same as before):
        buttonSave.setOnClickListener(v -> saveTask());
        // - Calls 'saveTask()' to save changes to the task.

        // 5. When 'buttonAddSubtask' is clicked (Same as before):
        buttonAddSubtask.setOnClickListener(v -> showAddSubtaskDialog());
        // - Calls 'showAddSubtaskDialog()' to allow adding a subtask.
        //   (The implementation of 'showAddSubtaskDialog' is also provided and looks reasonable).

        // 6. When 'buttonDeleteTask' is clicked (Same as before):
        buttonDeleteTask.setOnClickListener(v -> {
            if (currentTask == null || currentTask.getId() == null) {
                Toast.makeText(this, "Cannot delete task.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete \"" + currentTask.getTitle() + "\"?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        taskStore.deleteTask(currentTask);
                        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }


    private void showAddSubtaskDialog() {
        if (currentTask == null) {
            Toast.makeText(this, "Cannot add subtask: task not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Subtask");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Subtask title");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String subtaskTitle = input.getText().toString().trim();
            if (!subtaskTitle.isEmpty()) {
                Subtask newSubtask = new Subtask(subtaskTitle);
                currentTask.getSubtasks().add(newSubtask);
                subtaskAdapter.submitList(new ArrayList<>(currentTask.getSubtasks()));
                taskStore.updateTask(currentTask);
                Toast.makeText(this, "Subtask added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Subtask title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onSubtaskCheckedChanged(Subtask subtask, boolean isChecked) {
        if (currentTask == null || currentTask.getSubtasks() == null || subtask == null) return;

        for (Subtask s : currentTask.getSubtasks()) {
            if (s.getId().equals(subtask.getId())) {
                s.setDone(isChecked);
                break;
            }
        }
        taskStore.updateTask(currentTask);
        subtaskAdapter.submitList(new ArrayList<>(currentTask.getSubtasks()));
    }

    @Override
    public void onSubtaskDeleteClicked(Subtask subtaskToDelete) {
        if (currentTask == null || subtaskToDelete == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Subtask")
                .setMessage("Are you sure you want to delete subtask \"" + subtaskToDelete.getTitle() + "\"?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    boolean removed = currentTask.getSubtasks().removeIf(subtask ->
                            subtask.getId().equals(subtaskToDelete.getId()));

                    if (removed) {
                        subtaskAdapter.submitList(new ArrayList<>(currentTask.getSubtasks()));
                        taskStore.updateTask(currentTask);
                        Toast.makeText(this, "Subtask deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Subtask not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        if (currentTask != null && currentTask.getDate() != 0) {
            c.setTimeInMillis(currentTask.getDate());
        }

        DatePickerDialog dpd = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedDateCalendar.set(Calendar.YEAR, year);
                    selectedDateCalendar.set(Calendar.MONTH, month);
                    selectedDateCalendar.set(Calendar.DAY_OF_MONTH, day);
                    updateDateLabel();
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(selectedDateCalendar.getTime());
        textDateValue.setText(dateString);
        if (currentTask != null) {
            currentTask.setDate(selectedDateCalendar.getTimeInMillis());
        }
    }

    private void saveTask() {
        if (currentTask == null) {
            Toast.makeText(this, "No task to save", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTask.setTitle(editTitle.getText().toString().trim());
        currentTask.setNote(editNote.getText().toString().trim());

        taskStore.updateTask(currentTask);
        Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}



