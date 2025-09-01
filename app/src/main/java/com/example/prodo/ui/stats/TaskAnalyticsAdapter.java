package com.example.prodo.ui.stats;  // adjust package if needed

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prodo.R;
import com.example.prodo.data.TaskAnalyticItem;

import java.util.Locale;

public class TaskAnalyticsAdapter extends ListAdapter<TaskAnalyticItem, TaskAnalyticsAdapter.AnalyticViewHolder> {

    public TaskAnalyticsAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<TaskAnalyticItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TaskAnalyticItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull TaskAnalyticItem oldItem, @NonNull TaskAnalyticItem newItem) {
                    // Items are the same if they refer to the same task on the same date
                    return oldItem.getTaskTitle().equals(newItem.getTaskTitle()) &&
                           oldItem.getDate().equals(newItem.getDate());
                }

                @Override
                public boolean areContentsTheSame(@NonNull TaskAnalyticItem oldItem, @NonNull TaskAnalyticItem newItem) {
                    // Contents are the same if all fields (including pomodoro count and time) are equal
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    @Override
    public AnalyticViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_analytic, parent, false);
        return new AnalyticViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AnalyticViewHolder holder, int position) {
        TaskAnalyticItem item = getItem(position);
        holder.taskTitle.setText(item.getTaskTitle());
        holder.pomodoroCount.setText(String.format(Locale.getDefault(), "Pomodoros: %d", item.getPomodoroCount()));

        long millis = item.getTotalTimeSpentMillis();
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis / (1000 * 60)) % 60;
        holder.totalTime.setText(String.format(Locale.getDefault(), "Time: %dh %02dm", hours, minutes));

        // Bind the date
        if (item.getDate() != null && holder.taskDate != null) {
            holder.taskDate.setText(item.getDate());
        } else if (holder.taskDate != null) {
            holder.taskDate.setText("Date: N/A"); // Fallback if date is null
        }
    }

    static class AnalyticViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, pomodoroCount, totalTime, taskDate; // Added taskDate
        // View viewColor; // Uncomment if adding color dot

        AnalyticViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.tvAnalyticTaskTitle);
            pomodoroCount = itemView.findViewById(R.id.tvAnalyticPomodoroCount);
            totalTime = itemView.findViewById(R.id.tvAnalyticTimeSpent); // Changed variable name to totalTime for clarity
            taskDate = itemView.findViewById(R.id.tvAnalyticDate); // Initialize taskDate TextView
            // viewColor = itemView.findViewById(R.id.viewColor);
        }
    }
}

