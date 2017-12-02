package com.example.mz.dialogprinter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;

import static com.example.mz.dialogprinter.MainApp.getApp;
import static com.example.mz.dialogprinter.MainApp.getPref;
import static com.example.mz.dialogprinter.MainApp.getVkQueue;

public class AuthActivity extends AppCompatActivity {
    static private String username = null;
    static private final Object usernameSync = new Object();
    static public String getUsername() {
        synchronized (usernameSync) {
            if (username == null) {
                SharedPreferences pref = getPref();
                String name = pref.getString("name", null);
                String surname = pref.getString("surname", null);
                if(name == null || surname == null) {
                    return null;
                }
                username = name + " " + surname;
                return username;
            }
            return username;
        }
    }

    static private Bitmap userpic = null;
    static private final Object userpicSync = new Object();
    static public Bitmap getUserpic() {
        synchronized (userpicSync) {
            if (userpic != null)
                return userpic;
        }
            File cacheDir = getApp().getCacheDir();
            File picFile = new File(cacheDir, "userpic.png");
            try {
                FileInputStream input = new FileInputStream(picFile);
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                synchronized (userpicSync) {
                    userpic = bitmap;
                    return userpic;
                }
            } catch (Exception ex) {
                return null;
            }
        }

    static private class PicDownloader extends AsyncTask<String, Object, Boolean> {
        private Bitmap bitmap = null;
        @Override
        protected Boolean doInBackground(String... params) {
            String address = params[0];
            try {
                URL url = new URL(address);
                URLConnection connection = url.openConnection();
                bitmap = BitmapFactory.decodeStream(connection.getInputStream());
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(bitmap == null)
                return;
            synchronized (userpicSync) {
                userpic = bitmap;
            }
            ExportActivity activity = ExportActivity.getCurrent();
            if(activity != null) {
                activity.showProfile();
            }
            File cacheDir = getApp().getCacheDir();
            File picFile = new File(cacheDir, "userpic.png");
            try {
                FileOutputStream out = new FileOutputStream(picFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch(Exception e) {
                // do nothing
            }
        }
    }

    static private void downloadUserpic(String address) {
        (new PicDownloader()).execute(address);
    }

    static public void getName() {
        VKRequest request = new VKRequest("users.get", VKParameters.from(
                "fields", "photo_200",
                "name_case", "Nom"
        ));
        getVkQueue().addRequest(request, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                String name, surname, photoAddress;
                try {
                    JSONObject jsonResponse = response.json.getJSONArray("response").getJSONObject(0);
                    name = jsonResponse.getString("first_name");
                    surname = jsonResponse.getString("last_name");
                    photoAddress = jsonResponse.getString("photo_200");
                } catch (JSONException e) {
                    name = null;
                    surname = null;
                    photoAddress = null;
                }
                if(photoAddress != null)
                    downloadUserpic(photoAddress);
                if(name != null && surname != null) {
                    synchronized (usernameSync) {
                        username = name + " " + surname;
                    }
                    ExportActivity activity = ExportActivity.getCurrent();
                    if (activity != null) {
                        activity.showProfile();
                    }
                    SharedPreferences pref = getPref();
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("name", name);
                    editor.putString("surname", surname);
                    editor.apply();
                }
            }
            @Override
            public void onError(VKError error) {
                Log.d("get_name", "Error getting name");
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                res.save();
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
