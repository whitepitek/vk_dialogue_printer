package com.example.mz.dialogprinter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiOwner;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity  {

    private int AUTH_REQUEST = 0;
    private int EXPORT_REQUEST = 1;


    private void checkAuthorized() {
        if(!VKSdk.isLoggedIn()) {
            authorize();
        } else {
            onAuthorized();
        }
    }
    private void onAuthorized() {
        Intent intent = new Intent(this, ExportActivity.class);
        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkAuthorized();
    }

    private void authorize() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivityForResult(intent, AUTH_REQUEST);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAuthorized();
    }

}
