package com.example.mz.dialogprinter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.example.mz.dialogprinter.MainApp.getVkQueue;

public class ChooseActivity extends AppCompatActivity {
    static final public int RESULT_START = 0xFF;
    static final public int RESULT_CHOSEN = RESULT_START + 0;


    class DialogInfo {
        public int     userId;
        public String  name;
        public String  lastMessage;
        public boolean isChat = false;
        public int     chatId;
    }

    static private ArrayList<DialogInfo> dialogInfos = null;
    static private Map<Integer, String> names = MessageExtract.getNames();
    static private Set<Integer> unknownIds = new TreeSet<>();
    private int offset = 0;

    private void addDialogs() {
        if(dialogInfos == null || offset == 0) {
            dialogInfos = new ArrayList<>();
        }

        final int count = 200;
        final int previewLength = 10;
        VKRequest request = VKApi.messages().getDialogs(VKParameters.from(
                VKApiConst.OFFSET, offset,
                VKApiConst.COUNT, count,
                VKApiConst.PREVIEW_LENGTH, previewLength + 1
        ));
        getVkQueue().addRequest(request, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    JSONObject jsonResponse = response.json.getJSONObject("response");
                    long total_count = jsonResponse.getLong("count");
                    JSONArray jsonItems = jsonResponse.getJSONArray("items");
                    int itemsCnt = jsonItems.length();
                    for(int i = 0; i < itemsCnt; ++i) {
                        DialogInfo dialogInfo = new DialogInfo();
                        try {
                            JSONObject item = jsonItems.getJSONObject(i);
                            JSONObject message = item.getJSONObject("message");
                            dialogInfo.userId = message.getInt("user_id");
                            dialogInfo.chatId = message.optInt("chat_id", -1);
                            if(dialogInfo.chatId >= 0) {
                                dialogInfo.name = message.optString("title", "Chat " + Integer.toString(dialogInfo.chatId));
                                dialogInfo.userId = message.optInt("admin_id", dialogInfo.userId);
                                dialogInfo.isChat = true;
                            } else
                                dialogInfo.name = Integer.toString(dialogInfo.userId) + dialogInfo.lastMessage;
                            if(!names.containsKey(dialogInfo.userId)) {
                                unknownIds.add(dialogInfo.userId);
                            }
                            dialogInfo.lastMessage = message.getString("body");
                        } catch (JSONException e) {
                            dialogInfo.userId = 0;
                            dialogInfo.lastMessage = "Error";
                        }
                        if(dialogInfo.lastMessage.length() > previewLength) {
                            dialogInfo.lastMessage = dialogInfo.lastMessage.substring(0, previewLength - 3) + "...";
                        }
                        dialogInfos.add(dialogInfo);
                    }
                    offset += itemsCnt;
                    if(offset < total_count) {
                        addDialogs();
                        return;
                    } else {
                        findNamesForDialogs();
                    }
                } catch (JSONException e) {
                    Log.d("toast", "5");
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onError(VKError error) {
                Log.d("toast", "6");
                Toast.makeText(getApplicationContext(), error.apiError.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void findNamesForDialogs() {
        MessageExtract.requestNames(unknownIds, new MessageExtract.OnFinishedCallback() {
            @Override
            public void finished() {
                for(DialogInfo dialogInfo: dialogInfos) {
                    if(!dialogInfo.isChat && names.containsKey(dialogInfo.userId)) {
                        dialogInfo.name = names.get(dialogInfo.userId);
                    }
                }
                showDialogs();
            }
        });
    }

    private void showDialogs() {
        ArrayList<String> names = new ArrayList<>();
        for(DialogInfo dialogInfo: dialogInfos) {
            names.add(dialogInfo.name);
        }
        ListView dialogsListView = (ListView)findViewById(R.id.dialogsListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        dialogsListView.setAdapter(adapter);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        String[] friends = new String[] {
                "updating..."
        };
        ListView dialogsListView = (ListView)findViewById(R.id.dialogsListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friends);
        dialogsListView.setAdapter(adapter);
        dialogsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                    long id) {
                DialogInfo dialogInfo = dialogInfos.get(position);
                Intent resultIntent = new Intent();
                long userId;
                if(dialogInfo.isChat)
                    userId = 2000000000 + dialogInfo.chatId;
                else
                    userId = dialogInfo.userId;
                resultIntent.putExtra("user_id", userId);
                resultIntent.putExtra("user_name", dialogInfo.name);
                setResult(RESULT_CHOSEN, resultIntent);
                finish();
            }
        });
        offset = 0;
        addDialogs();
    }
}
