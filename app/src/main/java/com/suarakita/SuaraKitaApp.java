package com.suarakita;

import android.app.Application;

import com.suarakita.api.RetrofitClient;

public class SuaraKitaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}
