package com.example.prodo.ui.tasks;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText; // Required for dialog input
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // Required for AlertDialog
import com.example.prodo.R;
import com.example.prodo.data.CategoryManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // Required for dialog input layout

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AddTaskBottomSheet";
    private static final String ADD_NEW_CATEGORY_OPTION = "+ New Category";

    private TextInputEditText editTextTitle;
    private AutoCompleteTextView autoCompleteCategory;
    private TextInputEditText editTextDate;
    private TextInputEditText editTextNotes;
    private Button buttonSaveTask;

    private OnTaskAddedListener listener;
    private CategoryManager categoryManager;
    private ArrayAdapter<String> categoryAdapter;
    private List<String> categoryDisplayList;
    private Calendar selectedDateCalendar;


    public interface OnTaskAddedListener {
        // Updated to reflect the parameters we are actually collecting
        void onTaskAdded(String title, String category, String dateString, String notes);
    }

    // Constructor or a newInstance method to set the listener
    public AddTaskBottomSheet(OnTaskAddedListener listener) {
        this.listener = listener;
    }

    public AddTaskBottomSheet() {
        // Default constructor, listener should be set via setter or newInstance if used this way
    }

    public void setOnTaskAddedListener(OnTaskAddedListener listener) {
        this.listener = listener;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_task, container, false);

        editTextTitle = view.findViewById(R.id.editTextTitle);
        autoCompleteCategory = view.findViewById(R.id.autoCompleteCategory);
        editTextDate = view.findViewById(R.id.editTextDate);
        editTextNotes = view.findViewById(R.id.editTextNotes);
        buttonSaveTask = view.findViewById(R.id.buttonSaveTask);

        categoryManager = new CategoryManager(requireContext());
        selectedDateCalendar = Calendar.getInstance(); // Initialize with current date

        setupCategoryDropdown();
        setupDatePicker();

        buttonSaveTask.setOnClickListener(v -> saveTask());

        return view;
    }

    private void setupCategoryDropdown() {
        categoryDisplayList = new ArrayList<>(categoryManager.getCategories());
        // Ensure ADD_NEW_CATEGORY_OPTION is not duplicated if setupCategoryDropdown is called multiple times
        categoryDisplayList.remove(ADD_NEW_CATEGORY_OPTION);
        categoryDisplayList.add(ADD_NEW_CATEGORY_OPTION);

        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryDisplayList);
        autoCompleteCategory.setAdapter(categoryAdapter);
        autoCompleteCategory.setThreshold(0); // Show all items on first click

        autoCompleteCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = categoryAdapter.getItem(position);
            if (selectedItem != null && selectedItem.equals(ADD_NEW_CATEGORY_OPTION)) {
                autoCompleteCategory.setText("", false); // Clear the "+ New Category" text
                showAddNewCategoryDialogFromSheet(newCategoryName -> {
                    // This callback is invoked after the new category is added and saved
                    autoCompleteCategory.setText(newCategoryName, false); // Set the new category in the text field
                    autoCompleteCategory.dismissDropDown(); // Optionally dismiss dropdown
                    Log.d(TAG, "New category '" + newCategoryName + "' selected in AutoCompleteTextView.");
                });
            } else {
                Log.d(TAG, "Existing category '" + selectedItem + "' selected.");
            }
        });
    }

    private void setupDatePicker() {
        // Set initial date text
        updateDateEditText();

        editTextDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (datePicker, year, month, day) -> {
                        selectedDateCalendar.set(Calendar.YEAR, year);
                        selectedDateCalendar.set(Calendar.MONTH, month);
                        selectedDateCalendar.set(Calendar.DAY_OF_MONTH, day);
                        updateDateEditText();
                    },
                    selectedDateCalendar.get(Calendar.YEAR),
                    selectedDateCalendar.get(Calendar.MONTH),
                    selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void updateDateEditText() {
        if (editTextDate != null) { // Good practice to check
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()); // Or your preferred format
            editTextDate.setText(sdf.format(selectedDateCalendar.getTime()));
            Log.d(TAG, "Date display updated in editTextDate to: " + editTextDate.getText().toString());
        }
    }

    private interface OnNewCategoryAddedListenerInternal {
        void onCategoryAdded(String newCategoryName);
    }

    private void showAddNewCategoryDialogFromSheet(OnNewCategoryAddedListenerInternal newCategoryListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Category");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_text, (ViewGroup) getView(), false);
        final EditText input = viewInflated.findViewById(R.id.inputText);
        final TextInputLayout inputLayout = viewInflated.findViewById(R.id.inputLayout);
        inputLayout.setHint("Category Name");
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, null); // Will override later
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String categoryName = input.getText().toString().trim();
                if (categoryName.isEmpty()) {
                    inputLayout.setError("Category name cannot be empty");
                    return;
                }
                if (categoryManager.getCategories().contains(categoryName)) {
                    inputLayout.setError("Category already exists");
                    return;
                }
                inputLayout.setError(null); // Clear previous error

                if (categoryManager.addCategory(categoryName)) {
                    Log.d(TAG, "New category added: " + categoryName);
                    setupCategoryDropdown(); // Refresh the list in AutoCompleteTextView
                    if (newCategoryListener != null) {
                        newCategoryListener.onCategoryAdded(categoryName);
                    }
                    dialog.dismiss();
                } else {
                    inputLayout.setError("Failed to add category. Please try again.");
                }
            });
        });
        dialog.show();
    }


    private void saveTask() {
        Editable titleEditable = editTextTitle.getText();
        Editable categoryEditable = autoCompleteCategory.getText(); // Get Editable first
        Editable dateEditable = editTextDate.getText();
        Editable notesEditable = editTextNotes.getText();

        String title = (titleEditable != null) ? titleEditable.toString().trim() : "";
        String category = (categoryEditable != null) ? categoryEditable.toString().trim() : ""; // Then convert and trim
        String dateString = (dateEditable != null) ? dateEditable.toString().trim() : "";
        String notes = (notesEditable != null) ? notesEditable.toString().trim() : "";

        // --- The rest of your validation and listener call ---
        if (TextUtils.isEmpty(title)) {
            TextInputLayout titleLayout = (TextInputLayout) editTextTitle.getParent().getParent();
            if (titleLayout != null) titleLayout.setError("Title cannot be empty");
            return;
        } else {
            TextInputLayout titleLayout = (TextInputLayout) editTextTitle.getParent().getParent();
            if (titleLayout != null) titleLayout.setError(null);
        }

        if (category.equals(ADD_NEW_CATEGORY_OPTION) || category.isEmpty()) {
            TextInputLayout categoryLayout = (TextInputLayout) autoCompleteCategory.getParent().getParent();
            if (categoryLayout != null) categoryLayout.setError("Please select or add a category");
            return;
        } else {
            TextInputLayout categoryLayout = (TextInputLayout) autoCompleteCategory.getParent().getParent();
            if (categoryLayout != null) categoryLayout.setError(null);
        }

        if (listener != null) {
            // The listener was defined as:
            // void onTaskAdded(String title, String category, String dateString, String notes);
            // The parameters in the previous version of AddTaskBottomSheet might have been different
            // Ensure the parameters match the listener interface in AddTaskBottomSheet
            // and the lambda implementation in MainActivity.
            // My previous example for AddTaskBottomSheet listener was:
            // void onTaskAdded(String title, String category, String dateString, boolean repeat, String notes);
            // The 'repeat' boolean was there. If you removed it from the listener interface, this is fine.
            // If your MainActivity expects `repeat`, you need to pass it or update MainActivity's lambda.
            // For now, assuming your listener is: (String title, String category, String dateString, String notes)
            listener.onTaskAdded(title, category, dateString, notes);
        }
        dismiss();
    }

}

