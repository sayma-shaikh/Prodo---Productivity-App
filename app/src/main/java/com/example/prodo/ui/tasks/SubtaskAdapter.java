package com.example.prodo.ui.tasks;

import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView; // Needed for delete button
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.prodo.R;
import com.example.prodo.data.Subtask;
import java.util.ArrayList;
import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder> {

    private List<Subtask> subtasks = new ArrayList<>();
    private SubtaskListener listener;

    public interface SubtaskListener {
        void onSubtaskCheckedChanged(Subtask subtask, boolean isChecked);
        void onSubtaskDeleteClicked(Subtask subtask); // For deleting subtasks
    }

    public SubtaskAdapter(SubtaskListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Subtask> newSubtasks) {
        this.subtasks.clear();
        if (newSubtasks != null) {
            this.subtasks.addAll(newSubtasks);
        }
        // Use a defensive copy for internal storage if newSubtasks can be modified externally,
        // or ensure newSubtasks is always a fresh list.
        // For simplicity, current approach re-adds all. Consider DiffUtil for performance.
        notifyDataSetChanged();
        Log.d("SubtaskAdapter", "Submitted " + (this.subtasks != null ? this.subtasks.size() : 0) + " subtasks to adapter.");
    }

    @NonNull
    @Override
    public SubtaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subtask, parent, false);
        return new SubtaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubtaskViewHolder holder, int position) {
        Subtask currentSubtask = subtasks.get(position);
        holder.bind(currentSubtask, listener);
    }

    @Override
    public int getItemCount() {
        return subtasks != null ? subtasks.size() : 0;
    }

    static class SubtaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBoxDone;
        private TextView textViewTitle;
        private ImageView buttonDeleteSubtask; // ImageView for the delete button

        SubtaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxDone = itemView.findViewById(R.id.checkbox_subtask_done);
            textViewTitle = itemView.findViewById(R.id.textview_subtask_title);
            buttonDeleteSubtask = itemView.findViewById(R.id.button_delete_subtask); // Find the delete button
        }

        void bind(final Subtask subtask, final SubtaskListener listener) {
            if (subtask == null) {
                Log.e("SubtaskViewHolder", "bind called with null subtask for position: " + getAdapterPosition());
                // Optionally hide the view or show an error state
                itemView.setVisibility(View.GONE);
                return;
            }
            itemView.setVisibility(View.VISIBLE);

            textViewTitle.setText(subtask.getTitle());

            // --- CheckBox Listener ---
            // Remove previous listener to prevent it from firing during re-binding
            checkBoxDone.setOnCheckedChangeListener(null);
            checkBoxDone.setChecked(subtask.isDone());
            checkBoxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    // Update the subtask's state before calling the listener,
                    // or ensure listener knows it's the new state.
                    // subtask.setDone(isChecked); // Let the activity handle the data model change
                    listener.onSubtaskCheckedChanged(subtask, isChecked);
                }
            });

            // --- Strikethrough ---
            if (subtask.isDone()) {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textViewTitle.setPaintFlags(textViewTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // --- Delete Button Listener ---
            if (buttonDeleteSubtask != null) {
                buttonDeleteSubtask.setOnClickListener(v -> {
                    if (listener != null) {
                        Log.d("SubtaskViewHolder", "Delete button clicked for subtask: " + subtask.getTitle());
                        listener.onSubtaskDeleteClicked(subtask);
                    }
                });
            } else {
                Log.e("SubtaskViewHolder", "button_delete_subtask is null. Check ID in item_subtask.xml.");
            }
        }
    }
}
