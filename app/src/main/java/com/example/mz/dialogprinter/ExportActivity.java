package com.example.mz.dialogprinter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.POST;

import static com.example.mz.dialogprinter.MainApp.getVkQueue;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

public class ExportActivity extends AppCompatActivity {

    static final public int RESULT_START = 0xFF;
    static final public int RESULT_LOGOUT = RESULT_START + 0;

    static final public int CHOOSE_REQUEST = 0;

    private long chosenId = -1;
    private String chosenName = "name";

    private void updateName() {
        TextView textView = (TextView)findViewById(R.id.userNameView);
        if(textView != null) {
            SharedPreferences pref = MainApp.getPref();
            String name = pref.getString("name", "?");
            if(name.equals("?")) {
                AuthActivity.getName();
                name = pref.getString("name", "?");
            }
            String surname = pref.getString("surname", "?");
            textView.setText(name + " " + surname);
        }
    }
    public void logout(View view) {
        setResult(RESULT_LOGOUT);
        VKSdk.logout();
        finish();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHOOSE_REQUEST) {
            if(resultCode == ChooseActivity.RESULT_CHOSEN) {
                chosenId = data.getLongExtra("user_id", -1);
                chosenName = data.getStringExtra("user_name");
                TextView nameView = (TextView)findViewById(R.id.chosenNameView);
                nameView.setText(chosenName);
            }
        }
    }

    public void chooseDialog(View view) {
        Intent intent = new Intent(this, ChooseActivity.class);
        startActivityForResult(intent, CHOOSE_REQUEST);
    }

    class DialogPusherParams {
        TextView statusView;
        long peerId;
    }
    private class DialogPusher extends AsyncTask<DialogPusherParams, Integer, Boolean> {
        private TextView statusView = null;
        private String userId = null;
        private FileOutputStream outputStream = null;
        private OutputStreamWriter writer = null;

        private String getDialogueFileName() {
            return chosenName + "_" + userId + "_" + Long.toString(chosenId) + ".html";
        }
        private Uri getDialogueUri() {
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(directory, getDialogueFileName());
            return Uri.fromFile(file);
        }

        private boolean openFile() {
            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                return false;
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dialogueFile = new File(directory, getDialogueFileName());
            try {
                outputStream = new FileOutputStream(dialogueFile);
                writer = new OutputStreamWriter(outputStream);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        private boolean writeHeader() {
            if(outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {}
                outputStream = null;
            }

            if(!openFile())
                return false;
            return pushData(getResources().getString(R.string.head_template));
        }

        private boolean writeTrail() {
            publishProgress(400);
            boolean ok = true;
            ok = ok && pushData(getResources().getString(R.string.trail_template));
            if(ok)
                publishProgress(401);
            else
                publishProgress(402);
            try {
                writer.close();
                outputStream.close();
            } catch (IOException e) {
                ok = false;
            } finally {
                outputStream = null;
            }
            if(ok)
                publishProgress(403);
            else
                publishProgress(404);
            return ok;
        }

        private boolean pushData(String data) {
            if(writer == null)
                return false;
            try {
                outputStream.write(data.getBytes());
            } catch (IOException e) {
                return false;
            }
            return true;
        }

        private long offset = 0;
        private long totalCount = -1;
        private String lastError = "";

        private class GetDialoguesRequestListener extends VKRequest.VKRequestListener {
            private Object lock = new Object();
            boolean finished = false;

            JSONObject answer = null;

            void waitFinished() {
                synchronized (lock) {
                    while(!finished) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            finished = true;
                        }
                    }
                }
            }
            private void notifyFinished() {
                synchronized (lock) {
                    finished = true;
                    lock.notifyAll();
                }
            }

            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    answer = response.json.getJSONObject("response");
                } catch (JSONException e) {
                    lastError = e.getMessage();
                } finally {
                    notifyFinished();
                }
            }

            @Override
            public void onError(VKError error) {
                lastError = "onerror " + error.apiError.toString() + " " + error.errorCode + ";";
                notifyFinished();
            }

            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                if(attemptNumber >= totalAttempts) {
                    lastError = "attempt failed";
                    notifyFinished();
                }
            }
        }

        private String getDialogues() {

            VKRequest request = new VKRequest("messages.getHistory",
                    VKParameters.from(
                            VKApiConst.OFFSET, offset,
                            VKApiConst.COUNT, 200,
                            VKApiConst.USER_ID, userId,
                            "peer_id", chosenId,
                            "rev", 1
                    ));

            GetDialoguesRequestListener listener = new GetDialoguesRequestListener();
            getVkQueue().addRequest(request, listener);
            listener.waitFinished();
            JSONObject jsonResponse = listener.answer;
            String answer = null;
            if(jsonResponse != null) {
                try {
                    totalCount = jsonResponse.getLong("count");
                    JSONArray jsonItems = jsonResponse.getJSONArray("items");
                    int itemsCnt = jsonItems.length();
                    offset += itemsCnt;
                    for (int i = 0; i < itemsCnt; ++i) {
                        JSONObject item = jsonItems.getJSONObject(i);
                        if(answer == null)
                            answer = "";
                        answer += MessageExtract.getMessageString(item, false, false);
                    }
                } catch (JSONException e) {
                    lastError = e.getMessage();
                    answer = null;
                }
            }
            publishProgress(301);

            return answer;
        }

        private void showFile() {
            try {
                Uri fileUri = getDialogueUri();
                String mime = "text/html";
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, mime);
                startActivity(intent);
            } catch (IllegalArgumentException e) {
                statusView.setText(statusView.getText() + " " + e.getMessage());
            }
        }
        @Override
        protected Boolean doInBackground(DialogPusherParams... params) {
            statusView = params[0].statusView;
            userId = VKAccessToken.currentToken().userId;
            if(userId == null) {
                return false;
            }
            if(!writeHeader())
                return false;

            publishProgress(1);
            do {
                publishProgress(300);
                String answer = getDialogues();
                boolean ok = (answer != null && !answer.isEmpty());
                publishProgress(ok ? 10 : 11);
                if(!ok)
                    return false;
                publishProgress(310);
                ok = pushData(answer);
                publishProgress(311);
                if(!ok)
                    return false;
                publishProgress((int)offset);
                publishProgress((int)totalCount);
                publishProgress(4);
            } while(offset < totalCount);
            return writeTrail();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            statusView.setText(statusView.getText() + " " + progress[0].toString());
            if(lastError != null && !lastError.isEmpty())
                statusView.setText(statusView.getText() + " " + lastError);
            lastError = "";
        }

        @Override
        protected void onPostExecute(Boolean result) {
            statusView.setText(statusView.getText() + " print finished: " + result.toString());
            showFile();

        }

        @Override
        protected void onCancelled() {
            statusView.setText("Cancelled");
        }
    }
    public void printDialog() {
        final TextView statusView = (TextView)findViewById(R.id.statusView);
        statusView.setText("check if chosen..." + Long.toString(chosenId));
        if(chosenId <= 0) {
            return;
        }
        statusView.setText("chosen " + Long.toString(chosenId));

        DialogPusherParams params = new DialogPusherParams();
        params.statusView = statusView;
        params.peerId = chosenId;
        new DialogPusher().execute(params);


    }


    public void onExportClick(View view) {
        printDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        updateName();
    }
}
