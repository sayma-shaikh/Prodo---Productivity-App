package com.example.prodo.data; // Make sure this package is correct for your project

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet; // To maintain order and uniqueness
import java.util.List;
import java.util.Set;

public class CategoryManager {
    private static final String PREFS_NAME = "CategoryPrefs_ProdoApp"; // Added app name for uniqueness
    private static final String KEY_CATEGORIES = "user_defined_categories_list";
    private static final String TAG = "CategoryManager";

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public CategoryManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null for CategoryManager");
        }
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * Retrieves the list of user-defined categories.
     * Returns default categories if none are saved or if there's an error.
     * The returned list is mutable.
     */
    public List<String> getCategories() {
        String jsonCategories = sharedPreferences.getString(KEY_CATEGORIES, null);
        if (jsonCategories != null) {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            try {
                ArrayList<String> categories = gson.fromJson(jsonCategories, type);
                if (categories != null) {
                    // Use LinkedHashSet to remove duplicates while preserving insertion order
                    return new ArrayList<>(new LinkedHashSet<>(categories));
                } else {
                    Log.w(TAG, "Parsed categories list is null. Returning default.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing categories from JSON. Returning default.", e);
            }
        } else {
            Log.d(TAG, "No saved categories found. Returning default.");
        }
        // Return a mutable copy of default categories if nothing is saved or error occurs
        return new ArrayList<>(getDefaultCategories());
    }

    /**
     * Saves the given list of categories. Duplicates are removed before saving.
     * @param categories The list of categories to save.
     */
    public void saveCategories(List<String> categories) {
        if (categories == null) {
            Log.w(TAG, "Attempted to save a null list of categories. Saving empty list instead.");
            categories = new ArrayList<>();
        }
        // Use LinkedHashSet to ensure uniqueness and maintain original order as much as possible
        Set<String> uniqueCategories = new LinkedHashSet<>(categories);
        String jsonCategories = gson.toJson(new ArrayList<>(uniqueCategories));
        sharedPreferences.edit().putString(KEY_CATEGORIES, jsonCategories).apply();
        Log.d(TAG, "Categories saved: " + jsonCategories);
    }

    /**
     * Adds a new category to the list.
     * @param categoryName The name of the category to add.
     * @return true if the category was added successfully, false if the name is invalid,
     *         empty, or already exists.
     */
    public boolean addCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            Log.w(TAG, "Attempted to add an empty or null category name.");
            return false;
        }
        categoryName = categoryName.trim();
        List<String> categories = getCategories(); // getCategories now handles returning defaults or existing

        // Case-insensitive check for existence
        for (String existingCategory : categories) {
            if (existingCategory.equalsIgnoreCase(categoryName)) {
                Log.w(TAG, "Category '" + categoryName + "' already exists (case-insensitive). Cannot add.");
                return false; // Category already exists (case-insensitive)
            }
        }

        categories.add(categoryName);
        saveCategories(categories);
        Log.d(TAG, "Category added: " + categoryName);
        return true;
    }

    /**
     * Updates an existing category name.
     * @param oldCategoryName The current name of the category.
     * @param newCategoryName The new name for the category.
     * @return true if updated successfully, false if names are invalid, the same,
     *         the old category doesn't exist, or the new category name already exists elsewhere.
     */
    public boolean updateCategory(String oldCategoryName, String newCategoryName) {
        if (oldCategoryName == null || oldCategoryName.trim().isEmpty() ||
                newCategoryName == null || newCategoryName.trim().isEmpty()) {
            Log.w(TAG, "Old or new category name is null or empty. Cannot update.");
            return false;
        }

        oldCategoryName = oldCategoryName.trim();
        newCategoryName = newCategoryName.trim();

        if (oldCategoryName.equalsIgnoreCase(newCategoryName)) {
            Log.d(TAG, "Old and new category names are the same (case-insensitive). No update needed.");
            return false; // No change needed
        }

        List<String> categories = getCategories();

        int oldNameIndex = -1;
        for(int i = 0; i < categories.size(); i++) {
            if (categories.get(i).equalsIgnoreCase(oldCategoryName)) {
                oldNameIndex = i;
                break;
            }
        }

        if (oldNameIndex == -1) {
            Log.w(TAG, "Old category name '" + oldCategoryName + "' not found. Cannot update.");
            return false; // Old category name not found
        }

        // Check if new name already exists (excluding the current position being updated)
        for (int i = 0; i < categories.size(); i++) {
            if (i != oldNameIndex && categories.get(i).equalsIgnoreCase(newCategoryName)) {
                Log.w(TAG, "New category name '" + newCategoryName + "' already exists elsewhere. Cannot update.");
                return false; // New category name already exists at a different position
            }
        }

        categories.set(oldNameIndex, newCategoryName);
        saveCategories(categories);
        Log.d(TAG, "Category updated from '" + oldCategoryName + "' to '" + newCategoryName + "'.");
        // Remember to update tasks associated with oldCategoryName elsewhere in your app.
        return true;
    }

    /**
     * Deletes a category from the list.
     * @param categoryName The name of the category to delete.
     * @return true if the category was deleted successfully, false otherwise.
     */
    public boolean deleteCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            Log.w(TAG, "Attempted to delete an empty or null category name.");
            return false;
        }
        categoryName = categoryName.trim();
        List<String> categories = getCategories();

        boolean removed = false;
        // Case-insensitive removal
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).equalsIgnoreCase(categoryName)) {
                categories.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            saveCategories(categories);
            Log.d(TAG, "Category deleted: " + categoryName);
            // Remember to update tasks associated with categoryName elsewhere in your app.
        } else {
            Log.w(TAG, "Category '" + categoryName + "' not found for deletion.");
        }
        return removed;
    }

    /**
     * Provides a default list of categories if no categories are saved yet.
     * @return A list of default category names.
     */
    private List<String> getDefaultCategories() {
        List<String> defaults = new ArrayList<>();
        defaults.add("Personal");
        defaults.add("Work");
        defaults.add("Shopping");
        defaults.add("Wishlist");
        Log.d(TAG, "Providing default categories.");
        return defaults;
    }

    /**
     * Clears all saved categories and reverts to defaults on next getCategories() call if empty.
     * USE WITH CAUTION.
     */
    public void clearAllCategories() {
        sharedPreferences.edit().remove(KEY_CATEGORIES).apply();
        Log.i(TAG, "All user-defined categories have been cleared.");
    }
}
