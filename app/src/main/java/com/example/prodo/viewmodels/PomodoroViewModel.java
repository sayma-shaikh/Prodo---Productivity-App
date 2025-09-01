package com.example.prodo.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PomodoroViewModel extends AndroidViewModel {

    private static final String TAG = "PomodoroViewModel";

    // Configurable Durations
    private static final long DEFAULT_WORK_DURATION_MIN = 25;
    private static final long DEFAULT_SHORT_BREAK_DURATION_MIN = 5;
    private static final long DEFAULT_LONG_BREAK_DURATION_MIN = 15;

    public enum TimerSessionMode {
        WORK,
        SHORT_BREAK,
        LONG_BREAK
    }

    public enum TimerState {
        STOPPED,
        RUNNING,
        PAUSED
    }

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private long currentModeTotalDurationMillis;

    private final MutableLiveData<TimerState> _timerState = new MutableLiveData<>(TimerState.STOPPED);
    public final LiveData<TimerState> timerState = _timerState;

    private final MutableLiveData<TimerSessionMode> _currentSessionMode = new MutableLiveData<>(TimerSessionMode.WORK);
    public final LiveData<TimerSessionMode> currentSessionMode = _currentSessionMode;

    private final MutableLiveData<String> _timeDisplay = new MutableLiveData<>();
    public final LiveData<String> timeDisplay = _timeDisplay;

    private final MutableLiveData<Integer> _completedPomodorosTodayDisplay = new MutableLiveData<>(0);
    public final LiveData<Integer> completedPomodorosTodayDisplay = _completedPomodorosTodayDisplay;

    // --- NEW: For tracking selected task for stats ---
    private String currentTaskTitleForStats = null;
    // --------------------------------------------------

    public PomodoroViewModel(@NonNull Application application) {
        super(application);
        setMode(TimerSessionMode.WORK, false);
        loadCompletedPomodorosForTodayDisplay();
    }

    // --- NEW: Method to set the currently selected task title from Fragment ---
    public void setSelectedTaskTitleForStats(String taskTitle) {
        this.currentTaskTitleForStats = taskTitle;
        Log.d(TAG, "Selected task title for stats set to: " + (taskTitle != null ? taskTitle : "null"));
    }
    // --------------------------------------------------------------------

    private void setMode(TimerSessionMode mode, boolean autoStart) {
        Log.d(TAG, "Setting mode to: " + mode + ", AutoStart: " + autoStart);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        _currentSessionMode.setValue(mode);

        switch (mode) {
            case WORK:
                currentModeTotalDurationMillis = TimeUnit.MINUTES.toMillis(DEFAULT_WORK_DURATION_MIN);
                break;
            case SHORT_BREAK:
                currentModeTotalDurationMillis = TimeUnit.MINUTES.toMillis(DEFAULT_SHORT_BREAK_DURATION_MIN);
                break;
            case LONG_BREAK:
                currentModeTotalDurationMillis = TimeUnit.MINUTES.toMillis(DEFAULT_LONG_BREAK_DURATION_MIN);
                break;
            default:
                currentModeTotalDurationMillis = TimeUnit.MINUTES.toMillis(DEFAULT_WORK_DURATION_MIN);
        }
        timeLeftInMillis = currentModeTotalDurationMillis;
        _timerState.setValue(TimerState.STOPPED);
        updateDisplayTime();

        if (autoStart) {
            startTimer();
        }
    }

    public void setWorkMode() {
        setMode(TimerSessionMode.WORK, false);
    }

    public void setShortBreakMode() {
        setMode(TimerSessionMode.SHORT_BREAK, false);
    }

    public void setLongBreakMode() {
        setMode(TimerSessionMode.LONG_BREAK, false);
    }

    public void startTimer() {
        TimerState currentState = _timerState.getValue();
        if (currentState == TimerState.RUNNING) {
            Log.w(TAG, "Timer is already running.");
            return;
        }

        if (currentState == TimerState.STOPPED) {
            timeLeftInMillis = currentModeTotalDurationMillis;
        }
        if (timeLeftInMillis == 0) {
            timeLeftInMillis = currentModeTotalDurationMillis;
        }

        Log.d(TAG, "startTimer called. Current state: " + currentState + ", Time left: " + timeLeftInMillis + "ms for mode " + _currentSessionMode.getValue());
        _timerState.setValue(TimerState.RUNNING);
        startCountdown();
    }

    public void pauseTimer() {
        if (_timerState.getValue() != TimerState.RUNNING) {
            Log.w(TAG, "PauseTimer called when timer not running. State: " + _timerState.getValue());
            return;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        _timerState.setValue(TimerState.PAUSED);
        Log.d(TAG, "Timer paused. Time left: " + timeLeftInMillis + " ms. New state: " + _timerState.getValue());
    }

    public void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = currentModeTotalDurationMillis;
        _timerState.setValue(TimerState.STOPPED);
        updateDisplayTime();
        Log.d(TAG, "Timer reset for mode " + _currentSessionMode.getValue() + ". New state: STOPPED.");
    }

    private void startCountdown() {
        Log.d(TAG, "Starting countdown for " + timeLeftInMillis + " ms. Current mode: " + _currentSessionMode.getValue());
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateDisplayTime();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateDisplayTime();
                Log.d(TAG, "Countdown finished for mode: " + _currentSessionMode.getValue());
                handleSessionFinish();
            }
        }.start();
    }

    private void handleSessionFinish() {
        TimerSessionMode finishedMode = _currentSessionMode.getValue();
        Log.d(TAG, "handleSessionFinish. Finished mode: " + finishedMode);

        if (finishedMode == TimerSessionMode.WORK) {
            saveDailyPomodoroCount(); // This will now also save task-specific count
            setMode(TimerSessionMode.SHORT_BREAK, true);
        } else if (finishedMode == TimerSessionMode.SHORT_BREAK || finishedMode == TimerSessionMode.LONG_BREAK) {
            setMode(TimerSessionMode.WORK, false);
        } else {
            Log.w(TAG, "handleSessionFinish called in unexpected mode: " + finishedMode);
            setMode(TimerSessionMode.WORK, false);
        }
    }

    private void updateDisplayTime() {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) % 60;
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        _timeDisplay.setValue(formattedTime);
    }

    // --- NEW: safeKey method, same as in StatsViewModel ---
    // Consider moving to a shared utility class if used elsewhere too
    private String safeKey(String title) {
        if (title == null || title.trim().isEmpty()) {
            Log.w(TAG, "safeKey received null or empty title, returning 'untitled'");
            return "untitled";
        }
        // Consistent with StatsViewModel: allow underscore, dot, hyphen
        return title.trim().toLowerCase(Locale.getDefault()).replaceAll("[^a-z0-9_.-]+", "_");
    }
    // ----------------------------------------------------

    private void saveDailyPomodoroCount() {
        Context context = getApplication().getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("ProdoStats", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Calendar today = Calendar.getInstance();
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDateString = dateKeyFormat.format(today.getTime());

        // Save/Update total pomodoros for the day
        String totalDailyPomosKey = "pomos_" + todayDateString;
        int currentPomosForDay = prefs.getInt(totalDailyPomosKey, 0);
        int newPomosForDay = currentPomosForDay + 1;
        editor.putInt(totalDailyPomosKey, newPomosForDay);
        _completedPomodorosTodayDisplay.setValue(newPomosForDay);

        Log.i(TAG, "Saved total daily pomodoro. Key: '" + totalDailyPomosKey +
                "', New count: " + newPomosForDay);

        // ---- MODIFIED: Save pomodoro for the specific task if one is selected ----
        if (currentTaskTitleForStats != null && !currentTaskTitleForStats.trim().isEmpty()) {
            String taskSafeKeyValue = safeKey(currentTaskTitleForStats);
            String taskSpecificDayKey = "pomos_" + todayDateString + "_" + taskSafeKeyValue;

            int currentPomosForTaskDay = prefs.getInt(taskSpecificDayKey, 0);
            int newPomosForTaskDay = currentPomosForTaskDay + 1;
            editor.putInt(taskSpecificDayKey, newPomosForTaskDay);

            Log.i(TAG, "Saved pomodoro for specific task. Key: '" + taskSpecificDayKey +
                    "', Title: '" + currentTaskTitleForStats +
                    "', New count: " + newPomosForTaskDay);
        } else {
            Log.d(TAG, "No specific task selected for stats, only daily total pomodoro saved for key: " + totalDailyPomosKey);
        }
        // ----------------------------------------------------------------------

        boolean success = editor.commit(); // Commit all changes at once
        if (!success) {
            Log.e(TAG, "Failed to commit SharedPreferences changes for pomodoro counts!");
        } else {
            Log.d(TAG, "Successfully committed pomodoro counts to SharedPreferences.");
        }
    }

    private void loadCompletedPomodorosForTodayDisplay() {
        Context context = getApplication().getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("ProdoStats", Context.MODE_PRIVATE);
        Calendar today = Calendar.getInstance();
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDayKey = "pomos_" + dateKeyFormat.format(today.getTime());
        int pomosToday = prefs.getInt(currentDayKey, 0);
        _completedPomodorosTodayDisplay.setValue(pomosToday);
        Log.d(TAG, "Loaded completed pomodoros for today display (" + currentDayKey + "): " + pomosToday);
    }

    public long getWorkSessionDurationMillis() {
        return TimeUnit.MINUTES.toMillis(DEFAULT_WORK_DURATION_MIN);
    }

    public long getCurrentModeTotalDurationMillis() {
        return currentModeTotalDurationMillis;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        Log.d(TAG, "PomodoroViewModel cleared. Timer cancelled.");
    }
}

