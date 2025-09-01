package com.example.prodo.ui.tasks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prodo.R;
import com.example.prodo.data.CategoryManager;
import com.example.prodo.data.Task;
import com.example.prodo.ui.pomodoro.PomodoroFragment;
import com.example.prodo.viewmodels.TaskViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment implements TaskAdapter.Listener {

    private static final String TAG = "TasksFragment";

    // Inside public class PomodoroFragment extends Fragment {
    public static final String REQUEST_KEY_SELECT_TASK = "REQUEST_KEY_SELECT_TASK";
    public static final String BUNDLE_KEY_SELECTED_TASK_ID = "BUNDLE_KEY_SELECTED_TASK_ID";
    public static final String ARG_TASK_ID = "task_id"; // Add this
    // ... rest of your PomodoroFragment class
    // }
    private TaskAdapter adapter;
    private ChipGroup chipGroup;
    private Chip chipAllTasks;
    private Chip chipAddNewCategory;
    private CategoryManager categoryManager;
    private TaskViewModel taskViewModel;

    private ActivityResultLauncher<Intent> taskDetailLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        categoryManager = new CategoryManager(requireContext());
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        // ✅ ActivityResultLauncher for TaskDetailActivity
        Log.d(TAG, "onCreate: Registering ActivityResultLauncher.");
        taskDetailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "ActivityResultLauncher callback triggered. ResultCode: " + result.getResultCode());

                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.hasExtra(TaskDetailActivity.RESULT_EXTRA_TASK_ID_FOR_POMODORO)) {
                            String taskId = data.getStringExtra(TaskDetailActivity.RESULT_EXTRA_TASK_ID_FOR_POMODORO);
                            Log.d(TAG, "Received Task ID for Pomodoro: " + taskId);

                            if (taskId != null) {
                                try {
                                    NavController navController = Navigation.findNavController(
                                            requireActivity(), R.id.nav_host_fragment_content_main);
                                    Bundle args = new Bundle();
                                    args.putString(PomodoroFragment.ARG_TASK_ID, taskId);
                                    navController.navigate(R.id.nav_pomodoro, args);
                                } catch (Exception e) {
                                    Log.e(TAG, "Navigation to PomodoroFragment failed.", e);
                                }
                            } else {
                                Log.w(TAG, "Received null task ID.");
                            }
                        } else {
                            Log.w(TAG, "Result data missing expected extras.");
                        }
                    } else {
                        Log.d(TAG, "Result code not OK: " + result.getResultCode());
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        RecyclerView rv = v.findViewById(R.id.recyclerTasks);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(this);
        rv.setAdapter(adapter);

        chipGroup = v.findViewById(R.id.chipGroup);
        chipAllTasks = v.findViewById(R.id.chipAllTasks);
        chipAddNewCategory = v.findViewById(R.id.chipAddNewCategory);

        populateCategoryChips();

        taskViewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            Log.d(TAG, "Tasks LiveData updated. Task count: " + (tasks != null ? tasks.size() : 0));
            adapter.submitList(tasks != null ? new ArrayList<>(tasks) : new ArrayList<>());
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                taskViewModel.filterTasksByCategory(null);
                if (chipAllTasks != null) chipAllTasks.setChecked(true);
                return;
            }

            int checkedChipId = checkedIds.get(0);
            if (checkedChipId == R.id.chipAllTasks) {
                taskViewModel.filterTasksByCategory(null);
            } else if (checkedChipId == R.id.chipAddNewCategory) {
                showAddNewCategoryDialog();
            } else {
                Chip selectedChip = group.findViewById(checkedChipId);
                if (selectedChip != null) {
                    taskViewModel.filterTasksByCategory(selectedChip.getText().toString());
                }
            }
        });

        if (chipGroup.getCheckedChipId() == View.NO_ID && chipAllTasks != null) {
            chipAllTasks.setChecked(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        populateCategoryChips();
        taskViewModel.refreshTasks();
    }

    private void populateCategoryChips() {
        if (chipGroup == null || categoryManager == null) {
            Log.e(TAG, "ChipGroup or CategoryManager is null.");
            return;
        }

        List<View> chipsToRemove = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child.getId() != R.id.chipAllTasks && child.getId() != R.id.chipAddNewCategory) {
                chipsToRemove.add(child);
            }
        }
        for (View chipView : chipsToRemove) {
            chipGroup.removeView(chipView);
        }

        List<String> categories = categoryManager.getCategories();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int insertAtIndex = chipGroup.indexOfChild(chipAddNewCategory);
        if (insertAtIndex == -1) {
            insertAtIndex = chipGroup.getChildCount();
        }

        for (String categoryName : categories) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_category_item, chipGroup, false);
            chip.setText(categoryName);
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chipGroup.addView(chip, insertAtIndex++);
        }
    }

    private void showAddNewCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Category");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_text, (ViewGroup) getView(), false);
        final TextInputLayout inputLayout = viewInflated.findViewById(R.id.inputLayout);
        final EditText input = viewInflated.findViewById(R.id.inputText);
        inputLayout.setHint("Category Name");
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            if (chipAllTasks != null && chipGroup.getCheckedChipId() == R.id.chipAddNewCategory) {
                chipAllTasks.setChecked(true);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String categoryName = input.getText().toString().trim();
                if (categoryName.isEmpty()) {
                    inputLayout.setError("Category name cannot be empty");
                    return;
                }
                if (!categoryManager.addCategory(categoryName)) {
                    inputLayout.setError("Category already exists or is invalid");
                    return;
                }
                inputLayout.setError(null);
                populateCategoryChips();
                for (int i = 0; i < chipGroup.getChildCount(); i++) {
                    View child = chipGroup.getChildAt(i);
                    if (child instanceof Chip) {
                        Chip currentChip = (Chip) child;
                        if (currentChip.getText().toString().equals(categoryName)) {
                            currentChip.setChecked(true);
                            break;
                        }
                    }
                }
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    @Override
    public void onToggleFlag(Task task) {
        task.setFlagged(!task.isFlagged());
        taskViewModel.updateTask(task);
    }

    @Override
    public void onTaskCompleted(Task task) {
        task.setDone(!task.isDone());
        taskViewModel.updateTask(task);
    }

    @Override
    public void onDeleteTask(Task task) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete \"" + task.getTitle() + "\"?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    taskViewModel.deleteTask(task);
                    Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void refreshTaskList() {
        Log.d(TAG, "Manual refreshTaskList() called.");
        taskViewModel.refreshTasks();
    }

    // ✅ You can call this from your adapter or UI to open task details
    private void launchTaskDetail(Task taskToOpen) {
        if (taskToOpen == null || taskToOpen.getId() == null) {
            Log.e(TAG, "Cannot launch detail for null task or task with null ID.");
            Toast.makeText(getContext(), "Cannot open task details.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Launching TaskDetailActivity for task: " + taskToOpen.getTitle());
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskToOpen.getId().toString());
        taskDetailLauncher.launch(intent);
    }
}

