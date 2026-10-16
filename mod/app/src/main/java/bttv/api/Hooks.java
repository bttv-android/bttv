package bttv.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import bttv.Data;
import bttv.IrcEmotes;
import bttv.Network;
import bttv.RNNetwork;
import bttv.emote.EmoteUrlUtil;
import bttv.emote.InputEmotes;
import bttv.emote.RNEmoteCard;
import bttv.emote.RNEmoteMenu;
import bttv.emote.WideEmotes;

/**
 * Entry points for Twitch builds with the react native chat.
 * These only take plain types, as the names of most classes are obfuscated and change with every release.
 */
public class Hooks {
    public static final String TAG = "LBTTVHooks";

    private static final Set<Integer> bypassedRequests = new HashSet<>();

    /** @noinspection unused */
    public static void onAppCreate(Context context) {
        try {
            Data.ctx = context;
            Log.i(TAG, "onAppCreate: context set");
            // the emote menu is filled before the first chat message arrives
            Network.getBTTVGlobalEmotes();
            Network.getFFZGlobalEmotes();
            Network.get7TVGlobalEmotes();
        } catch (Throwable t) {
            Log.e(TAG, "onAppCreate: ", t);
        }
    }

    /**
     * @param socketId id of the react native websocket
     * @param text incoming text frame
     * @return text to hand to react native
     * @noinspection unused
     */
    public static String onWebSocketText(int socketId, String text) {
        try {
            if (text == null) {
                return null;
            }
            if (text.contains(IrcEmotes.ROOMSTATE)) {
                IrcEmotes.onRoomState(text);
            }
            if (!text.contains(" PRIVMSG #")) {
                return text;
            }
            return IrcEmotes.rewriteFrame(text);
        } catch (Throwable t) {
            Log.e(TAG, "onWebSocketText: ", t);
            return text;
        }
    }

    /**
     * @param uri uri of an image react native is about to load
     * @return uri to load instead
     * @noinspection unused
     */
    public static String imageUri(String uri) {
        try {
            String id = EmoteUrlUtil.idFromTwitchUrl(uri);
            if (id == null) {
                return uri;
            }
            String url = EmoteUrlUtil.getEmoteUrl(id);
            return url == null ? uri : url;
        } catch (Throwable t) {
            Log.e(TAG, "imageUri: ", t);
            return uri;
        }
    }

    /**
     * @param uri url the chat input built for an emote
     * @return url to load instead
     * @noinspection unused
     */
    public static String inputEmoteUrl(String uri) {
        try {
            return uri == null ? null : InputEmotes.imageUrl(uri);
        } catch (Throwable t) {
            Log.e(TAG, "inputEmoteUrl: ", t);
            return uri;
        }
    }

    /**
     * @param url image url of an emote in the chat input
     * @param size height of the emote
     * @return width of the emote
     * @noinspection unused
     */
    public static int inputEmoteWidth(String url, int size) {
        try {
            return InputEmotes.width(url, size);
        } catch (Throwable t) {
            Log.e(TAG, "inputEmoteWidth: ", t);
            return size;
        }
    }

    /**
     * @param url image url of an emote in the chat input
     * @param left left edge of the emote
     * @param size height of the emote
     * @return right edge of the emote
     * @noinspection unused
     */
    public static float inputEmoteRight(String url, float left, float size) {
        try {
            return InputEmotes.right(url, left, size);
        } catch (Throwable t) {
            Log.e(TAG, "inputEmoteRight: ", t);
            return left + size;
        }
    }

    /**
     * @param url image url of an emote in the chat input, called on a background thread
     * @return the image, null to let the input download it
     * @noinspection unused
     */
    public static Bitmap inputEmoteBitmap(String url) {
        try {
            return InputEmotes.bitmap(url);
        } catch (Throwable t) {
            Log.e(TAG, "inputEmoteBitmap: ", t);
            return null;
        }
    }

