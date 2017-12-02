package com.example.mz.dialogprinter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vk.sdk.VKSdk;

public class ExportActivity extends AppCompatActivity {
    static final public int RESULT_START = 0xFF;
    static final public int RESULT_LOGOUT = RESULT_START + 0;

    static final public int CHOOSE_REQUEST = 0;

    private static ExportActivity current = null;
    private static final Object currentSync = new Object();
    public static ExportActivity getCurrent() {
        synchronized (currentSync) {
            return current;
        }
    }

    public void showProfile(){
        String username = AuthActivity.getUsername();
        if(username == null) {
            username = getResources().getString(R.string.username_placeholder);
            AuthActivity.getName();
        }
        TextView usernameText = (TextView)findViewById(R.id.username_text);
        usernameText.setText(username);
        Bitmap userpic = AuthActivity.getUserpic();
        if(userpic != null) {
            ImageView userpicView = (ImageView)findViewById(R.id.userpic_view);
            userpicView.setImageBitmap(userpic);
        }
    }

    private long chosenId = -1;
    private String chosenName = null;
    private TextView statusTextView;

    public void chooseDialogue(View view) {
        Intent intent = new Intent(this, ChooseActivity.class);
        startActivityForResult(intent, CHOOSE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHOOSE_REQUEST) {
            if(resultCode == ChooseActivity.RESULT_CHOSEN) {
                chosenId = data.getLongExtra("user_id", -1);
                chosenName = data.getStringExtra("user_name");
                TextView chosenDialogueText = (TextView)findViewById(R.id.chosen_dialogue_text);
                chosenDialogueText.setText(String.format(getResources().getString(R.string.chosen_dialogue_text_format), chosenName));
                statusTextView.setText(R.string.status_text_start);
            }
        }
    }

    public void exportDialogue(View view) {
        if(chosenId <= 0) {
            statusTextView.setText(R.string.status_text_choose);
            return;
        }
        statusTextView.setText(R.string.status_text_exporting);
        DialogPusherParams params = new DialogPusherParams();
        params.peerId = chosenId;
        params.chosenName = chosenName;
        params.callingActivity = this;
        params.statusView = statusTextView;
        params.progressBar = (ProgressBar)findViewById(R.id.export_progress);
        new DialogPusher().execute(params);
    }

    public void logout(View view) {
        setResult(RESULT_LOGOUT);
        VKSdk.logout();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        synchronized (currentSync) {
            current = this;
        }
        setContentView(R.layout.activity_export);
        statusTextView = (TextView)findViewById(R.id.status_text);
        statusTextView.setText(R.string.status_text_choose);
        showProfile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        synchronized (currentSync) {
            current = null;
        }
    }
}
