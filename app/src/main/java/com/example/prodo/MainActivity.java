package com.example.prodo;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.prodo.data.Task;
import com.example.prodo.data.TaskStore;
import com.example.prodo.databinding.ActivityMainBinding;
import com.example.prodo.ui.tasks.AddTaskBottomSheet;
import com.example.prodo.ui.tasks.TasksFragment;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // --- Add Task FAB ---
        if (binding.appBarMain.bottomRightAddTaskButton != null) {
            binding.appBarMain.bottomRightAddTaskButton.setOnClickListener(view -> {
                new AddTaskBottomSheet((title, category, dateString, notes) -> {
                    Log.d(TAG, "AddTask CB - Title: '" + title + "'");
                    Log.d(TAG, "AddTask CB - Category: '" + category + "'");
                    Log.d(TAG, "AddTask CB - Notes: '" + notes + "'");
                    Log.e("MAIN_ACTIVITY_DATE_DEBUG", "--------------------------------------------------");
                    Log.e("MAIN_ACTIVITY_DATE_DEBUG", "AddTask CB - Raw DateString from Picker: '" + dateString + "'");
                    Log.e("MAIN_ACTIVITY_DATE_DEBUG", "--------------------------------------------------");

                    if (title == null || title.trim().isEmpty()) {
                        Log.w(TAG, "Task title is empty, not adding task.");
                        return;
                    }

                    long dateMillis = 0;
                    if (dateString != null && !dateString.trim().isEmpty()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                        try {
                            Date parsedDate = sdf.parse(dateString.trim());
                            if (parsedDate != null) {
                                dateMillis = parsedDate.getTime();
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date string: '" + dateString + "' with format 'dd/MM/yyyy'", e);
                        }
                    }

                    Task newTask = new Task(
                            title.trim(),
                            category,
                            dateMillis,
                            notes != null ? notes.trim() : ""
                    );

                    Log.d(TAG, "New Task Created - ID: " + (newTask.getId() != null ? newTask.getId().toString() : "NULL ID!") +
                            ", Title: " + newTask.getTitle() +
                            ", Category: " + newTask.getCategory() +
                            ", Date (ms): " + newTask.getDate() +
                            ", Notes: " + newTask.getNote());

                    TaskStore.get(getApplicationContext()).addTask(newTask);
                    notifyTasksFragmentRefresh();
                }).show(getSupportFragmentManager(), AddTaskBottomSheet.TAG);
            });
        } else {
            Log.e(TAG, "bottomRightAddTaskButton is null. Check your app_bar_main.xml layout.");
        }

        // --- Navigation Setup ---
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_tasks, R.id.nav_stats, R.id.nav_pomodoro, R.id.nav_calendar)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // --- Show/hide FAB based on destination ---
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Log.d(TAG, "OnDestinationChanged - Current Destination ID: " + destination.getId());
            Log.d(TAG, "OnDestinationChanged - Current Destination Label: " + destination.getLabel());

            if (binding.appBarMain.bottomRightAddTaskButton == null) {
                Log.e(TAG, "OnDestinationChanged - bottomRightAddTaskButton is NULL!");
                return;
            }

            if (destination.getId() == R.id.nav_manage_categories) {
                binding.appBarMain.bottomRightAddTaskButton.setVisibility(View.GONE);
            } else if (destination.getId() == R.id.nav_tasks) {
                binding.appBarMain.bottomRightAddTaskButton.setVisibility(View.VISIBLE);
            } else {
                binding.appBarMain.bottomRightAddTaskButton.setVisibility(View.GONE);
            }
        });
    }

    private void notifyTasksFragmentRefresh() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            FragmentManager childFragmentManager = navHostFragment.getChildFragmentManager();
            Fragment currentFragment = childFragmentManager.getPrimaryNavigationFragment();
            if (currentFragment instanceof TasksFragment) {
                Log.d(TAG, "Found TasksFragment, calling refreshTaskList().");
                ((TasksFragment) currentFragment).refreshTaskList();
            } else {
                Log.w(TAG, "TasksFragment not currently primary navigation fragment.");
            }
        } else {
            Log.e(TAG, "NavHostFragment not found. Cannot refresh TasksFragment.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        if (NavigationUI.onNavDestinationSelected(item, navController)) {
            return true;
        }

        if (item.getItemId() == R.id.action_manage_categories) {
            navController.navigate(R.id.nav_manage_categories);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}