    /**
     * @param view TwitchEmoteInputView
     * @noinspection unused
     */
    public static void onInputViewAttached(Object view) {
        try {
            InputEmotes.onInputViewAttached(view);
        } catch (Throwable t) {
            Log.e(TAG, "onInputViewAttached: ", t);
        }
    }

    /**
     * Same arguments as the drawBitmap call of an emote span in the chat input.
     * @return true if the emote was drawn, the still image must not be drawn then
     * @noinspection unused
     */
    public static boolean drawInputEmote(Bitmap bitmap, Canvas canvas, RectF destination, Paint paint) {
        try {
            return InputEmotes.draw(bitmap, canvas, destination, paint);
        } catch (Throwable t) {
            Log.e(TAG, "drawInputEmote: ", t);
            return false;
        }
    }

    /**
     * Same arguments as NetworkingModule.sendRequest
     * @param module react native NetworkingModule
     * @param data react native ReadableMap, the body is stored under "string"
     * @return true if the request was answered by us and must not be sent
     * @noinspection unused
     */
    public static boolean interceptHttpRequest(final Object module, final String method, final String url, final double requestId,
                                               final Object headers, final Object data, final String responseType,
                                               final boolean incrementalUpdates, final double timeout, final boolean withCredentials) {
        try {
            if (url == null || data == null || !url.contains("gql.twitch.tv")) {
                return false;
            }
            final int id = (int) requestId;
            synchronized (bypassedRequests) {
                if (bypassedRequests.remove(id)) {
                    return false;
                }
            }
            String body = RNNetwork.readBody(data);
            if (RNEmoteCard.intercept(module, url, id, body)) {
                return true;
            }
            Runnable fallback = new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (bypassedRequests) {
                            bypassedRequests.add(id);
                        }
                        Class<?> readableArray = Class.forName("com.facebook.react.bridge.ReadableArray");
                        Class<?> readableMap = Class.forName("com.facebook.react.bridge.ReadableMap");
                        module.getClass().getMethod("sendRequest", String.class, String.class, double.class, readableArray,
                                        readableMap, String.class, boolean.class, double.class, boolean.class)
                                .invoke(module, method, url, requestId, headers, data, responseType, incrementalUpdates, timeout, withCredentials);
                    } catch (Throwable t) {
                        Log.e(TAG, "interceptHttpRequest: fallback failed: ", t);
                    }
                }
            };
            return RNEmoteMenu.intercept(module, method, url, id, headers, body, fallback);
        } catch (Throwable t) {
            Log.e(TAG, "interceptHttpRequest: ", t);
            return false;
        }
    }

    /**
     * @param text a piece of text react native is about to lay out
     * @return text to show
     * @noinspection unused
     */
    public static String text(String text) {
        try {
            return RNEmoteCard.swapText(text);
        } catch (Throwable t) {
            Log.e(TAG, "text: ", t);
            return text;
        }
    }

    /**
     * @param text text an inline view is part of
     * @param end index behind the inline view
     * @param width width of the slot for the inline view
     * @return width to reserve
     * @noinspection unused
     */
    public static int inlineViewWidth(CharSequence text, int end, int width) {
        try {
            return WideEmotes.inlineViewWidth(text, end, width);
        } catch (Throwable t) {
            Log.e(TAG, "inlineViewWidth: ", t);
            return width;
        }
    }

    /**
     * @param view react native image view
     * @param sources ReadableArray, the sources the view is about to show
     * @noinspection unused
     */
    public static void onImageViewSource(Object view, Object sources) {
        try {
            WideEmotes.onImageViewSource(view, sources);
        } catch (Throwable t) {
            Log.e(TAG, "onImageViewSource: ", t);
        }
    }

    /**
     * @param manager react native SurfaceMountingManager
     * @param tag tag of the view that gets its layout
     * @return width the view should get
     * @noinspection unused
     */
    public static int layoutWidth(Object manager, int tag, int width, int height) {
        try {
            return WideEmotes.layoutWidth(manager, tag, width, height);
        } catch (Throwable t) {
            Log.e(TAG, "layoutWidth: ", t);
            return width;
        }
    }
}
