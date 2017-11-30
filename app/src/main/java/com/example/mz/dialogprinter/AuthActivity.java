package com.example.mz.dialogprinter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONException;
import org.json.JSONObject;

import static com.example.mz.dialogprinter.MainApp.getPref;
import static com.example.mz.dialogprinter.MainApp.getVkQueue;

public class AuthActivity extends AppCompatActivity {
    static public void getName() {
        VKRequest request = new VKRequest("account.getProfileInfo");
        getVkQueue().addRequest(request, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                String name, surname;
                try {
                    JSONObject jsonResponse = response.json.getJSONObject("response");
                    name = jsonResponse.getString("first_name");
                    surname = jsonResponse.getString("last_name");
                } catch (JSONException e) {
                    name = "Error getting name";
                    surname = "";
                }
                SharedPreferences pref = getPref();
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("name", name);
                editor.putString("surname", surname);
                editor.commit();
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                res.save();
                getName();
                AuthActivity.this.finish();
            }
            @Override
            public void onError(VKError error) {
                Toast.makeText(getApplicationContext(), error.apiError.toString(), Toast.LENGTH_LONG).show();
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    public void authorize(View view) {
        VKSdk.login(this, VKScope.MESSAGES, VKScope.OFFLINE, VKScope.DOCS);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

    }
}
