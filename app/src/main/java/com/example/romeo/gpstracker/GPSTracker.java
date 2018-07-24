package com.example.romeo.gpstracker;

import android.app.Application;
import android.content.Intent;

public class GPSTracker extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this,MyService.class));
    }
}
