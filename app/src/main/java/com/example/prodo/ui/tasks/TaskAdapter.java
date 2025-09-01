package com.example.prodo.ui.tasks; // Ensure this package is correct

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prodo.R; // Ensure this R file path is correct
import com.example.prodo.data.Task; // Ensure this path is correct
// import com.example.prodo.ui.tasks.TaskDetailActivity; // Ensure this path is correct

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    public interface Listener {
        void onToggleFlag(Task task);
        // void onTaskClicked(Task task); // You might not need this if itemView click goes to Detail
        void onTaskCompleted(Task task);
        void onDeleteTask(Task task);
    }

    private final Listener listener;

    public TaskAdapter(Listener listener) { // Constructor requires a Listener
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Task>() {
                @Override
                public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    // Make sure Task has a reliable getId() method
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    // Make sure Task has a proper equals() method implemented
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = getItem(position);

        if (currentTask == null) {
            return; // Should not happen with ListAdapter if list is managed properly
        }

        holder.title.setText(currentTask.getTitle());

        if (currentTask.getNote() != null && !currentTask.getNote().isEmpty()) {
            holder.sub.setText(currentTask.getNote());
            holder.sub.setVisibility(View.VISIBLE);
        } else {
            holder.sub.setVisibility(View.GONE);
        }

        // Apply or remove strikethrough based on task completion
        if (currentTask.isDone()) {
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.title.setPaintFlags(holder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.check.setOnCheckedChangeListener(null); // Important: remove previous listener
        holder.check.setChecked(currentTask.isDone());
        holder.check.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                // The task's 'done' state will be updated by the listener implementation
                listener.onTaskCompleted(currentTask);
            }
        });

        holder.flag.setImageResource(
                currentTask.isFlagged() ? android.R.drawable.btn_star_big_on
                        : android.R.drawable.btn_star_big_off
        );
        holder.flag.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleFlag(currentTask);
            }
        });

        if (holder.buttonDeleteItem != null) {
            holder.buttonDeleteItem.setOnClickListener(v -> {
                Log.d("TaskAdapter", "Delete icon clicked for task: " + currentTask.getTitle());
                if (listener != null) {
                    listener.onDeleteTask(currentTask);
                }
            });
        } else {
            Log.e("TaskAdapter", "button_delete_item in ViewHolder is null. Check item_task.xml ID.");
        }

        // Handle item click to go to detail view
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, TaskDetailActivity.class);
            intent.putExtra("task_id", currentTask.getId().toString()); // Assuming getId() returns UUID, convert to String
            context.startActivity(intent);
        });
    }

    // Renamed from VH to TaskViewHolder for clarity, matching ListAdapter convention
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox check;
        TextView title;
        TextView sub;
        ImageView flag;
        ImageView buttonDeleteItem;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.check);
            title = itemView.findViewById(R.id.title);
            sub = itemView.findViewById(R.id.sub);
            flag = itemView.findViewById(R.id.flag);
            buttonDeleteItem = itemView.findViewById(R.id.button_delete_item);
        }
    }
}
