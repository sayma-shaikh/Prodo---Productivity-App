package com.example.prodo.ui.categories; // Or the package where your ManageCategoriesFragment is

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.prodo.R; // Make sure this R is your project's R

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<String> categories;
    private OnCategoryActionListener listener;

    // Listener interface for actions (edit, delete)
    public interface OnCategoryActionListener {
        void onCategoryAction(String categoryName, String action); // "edit" or "delete"
    }

    public CategoryAdapter(List<String> categories, OnCategoryActionListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String categoryName = categories.get(position);
        holder.textViewCategoryName.setText(categoryName);

        holder.buttonEditCategory.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryAction(categoryName, "edit");
            }
        });

        holder.buttonDeleteCategory.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryAction(categoryName, "delete");
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    // Method to update the list of categories in the adapter
    public void updateCategories(List<String> newCategories) {
        this.categories.clear();
        if (newCategories != null) {
            this.categories.addAll(newCategories);
        }
        notifyDataSetChanged(); // Important: Notifies RecyclerView to refresh
    }

    // ViewHolder class
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCategoryName;
        ImageButton buttonEditCategory;
        ImageButton buttonDeleteCategory;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCategoryName = itemView.findViewById(R.id.textViewCategoryName);
            buttonEditCategory = itemView.findViewById(R.id.buttonEditCategory);
            buttonDeleteCategory = itemView.findViewById(R.id.buttonDeleteCategory);
        }
    }
}
