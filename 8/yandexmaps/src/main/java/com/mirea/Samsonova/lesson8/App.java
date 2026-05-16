package com.mirea.Samsonova.lesson8;

import android.app.Application;

import com.yandex.mapkit.MapKitFactory;

public class App extends Application {

    private static final String MAPKIT_API_KEY = "PASTE_YOUR_YANDEX_MAPKIT_API_KEY_HERE";

    @Override
    public void onCreate() {
        super.onCreate();

        MapKitFactory.setApiKey(MAPKIT_API_KEY);
    }
}