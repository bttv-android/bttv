package bttv;

import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tv.twitch.android.models.chat.MessageBadge;
import tv.twitch.chat.ChatMessageInfo;

public class Badges {
    public static final String TAG = "LBTTVBadges";
    public static final String BADGE_VERSION = "bttv-android";

    // userToBadgesMap maps all known users to their badges for all providers
    private static final ConcurrentHashMap<String, List<BTTVBadge>> userToBadgesMap = new ConcurrentHashMap<>();

    /*
    * Appends our badges to the list of badges if the user has any
    */
    public static void appendToBadges(ChatMessageInfo chatMessageInfo, List<MessageBadge> originalBadges) {
        String userId = String.valueOf(chatMessageInfo.userId);

        List<BTTVBadge> ourBadges = userToBadgesMap.get(userId);

        if (ourBadges == null) {
            Log.d(TAG, "appendToBadges(" + userId + "): ourBadges: null");
            return;
        }

        Log.d(TAG, "appendToBadges(" + userId + "): ourBadges: " + Arrays.toString(ourBadges.toArray()));

        // FFZ supports overwriting badges, so we might have to remove badges from the list later
        ArrayList<String> keysToRemove = new ArrayList<>(1);

        // Add new MessageBadges to list
        for (int i = 0; i < ourBadges.size(); i++) {
            BTTVBadge badge = ourBadges.get(i);
            originalBadges.add(new MessageBadge(userId + "-" + i, BADGE_VERSION));
            if (badge.removes != null) {
                keysToRemove.add(badge.removes);
            }
        }

        // Remove overwritten badges
        for (String key : keysToRemove) {
            MessageBadge target = null;
            for (MessageBadge badge : originalBadges) {
                if (badge.component1().equalsIgnoreCase(key)) {
                    target = badge;
                    break;
                }
            }
            if (target != null) {
                originalBadges.remove(target);
            }
        }
    }

    public static String getUrl(int _channelId, String badgeName, String badgeVersion) {
        Log.d(TAG, "getUrl(" + badgeName + "): badgeVersion: " + badgeVersion);
        if (!badgeVersion.equals(BADGE_VERSION)) {
            return null;
        }
        String[] split = badgeName.split("-");
        String userId = split[0];
        int ix = Integer.parseInt(split[1]);
        List<BTTVBadge> badges = userToBadgesMap.get(userId);
        if (badges == null) {
            Log.w(TAG, "getUrl: arr not found for " + badgeName);
            return null;
        }
        BTTVBadge badge = badges.get(ix);
        if (badge.bg != null) {
            Log.d(TAG, "getUrl: backgrounds.put(" + badge.url + ", " + badge.bg +")");
            Data.backgrounds.put(badge.url, badge.bg);
        }
        return badge.url;
    }

    public static void getBadges() {
        Network.get(Network.BTTV_API_HOST + "/3/cached/badges", new ResponseHandler(BTTVBadgeKind.BTTV));
        Network.get(Network.FFZ_API_HOST + "/badges/ids", new ResponseHandler(BTTVBadgeKind.FFZ));
        Network.get(Network.STV_API_HOST + "/cosmetics?user_identifier=twitch_id", new ResponseHandler(BTTVBadgeKind.STV));
        Network.get(Network.CHATTERINO_API_HOST + "/badges", new ResponseHandler(BTTVBadgeKind.Chatterino));
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

                        String userId = item.getString("providerId");
                        String description = badgeObj.getString("description");
                        String url = badgeObj.getString("svg");

                        BTTVBadge badge = new BTTVBadge(description, url);
                        appendBadgeToUser(userId, badge);
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
                            url = urls.getJSONArray(1).getString(1);
                        } else if (urls.length() >= 1) {
                            url = urls.getJSONArray(0).getString(1);
                        } else {
                            Log.w(TAG, "onResponse: ignoring because missing urls: " + item);
                            continue;
                        }

                        JSONArray usersArr = item.getJSONArray("users");
                        for (int j = 0; j < usersArr.length(); j++) {
                            String userId = usersArr.getString(j);
                            BTTVBadge badge = new BTTVBadge(description, url);
                            appendBadgeToUser(userId, badge);
                        }
                    }
                } else if (kind == BTTVBadgeKind.Chatterino) {
                    JSONObject obj = new JSONObject(json);
                    JSONArray array = obj.getJSONArray("badges");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        String url;
                        String description = item.getString("tooltip");

                        if (item.has("image2")) {
                            url = item.getString("image2");
                        } else if (item.has("image1")) {
                            url = item.getString("image1");
                        } else {
                            Log.w(TAG, "onResponse: ignoring because missing urls: " + item);
                            continue;
                        }

                        JSONArray usersArr = item.getJSONArray("users");
                        for (int j = 0; j < usersArr.length(); j++) {
                            String userId = usersArr.getString(j);
                            BTTVBadge badge = new BTTVBadge(description, url);
                            appendBadgeToUser(userId, badge);
                        }
                    }

                } else if (kind == BTTVBadgeKind.FFZ) {
                    JSONObject obj = new JSONObject(json);
                    JSONArray badgeArray = obj.getJSONArray("badges");
                    JSONObject usersObj = obj.getJSONObject("users");

                    for (int i = 0; i < badgeArray.length(); i++) {
                        JSONObject badgeObj = badgeArray.getJSONObject(i);
                        String id = badgeObj.getString("id");

                        String description = badgeObj.getString("title");
                        String url = badgeObj.getJSONObject("urls").getString("2");
                        String replaces = badgeObj.getString("replaces");
                        String bg = badgeObj.getString("color");
                        Integer color = Color.parseColor(bg);

                        JSONArray usersArr = usersObj.getJSONArray(id);


                        for (int j = 0; j < usersArr.length(); j++) {
                            String userId = usersArr.getString(j);
                            BTTVBadge badge = new BTTVBadge(description, url, replaces, color);
                            appendBadgeToUser(userId, badge);
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

        private static void appendBadgeToUser(String userId, BTTVBadge badge) {
            List<BTTVBadge> existingBadges = userToBadgesMap.get(userId);
            if (existingBadges == null) {
                existingBadges = new ArrayList<>();
            }
            existingBadges.add(badge);
            userToBadgesMap.put(userId, existingBadges);
        }
    }
}

enum BTTVBadgeKind {
    BTTV,
    FFZ,
    STV,
    Chatterino,
}

class BTTVBadge {
    String description;
    String url;
    String removes;
    Integer bg;

    public BTTVBadge(String description, String url) {
        this(description, url, null, null);
    }

    public BTTVBadge(String description, String url, String removes, Integer bg) {
        this.description = description;
        this.url = url;
        this.removes = removes;
        this.bg = bg;
    }
}
