package bttv.highlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import bttv.Res;
import bttv.Util;
import tv.twitch.android.shared.chat.ChatMessageDelegate;
import tv.twitch.android.shared.chat.ChatMessageInterface;

public class Highlight {
    private static final String TAG = "LBTTVHighlight";
    private static Set<String> highlightSet = null;

    public static Integer replaceNum(ChatMessageInterface chatMessageInterface, Integer num) {
        if (!(chatMessageInterface instanceof ChatMessageDelegate)) {
            Log.i(TAG, "replaceNum: " + chatMessageInterface + " is not a ChatMessageDelegate");
            return num;
        }
        ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
        if (delegate.mChatMessage.messageType.equals("bttv-highlighted-message")) {
            num = Util.getResourceId(Res.colors.bttv_sonic);
        }
        return num;
    }

    private static void loadSet() {
        if (highlightSet != null) {
            return;
        }
        // TODO load from shared prefs
        highlightSet = new HashSet<>();
        highlightSet.add("hi");
        highlightSet.add("hello");
        highlightSet.add("lamo");
        highlightSet.add("test");
        highlightSet.add("xDDD");
        highlightSet.add("spam");
        highlightSet.add("is");
        highlightSet.add("good");
        highlightSet.add("and");
        highlightSet.add("it");
        highlightSet.add("gets");
        highlightSet.add("not");
        highlightSet.add("better");
        highlightSet.add("every");
        highlightSet.add("word");
    }

    public static void remove(String word) {
        highlightSet.remove(word);
    }

    public static boolean shouldHighlight(String word) {
        loadSet();
        return highlightSet.contains(word);
    }

    public static void openDialog(Activity activity) {
        loadSet();
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        View v = activity.getLayoutInflater().inflate(Util.getResourceId(Res.layouts.bttv_highlight_dialog), null);
        b.setView(v);
        b.setCancelable(true);
        ListView list = v.findViewById(Util.getResourceId(activity, Res.ids.bttv_highlight_dia_list));

        String[] asArr = highlightSet.toArray(new String[0]);

        HighlightAdapter adapter = new HighlightAdapter(
                activity,
                Util.getResourceId(activity, Res.layouts.bttv_highlight_list_view),
                asArr
        );
        list.setAdapter(adapter);

        b.setPositiveButton(Util.getStringId("close"), null);

        AlertDialog dialog = b.create();
        dialog.show();
    }
}
