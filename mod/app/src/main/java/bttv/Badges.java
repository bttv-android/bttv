package bttv;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tv.twitch.android.models.chat.MessageBadge;
import tv.twitch.chat.ChatMessageInfo;

public class Badges {
    public static final String TAG = "LBTTVBadges";
    public static final String BADGE_VERSION = "bttv-android";

    private static HashMap<String, List<BTTVBadge>> badgeHashMap = new HashMap<>();

    public static void appendToBadges(ChatMessageInfo chatMessageInfo, List<MessageBadge> badges) {
        List<BTTVBadge> ourBadges = badgeHashMap.get(chatMessageInfo.userId + "");
        if (ourBadges == null)
            return;
        int i = 0;
        for (BTTVBadge badge : ourBadges) {
            badges.add(new MessageBadge(badge.userId + "-" + i, BADGE_VERSION));
            i++;
        }
    }

    public static String getUrl(int _channelId, String badgeName, String badgeVersion) {
        if (!badgeVersion.equals(BADGE_VERSION)) {
            return null;
        }
        String[] split = badgeName.split("-");
        String id = split[0];
        int ix = Integer.parseInt(split[1]);
        List<BTTVBadge> badges = badgeHashMap.get(id);
        if (badges == null) {
            Log.w(TAG, "getUrl: arr not found for " + badgeName);
            return null;
        }
        return badges.get(ix).url;
    }

    public static void getBadges() {
        Network.get(Network.BTTV_API_HOST + "/3/cached/badges", new ResponseHandler(BTTVBadgeKind.BTTV));
        Network.get(Network.STV_API_HOST + "/cosmetics?user_identifier=twitch_id", new ResponseHandler(BTTVBadgeKind.STV));
    }

    private static class ResponseHandler implements Callback {
        final BTTVBadgeKind kind;
        ResponseHandler(BTTVBadgeKind kind) {
            this.kind = kind;
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            Log.e(TAG, "onFailure: ", e);
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            try (ResponseBody responseBody = response.body()) {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                String json = responseBody.string();
                if (kind == BTTVBadgeKind.BTTV) {
                    JSONArray array = new JSONArray(json);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        JSONObject badgeObj = item.getJSONObject("badge");

                        String userId = item.getString("id");
                        String description = badgeObj.getString("description");
                        String url = badgeObj.getString("svg");

                        BTTVBadge badge = new BTTVBadge(userId, description, url);
                        appendToUser(badge);
                    }
                } else if (kind == BTTVBadgeKind.STV) {
                    JSONObject obj = new JSONObject(json);
                    JSONArray array = obj.getJSONArray("badges");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        JSONArray urls = item.getJSONArray("urls");
                        String url;
                        String description = item.getString("tooltip");

                        if (urls.length() >= 2) {
                            url = urls.getString(1);
                        } else if (urls.length() >= 1) {
                            url = urls.getString(0);
                        } else {
                            Log.w(TAG, "onResponse: ignoring because missing urls: " + item);
                            continue;
                        }

                        JSONArray usersArr = item.getJSONArray("users");
                        for (int j = 0; j < usersArr.length(); j++) {
                            String userId = usersArr.getString(j);
                            BTTVBadge badge = new BTTVBadge(userId, description, url);
                            appendToUser(badge);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "onResponse: " + e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage(), e);
            }
        }

        private static void appendToUser(BTTVBadge badge) {
            String userId = badge.userId;
            List<BTTVBadge> existingBadges = badgeHashMap.get(userId);
            if (existingBadges == null) {
                existingBadges = new ArrayList<>();
            }
            existingBadges.add(badge);
            badgeHashMap.put(userId, existingBadges);
        }
    }
}

enum BTTVBadgeKind {
    BTTV,
    STV
}

class BTTVBadge {
    String userId;
    String description;
    String url;

    public BTTVBadge(String userId, String description, String url) {
        this.userId = userId;
        this.description = description;
        this.url = url;
    }
}
