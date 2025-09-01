// MyApplication.java
package com.example.prodo; // your app's package

import android.app.Application;
import com.example.prodo.data.TaskStore;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize TaskStore by calling get() with context.
        // The instance will be created and tasks loaded if it's the first time.
        TaskStore.get(getApplicationContext());
    }
}

