package com.example.mz.dialogprinter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.vk.sdk.VKSdk;

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
