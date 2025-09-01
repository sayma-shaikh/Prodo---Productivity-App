package com.example.prodo.ui.pomodoro;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView; // Keep if you plan to use rvTasks

import com.example.prodo.R;
import com.example.prodo.data.Task;
import com.example.prodo.data.TaskStore;
import com.example.prodo.viewmodels.PomodoroViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PomodoroFragment extends Fragment {

    private static final String TAG = "PomodoroFragment";

    public static final String ARG_TASK_ID = "selected_task_id_arg";
    // --- MODIFIED: Uncommented these lines ---
    public static final String REQUEST_KEY_SELECT_TASK = "requestKeySelectTask";
    public static final String BUNDLE_KEY_SELECTED_TASK_ID = "bundleKeySelectedTaskId";
    // -----------------------------------------

    private PomodoroViewModel pomodoroViewModel;
    private TaskStore taskStore;
    private String selectedTaskId = null;
    private Task currentSelectedTaskObject = null;

    private TabLayout tabLayout;
    private TextView tvTimer;
    private Button btnStart;
    private Button btnReset;
    private TextView tvSessionInfo;
    private ImageButton btnMoreOptions;
    // private RecyclerView rvTasks; // Commented out as per previous state, uncomment if needed
    // private Button btnAddTask; // Commented out as per previous state, uncomment if needed

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pomodoro, container, false);

        try {
            taskStore = TaskStore.get(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "Error getting TaskStore instance", e);
            Toast.makeText(getContext(), "Error initializing task store.", Toast.LENGTH_LONG).show();
            // Consider more robust error handling, like disabling task features
        }

        initializeViews(root);
        setupViewModel(); // Setup ViewModel before listeners that might use it

        if (pomodoroViewModel != null) {
            pomodoroViewModel.setSelectedTaskTitleForStats(null);
        }

        setupTabLayout();
        setupClickListeners();
        // setupRecyclerView(); // Call if rvTasks is used

        if (getArguments() != null && getArguments().containsKey(ARG_TASK_ID)) {
            selectedTaskId = getArguments().getString(ARG_TASK_ID);
            loadAndDisplaySelectedTaskInfo();
        } else {
            updateSessionInfoText(pomodoroViewModel != null ? pomodoroViewModel.currentSessionMode.getValue() : PomodoroViewModel.TimerSessionMode.WORK);
        }

        // --- NEW: Setup FragmentResultListener ---
        // Moved from onCreate to ensure viewLifecycleOwner is available for LiveData observation if needed within listener
        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_SELECT_TASK, getViewLifecycleOwner(), (requestKey, bundle) -> {
            String taskIdFromBundle = bundle.getString(BUNDLE_KEY_SELECTED_TASK_ID);
            if (taskIdFromBundle != null) {
                Log.d(TAG, "Received task ID from TaskListDialogFragment: " + taskIdFromBundle);
                selectedTaskId = taskIdFromBundle;
                loadAndDisplaySelectedTaskInfo(); // This will load the task and update UI/ViewModel
                Toast.makeText(getContext(), "Task selected: " + (currentSelectedTaskObject != null ? currentSelectedTaskObject.getTitle() : "ID " + taskIdFromBundle), Toast.LENGTH_SHORT).show();
            }
        });
        // ------------------------------------------

        return root;
    }

    private void initializeViews(View root) {
        tabLayout = root.findViewById(R.id.tabLayout);
        tvTimer = root.findViewById(R.id.tvTimer);
        btnStart = root.findViewById(R.id.btnStart);
        btnReset = root.findViewById(R.id.btnReset);
        tvSessionInfo = root.findViewById(R.id.tvSessionInfo);
        btnMoreOptions = root.findViewById(R.id.btnMoreOptions);
        // rvTasks = root.findViewById(R.id.rvTasks);
        // btnAddTask = root.findViewById(R.id.btnAddTask);
    }

    private void setupViewModel() {
        pomodoroViewModel = new ViewModelProvider(this).get(PomodoroViewModel.class);

        pomodoroViewModel.timeDisplay.observe(getViewLifecycleOwner(), time -> {
            if (tvTimer != null) {
                tvTimer.setText(time);
            }
        });

        pomodoroViewModel.timerState.observe(getViewLifecycleOwner(), state -> {
            updateUIBasedOnTimerState(state);
        });

        pomodoroViewModel.currentSessionMode.observe(getViewLifecycleOwner(), mode -> {
            updateSessionInfoText(mode);
            if (tabLayout != null && mode != null) {
                int tabPosition = mode.ordinal();
                if (tabLayout.getSelectedTabPosition() != tabPosition) {
                    TabLayout.Tab tab = tabLayout.getTabAt(tabPosition);
                    if (tab != null) {
                        tab.select();
                    }
                }
            }
        });

        pomodoroViewModel.completedPomodorosTodayDisplay.observe(getViewLifecycleOwner(), count -> {
            Log.d(TAG, "Today's completed Pomodoros (from ViewModel): " + count);
        });
    }

    private void updateSessionInfoText(PomodoroViewModel.TimerSessionMode mode) {
        if (tvSessionInfo == null || mode == null) return;

        String modeText;
        switch (mode) {
            case WORK:
                modeText = (currentSelectedTaskObject != null && currentSelectedTaskObject.getTitle() != null) ?
                        "Focus: " + currentSelectedTaskObject.getTitle() :
                        getString(R.string.default_pomodoro_session_info);
                break;
            case SHORT_BREAK:
                modeText = "Short Break";
                break;
            case LONG_BREAK:
                modeText = "Long Break";
                break;
            default:
                modeText = getString(R.string.default_pomodoro_session_info);
        }
        tvSessionInfo.setText(modeText);
    }


    private void setupTabLayout() {
        if (tabLayout == null) return;
        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Pomodoro"));
            tabLayout.addTab(tabLayout.newTab().setText("Short Break"));
            tabLayout.addTab(tabLayout.newTab().setText("Long Break"));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (pomodoroViewModel.timerState.getValue() == PomodoroViewModel.TimerState.RUNNING) {
                    Toast.makeText(getContext(), "Stop current timer before changing mode.", Toast.LENGTH_SHORT).show();
                    PomodoroViewModel.TimerSessionMode currentMode = pomodoroViewModel.currentSessionMode.getValue();
                    if (currentMode != null) {
                        TabLayout.Tab previousTab = tabLayout.getTabAt(currentMode.ordinal());
                        if (previousTab != null) {
                            previousTab.select();
                        }
                    }
                    return;
                }
                switch (tab.getPosition()) {
                    case 0: pomodoroViewModel.setWorkMode(); break;
                    case 1: pomodoroViewModel.setShortBreakMode(); break;
                    case 2: pomodoroViewModel.setLongBreakMode(); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupClickListeners() {
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                PomodoroViewModel.TimerState currentState = pomodoroViewModel.timerState.getValue();
                if (currentState == PomodoroViewModel.TimerState.RUNNING) {
                    pomodoroViewModel.pauseTimer();
                } else {
                    pomodoroViewModel.startTimer();
                }
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> pomodoroViewModel.resetTimer());
        }

        if (btnMoreOptions != null) {
            // Updated to show TaskListDialogFragment instead of PopupMenu
            btnMoreOptions.setOnClickListener(v -> {
                Log.d(TAG, "More options clicked, showing TaskListDialogFragment.");
                // Ensure TaskListDialogFragment is correctly defined and in your nav graph
                // or use newInstance() and show() if it's a DialogFragment not in nav graph.
                // Assuming TaskListDialogFragment.newInstance() exists as per previous context
                com.example.prodo.ui.tasks.TaskListDialogFragment.newInstance()
                        .show(getParentFragmentManager(), com.example.prodo.ui.tasks.TaskListDialogFragment.TAG);
            });
        }
    }

    // This method is now effectively replaced by showing TaskListDialogFragment and listening to its result
    // Keep it if you have other ways to select tasks, or remove if TaskListDialogFragment is the sole method.
    private void showTaskSelectionPopup(View v) {
        // This is the old PopupMenu logic.
        // If TaskListDialogFragment is the primary way to select tasks,
        // this method might be deprecated or removed.
        // For now, let's keep it but note that btnMoreOptions now calls the dialog.
        Log.d(TAG, "showTaskSelectionPopup called, but btnMoreOptions now uses TaskListDialogFragment.");
        // Consider removing this or having a different trigger if you still want a PopupMenu option.
        if (taskStore == null) {
            Toast.makeText(getContext(), "Task store not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Task> tasks = taskStore.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            Toast.makeText(getContext(), "No tasks available to select.", Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu popup = new PopupMenu(getContext(), v);
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.getTitle() != null && !task.getTitle().isEmpty()) {
                popup.getMenu().add(0, i, i, task.getTitle());
            }
        }
        popup.getMenu().add(0, tasks.size(), tasks.size(), getString(R.string.clear_selected_task));

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == tasks.size()) {
                selectedTaskId = null;
                currentSelectedTaskObject = null;
                if (pomodoroViewModel != null) {
                    pomodoroViewModel.setSelectedTaskTitleForStats(null);
                }
                updateSessionInfoText(pomodoroViewModel != null ? pomodoroViewModel.currentSessionMode.getValue() : PomodoroViewModel.TimerSessionMode.WORK);
                Toast.makeText(getContext(), "Task selection cleared.", Toast.LENGTH_SHORT).show();
                return true;
            }

            Task selected = tasks.get(item.getItemId());
            if (selected.getId() != null) {
                selectedTaskId = selected.getId().toString();
                currentSelectedTaskObject = selected;
                if (pomodoroViewModel != null && currentSelectedTaskObject.getTitle() != null) {
                    pomodoroViewModel.setSelectedTaskTitleForStats(currentSelectedTaskObject.getTitle());
                }
                loadAndDisplaySelectedTaskInfo();
            } else {
                Log.e(TAG, "Selected task has a null ID.");
                Toast.makeText(getContext(), "Selected task has an invalid ID.", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popup.show();
    }


    private void loadAndDisplaySelectedTaskInfo() {
        if (selectedTaskId != null && taskStore != null) {
            try {
                currentSelectedTaskObject = taskStore.getTask(UUID.fromString(selectedTaskId));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "selectedTaskId is not a valid UUID string: " + selectedTaskId, e);
                currentSelectedTaskObject = null;
            }

            if (currentSelectedTaskObject != null) {
                updateSessionInfoText(pomodoroViewModel != null ? pomodoroViewModel.currentSessionMode.getValue() : PomodoroViewModel.TimerSessionMode.WORK);
                if (pomodoroViewModel != null && currentSelectedTaskObject.getTitle() != null) {
                    pomodoroViewModel.setSelectedTaskTitleForStats(currentSelectedTaskObject.getTitle());
                } else if (pomodoroViewModel != null) {
                    pomodoroViewModel.setSelectedTaskTitleForStats(null);
                }
            } else {
                selectedTaskId = null;
                if (pomodoroViewModel != null) {
                    pomodoroViewModel.setSelectedTaskTitleForStats(null);
                }
                updateSessionInfoText(pomodoroViewModel != null ? pomodoroViewModel.currentSessionMode.getValue() : PomodoroViewModel.TimerSessionMode.WORK);
            }
        } else {
            currentSelectedTaskObject = null;
            if (pomodoroViewModel != null) {
                pomodoroViewModel.setSelectedTaskTitleForStats(null);
            }
            updateSessionInfoText(pomodoroViewModel != null ? pomodoroViewModel.currentSessionMode.getValue() : PomodoroViewModel.TimerSessionMode.WORK);
        }
    }

    private void updateUIBasedOnTimerState(PomodoroViewModel.TimerState state) {
        if (state == null || btnStart == null || btnReset == null || tvTimer == null || pomodoroViewModel == null) {
            Log.w(TAG, "updateUIBasedOnTimerState: A required view or ViewModel is null. State: " + state);
            return;
        }
        switch (state) {
            case RUNNING:
                btnStart.setText(R.string.pause_button_text);
                btnReset.setVisibility(View.INVISIBLE);
                break;
            case PAUSED:
                btnStart.setText(R.string.resume_button_text);
                btnReset.setVisibility(View.VISIBLE);
                break;
            case STOPPED:
                btnStart.setText(R.string.start_button_text);
                btnReset.setVisibility(View.VISIBLE);
                tvTimer.setText(formatTime(pomodoroViewModel.getCurrentModeTotalDurationMillis()));
                break;
        }
    }

    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void setupRecyclerView() {
        Log.d(TAG, "RecyclerView setup (basic). Adapter and data TODO (if rvTasks is used).");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");
    }
}



