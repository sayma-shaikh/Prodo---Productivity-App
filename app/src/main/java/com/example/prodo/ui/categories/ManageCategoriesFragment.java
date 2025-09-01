package com.example.prodo.ui.categories; // Adjust package if it's different

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // If using ViewModel for task updates
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.prodo.R;
import com.example.prodo.data.CategoryManager;
import com.example.prodo.data.Task; // For Task.UNCATEGORIZED
// import com.example.prodo.viewmodels.TaskViewModel; // If you have a TaskViewModel to update tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesFragment extends Fragment {

    private static final String TAG = "ManageCategoriesFrag";

    private RecyclerView recyclerViewCategories;
    private CategoryAdapter categoryAdapter;
    private CategoryManager categoryManager;
    private FloatingActionButton fabAddCategory;
    // private TaskViewModel taskViewModel; // Optional: For updating tasks when categories change

    public ManageCategoriesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        categoryManager = new CategoryManager(requireContext());
        // if (getActivity() != null) {
        // taskViewModel = new ViewModelProvider(getActivity()).get(TaskViewModel.class);
        // }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_manage_categories, container, false);

        recyclerViewCategories = view.findViewById(R.id.recyclerCategories);
        fabAddCategory = view.findViewById(R.id.fabAddCategory);

        setupRecyclerView();

        fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadCategories(); // Load categories when view is created
    }

    private void setupRecyclerView() {
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        // Initialize with an empty list, loadCategories will populate it
        categoryAdapter = new CategoryAdapter(new ArrayList<>(),
                (categoryName, action) -> { // Using a simple lambda for actions
                    if ("edit".equals(action)) {
                        showEditCategoryDialog(categoryName);
                    } else if ("delete".equals(action)) {
                        showDeleteCategoryConfirmDialog(categoryName);
                    }
                });
        recyclerViewCategories.setAdapter(categoryAdapter);
    }

    private void loadCategories() {
        if (categoryManager != null && categoryAdapter != null) {
            List<String> categories = categoryManager.getCategories();
            categoryAdapter.updateCategories(categories);
            Log.d(TAG, "Categories loaded: " + categories.size());
        } else {
            Log.e(TAG, "CategoryManager or CategoryAdapter is null in loadCategories.");
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Category");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_text, (ViewGroup) getView(), false);
        final TextInputLayout inputLayout = viewInflated.findViewById(R.id.inputLayout);
        final EditText input = viewInflated.findViewById(R.id.inputText);
        inputLayout.setHint("Category Name");
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, null); // Override below
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String categoryName = input.getText().toString().trim();
                if (categoryName.isEmpty()) {
                    inputLayout.setError("Category name cannot be empty");
                    return;
                }
                // Check for duplicates (case-insensitive) using CategoryManager logic
                if (!categoryManager.addCategory(categoryName)) {
                    // addCategory returns false if already exists (case-insensitive) or other issue
                    inputLayout.setError("Category already exists or invalid");
                    return;
                }
                inputLayout.setError(null);
                Toast.makeText(getContext(), "Category '" + categoryName + "' added", Toast.LENGTH_SHORT).show();
                loadCategories(); // Refresh the list
                // TODO: Consider notifying TasksFragment if it's alive to refresh its chips
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void showEditCategoryDialog(String oldCategoryName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Category");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_text, (ViewGroup) getView(), false);
        final TextInputLayout inputLayout = viewInflated.findViewById(R.id.inputLayout);
        final EditText input = viewInflated.findViewById(R.id.inputText);
        inputLayout.setHint("New Category Name");
        input.setText(oldCategoryName);
        input.setSelection(oldCategoryName.length()); // Place cursor at the end
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, null); // Override below
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newCategoryName = input.getText().toString().trim();
                if (newCategoryName.isEmpty()) {
                    inputLayout.setError("Category name cannot be empty");
                    return;
                }
                if (oldCategoryName.equalsIgnoreCase(newCategoryName)) {
                    dialog.dismiss(); // No change needed
                    return;
                }

                // updateCategory handles checks for new name already existing elsewhere
                if (categoryManager.updateCategory(oldCategoryName, newCategoryName)) {
                    Toast.makeText(getContext(), "Category updated to '" + newCategoryName + "'", Toast.LENGTH_SHORT).show();
                    // IMPORTANT: Update tasks in TaskStore/ViewModel
                    // if (taskViewModel != null) {
                    // taskViewModel.updateTasksCategoryBatch(oldCategoryName, newCategoryName);
                    // } else {
                    Log.w(TAG, "TaskViewModel is null. Cannot update tasks for category rename.");
                    // }
                    loadCategories(); // Refresh the list
                    // TODO: Notify TasksFragment
                    dialog.dismiss();
                } else {
                    inputLayout.setError("New name may already exist or is invalid");
                }
            });
        });
        dialog.show();
    }

    private void showDeleteCategoryConfirmDialog(String categoryName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete the category '" + categoryName + "'? Tasks in this category will become uncategorized.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (categoryManager.deleteCategory(categoryName)) {
                        Toast.makeText(getContext(), "Category '" + categoryName + "' deleted", Toast.LENGTH_SHORT).show();
                        // IMPORTANT: Update tasks in TaskStore/ViewModel to have null or "Uncategorized"
                        // if (taskViewModel != null) {
                        // taskViewModel.updateTasksCategoryBatch(categoryName, Task.UNCATEGORIZED); // Task.UNCATEGORIZED or null
                        // } else {
                        Log.w(TAG, "TaskViewModel is null. Cannot update tasks for category deletion.");
                        // }
                        loadCategories(); // Refresh the list
                        // TODO: Notify TasksFragment
                    } else {
                        Toast.makeText(getContext(), "Failed to delete category", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
