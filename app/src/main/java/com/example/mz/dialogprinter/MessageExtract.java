package com.example.mz.dialogprinter;

import android.util.Log;
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

import java.io.Console;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import retrofit2.http.POST;

import static com.example.mz.dialogprinter.MainApp.getVkQueue;
import static com.vk.sdk.VKUIHelper.getApplicationContext;

/**
 * Created by mz on 10/31/17.
 */

public class MessageExtract {
    public static Map<Integer, String> getNames() {
        return names;
    }

    private static Map<Integer, String> names = new HashMap<>();

    private static Set<Integer> unknownUserIds = new TreeSet<>();
    private static Set<Integer> unknownGroupIds = new TreeSet<>();
    interface OnFinishedCallback {
        void finished();
    }

    public static void requestNames(Set<Integer> newUnknownIds, final OnFinishedCallback onFinished) {
        for(int id: newUnknownIds) {
            if(!names.containsKey(id)) {
                if(id >= 0)
                    unknownUserIds.add(id);
                else
                    unknownGroupIds.add(-id);
            }
        }
        boolean findUsers = true;
        if(unknownUserIds.isEmpty())
            findUsers = false;
        if(!findUsers && unknownGroupIds.isEmpty()) {
            onFinished.finished();
            return;
        }
        String ids = "";
        int cnt = 0;
        final int maxCnt = 1000;
        Set<Integer> unknownIds = (findUsers ? unknownUserIds : unknownGroupIds);
        for(int id: unknownIds) {
            if(cnt >= maxCnt) {
                break;
            }
            if(!ids.isEmpty()) {
                ids += ",";
            }
            ids += Integer.toString(id);
            ++cnt;
        }
        VKRequest request;
        if(findUsers) {
            request = VKApi.users().get(VKParameters.from(
                    VKApiConst.USER_IDS, ids
            ));
            getVkQueue().addRequest(request, new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                    try {
                        JSONArray jsonUsers = response.json.getJSONArray("response");
                        int usersCnt = jsonUsers.length();
                        for(int i = 0; i < usersCnt; ++i) {
                            JSONObject jsonUser = jsonUsers.getJSONObject(i);
                            int id = jsonUser.getInt("id");
                            String first_name = jsonUser.optString("first_name", "");
                            String last_name = jsonUser.optString("last_name", "");
                            names.put(id, first_name + " " + last_name);
                            unknownUserIds.remove(id);
                        }
                    } catch (JSONException e) {
                        Log.d("toast", "1");
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    requestNames(new TreeSet<Integer>(), onFinished);
                }
                @Override
                public void onError(VKError error) {
                    Log.d("toast", "2");
                    Toast.makeText(getApplicationContext(), error.apiError.toString(), Toast.LENGTH_LONG).show();
                    unknownUserIds.clear();
                    requestNames(new TreeSet<Integer>(), onFinished);
                }
                @Override
                public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {

                }
            });
        } else {
            request = VKApi.groups().getById(VKParameters.from(
                    "group_ids", ids
            ));
            getVkQueue().addRequest(request, new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                    try {
                        JSONArray jsonUsers = response.json.getJSONArray("response");
                        int usersCnt = jsonUsers.length();
                        for(int i = 0; i < usersCnt; ++i) {
                            JSONObject jsonUser = jsonUsers.getJSONObject(i);
                            int id = jsonUser.getInt("id");
                            String name = jsonUser.getString("name");
                            names.put(-id, name);
                            unknownGroupIds.remove(id);
                        }
                    } catch (JSONException e) {
                        Log.d("toast", "3");
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    requestNames(new TreeSet<Integer>(), onFinished);
                }
                @Override
                public void onError(VKError error) {
                    Log.d("toast", "4");
                    Toast.makeText(getApplicationContext(), error.apiError.toString(), Toast.LENGTH_LONG).show();
                    unknownGroupIds.clear();
                    requestNames(new TreeSet<Integer>(), onFinished);
                }
                @Override
                public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {

                }
            });
        }

    }

    private static final Object lock = new Object();
    private static boolean finished = true;
    private static String getNameBlocking(int id) {
        if(id <= 0)
            return "group";
        Set<Integer> ids = new TreeSet<>();
        ids.add(id);
        finished = false;
        requestNames(ids, new OnFinishedCallback() {
            @Override
            public void finished() {
                synchronized (lock) {
                    finished = true;
                    lock.notifyAll();
                }
            }
        });
        synchronized (lock) {
            while(!finished) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    return "error(i)";
                }
            }
        }
        if(names.containsKey(id)) {
            return names.get(id);
        }
        return "error";
    }

    private static class Message {
        long date = 0;
        String from_name = "from";
        String body = "";
        ArrayList<String> attach_photos = new ArrayList<>();
        ArrayList<String> attach_links = new ArrayList<>();
        public static class Video {
            String url = "";
            String title = "";
        }
        ArrayList<Video> attach_videos = new ArrayList<>();
        ArrayList<String> attach_posts = new ArrayList<>();
        public static class Audio {
            String url = "";
            String artist = "";
            String title = "";
        }
        ArrayList<Audio> attach_audios = new ArrayList<>();
        ArrayList<String> attach_stickers = new ArrayList<>();
        public static class Doc {
            String url = "";
            String type = "";
            String title = "";
        }
        ArrayList<Doc> attach_docs = new ArrayList<>();
        ArrayList<String> forwarded_messages = new ArrayList<>();
    }

    public static String getMessageString(JSONObject jsonMessage, boolean is_fwd, boolean is_post) {
        Message message = new Message();
        try {
            if (is_post) {
                int wallId = jsonMessage.getInt("to_id");
                getNameBlocking(wallId);
            }
            else {
                int id = 0;
                if(is_fwd) {
                    id = jsonMessage.getInt("user_id");
                } else {
                    id = jsonMessage.getInt("from_id");
                }
                message.from_name = getNameBlocking(id);
            }
            message.date = jsonMessage.getLong("date");
            if(is_post) {
                message.body = jsonMessage.getString("text");
            } else {
                message.body = jsonMessage.getString("body");
            }
            JSONArray attachments = null;
            try {
                attachments = jsonMessage.getJSONArray("attachments");
            } catch (JSONException e) {
                // no attachments
            }
            if(attachments != null) {
                int attachmentsCnt = attachments.length();
                for(int i = 0; i < attachmentsCnt; ++i) {
                    JSONObject attachment = attachments.getJSONObject(i);
                    String type = attachment.getString("type");
                    if(type.equals("photo")) {
                        message.attach_photos.add(getAttachPhoto(attachment.getJSONObject("photo")));
                    } else if(type.equals("link")) {
                        message.attach_links.add(attachment.getJSONObject("link").getString("url"));
                    } else if(type.equals("video")) {
                        Message.Video video = new Message.Video();
                        JSONObject jsonVideo = attachment.getJSONObject("video");
                        video.url = ("https://vk.com/video?z=video" +
                                jsonVideo.getString("owner_id") + "_" +
                                jsonVideo.getString("id"));
                        video.title = jsonVideo.getString("title");
                        message.attach_videos.add(video);
                    } else if(type.equals("wall")) {
                        JSONObject jsonWall = attachment.getJSONObject("wall");
                        message.attach_posts.add(getMessageString(jsonWall, false, true));
                    } else if(type.equals("audio")) {
                        Message.Audio audio = new Message.Audio();
                        JSONObject jsonAudio = attachment.getJSONObject("audio");
                        audio.url = jsonAudio.getString("url");
                        audio.artist = jsonAudio.getString("artist");
                        audio.title = jsonAudio.getString("title");
                        message.attach_audios.add(audio);
                    } else if(type.equals("sticker")) {
                        message.attach_stickers.add(getAttachSticker(attachment.getJSONObject("sticker")));
                    } else if(type.equals("doc")) {
                        Message.Doc doc = new Message.Doc();
                        JSONObject jsonDoc = attachment.getJSONObject("doc");
                        doc.url = jsonDoc.getString("url");
                        doc.title = jsonDoc.getString("title");
                        doc.type = getDocType(jsonDoc.getInt("type"));
                        message.attach_docs.add(doc);
                    }
                }
            }
            JSONArray fwds = null;
            try {
                fwds = jsonMessage.getJSONArray("fwd_messages");
            } catch (JSONException e) {

            }
            if(fwds != null) {
                int fwds_cnt = fwds.length();
                for(int i = 0; i < fwds_cnt; ++i) {
                    JSONObject fwd = fwds.getJSONObject(i);
                    message.forwarded_messages.add(getMessageString(fwd, true, false));
                }
            }
        } catch (JSONException e) {

        }
        return render(message);
    }

    private static String tryGetS(JSONObject o, String k) {
        try {
            return o.getString(k);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String getAttachPhoto(JSONObject photo) {
        String ret = tryGetS(photo, "photo_2560");
        if(ret == null)
            ret = tryGetS(photo, "photo_1280");
        if(ret == null)
            ret = tryGetS(photo, "photo_807");
        if(ret == null)
            ret = tryGetS(photo, "photo_604");
        if(ret == null)
            ret = tryGetS(photo, "photo_130");
        if(ret == null)
            ret = tryGetS(photo, "photo_75");
        return ret;
    }

    private static String getAttachSticker(JSONObject sticker) {
        String ret = tryGetS(sticker, "photo_352");
        if(ret == null)
            ret = tryGetS(sticker, "photo_256");
        if(ret == null)
            ret = tryGetS(sticker, "photo_128");
        if(ret == null)
            ret = tryGetS(sticker, "photo_64");
        return ret;
    }

    private static String getDocType(int t) {
        if(t == 1)
            return "Текстовый документ";
        if(t == 2)
            return "Архив";
        if(t == 3)
            return "GIF";
        if(t == 4)
            return "Изображение";
        if(t == 5)
            return "Аудио";
        if(t == 6)
            return "Видео";
        if(t == 7)
            return "Книга";
        if(t == 8)
            return "Документ";
        return "unknown";
    }

    private static String formatDate(long utime) {
        Date date = new Date(utime*1000);
        SimpleDateFormat format = new SimpleDateFormat("d.MM.yyyy", Locale.getDefault());
        return format.format(date);
    }
    private static String render(Message message) {
        String html = "";
        html += "<div class='message'>";
        html +=  "<div class='msg_top'>";

        html +=   "<div class='msg_left'>";
        html +=    "<span class='name'>" + message.from_name + ":</span><br>";
        html +=    "<span class='date'>" + formatDate(message.date) + "</span>";
        html +=   "</div>";

        html +=   "<div class='msg_right'>";
        html +=    "<div class='message_body'>";
        if(!message.body.isEmpty())
            html +=  message.body;
        for(String sticker: message.attach_stickers)
            html += "<img class='attach_sticker' src='" + sticker + "'>";
        if(!message.attach_links.isEmpty()) {
            html += "<div class='attach_desc'>Прикреплённые ссылки:</div>";
            for (String link : message.attach_links)
                html += "<div class='attach_link'><a href=" + link + ">" + link + "</a></div>";
        }
        if(!message.attach_photos.isEmpty()) {
            html += "<div class='attach_desc'>Прикреплённые фотографии:</div>";
            for(String photo: message.attach_photos)
                html += "<div class='attach_photos'><img class='attach_photo' src=" + photo + "></div>";
        }
        html +=    "</div>";
        html +=   "</div>";

        html +=  "</div>";
        html += "</div>";
        return html;
    }
}
