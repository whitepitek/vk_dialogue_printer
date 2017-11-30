package com.example.mz.dialogprinter;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.vk.sdk.VKSdk;


/**
 * Created by mz on 6/30/17.
 */

public class MainApp extends Application {
    private static Application app;
    private static final String PREF_NAME = "pref";
    private static VkQueue vkQueue = new VkQueue();

    public static Application getApp() {
        return app;
    }
    public static SharedPreferences getPref() {
        return app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    public static VkQueue getVkQueue() {
        if(!vkQueue.isRunning())
            vkQueue.start();
        return vkQueue;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        //VKSdk.customInitialize(getApplicationContext(), R.integer.com_vk_sdk_AppId, "5.69");
        VKSdk.initialize(getApplicationContext());
    }
}
