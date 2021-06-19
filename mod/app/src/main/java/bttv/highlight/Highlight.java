package bttv.highlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.UserPreferences;
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
            num = ResUtil.getResourceId(Res.colors.bttv_sonic);
        }
        return num;
    }

    private static void loadSet() {
        if (highlightSet != null) {
            return;
        }
        highlightSet = UserPreferences.getStringSet(Data.ctx, "bttv_highlight_set");
    }

    public static void remove(String word) {
        highlightSet.remove(word);
        UserPreferences.setStringSet(Data.ctx, "bttv_highlight_set", highlightSet);
    }

    public static boolean add(String word) {
        boolean val = highlightSet.add(word);
        UserPreferences.setStringSet(Data.ctx, "bttv_highlight_set", highlightSet);
        return val;
    }

    public static boolean shouldHighlight(String word) {
        loadSet();
        return highlightSet.contains(word);
    }

    public static void openDialog(Activity activity) {
        loadSet();
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        View v = activity.getLayoutInflater().inflate(ResUtil.getResourceId(Res.layouts.bttv_highlight_dialog), null);
        b.setView(v);
        b.setCancelable(true);
        ListView list = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_highlight_dia_list));

        String[] asArr = highlightSet.toArray(new String[0]);
        ArrayList<String> asList = new ArrayList<>(Arrays.asList(asArr));

        HighlightAdapter adapter = new HighlightAdapter(
                activity,
                ResUtil.getResourceId(activity, Res.layouts.bttv_highlight_list_view),
                asList
        );
        list.setAdapter(adapter);

        b.setPositiveButton(ResUtil.getStringId("close"), null);

        EditText eT = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_highlight_dia_input));
        eT.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                for (String word: v.getText().toString().split(" ")) {
                    if (word.trim().isEmpty()) {
                        continue;
                    }
                    if (Highlight.add(word)) {
                        asList.add(word);
                    }
                }
                adapter.notifyDataSetChanged();
                v.setText("");
                return true;
            }
        });

        AlertDialog dialog = b.create();
        dialog.show();
    }
}
