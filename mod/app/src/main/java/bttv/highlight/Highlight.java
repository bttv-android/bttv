package bttv.highlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import bttv.ChommentModelDelegateWrapper;
import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import bttv.settings.UserPreferences;
import tv.twitch.android.shared.chat.ChatMessageDelegate;
import tv.twitch.android.provider.chat.ChatMessageInterface;

public class Highlight {
    private static final String TAG = "LBTTVHighlight";
    private static Set<String> highlightSet = null;

    public static Integer replaceNum(ChatMessageInterface chatMessageInterface, Integer num) {
        if (chatMessageInterface == null) {
            Log.w(TAG, "replaceNum: chatMessageInterface is null", new Exception());
            return num;
        }
        if (chatMessageInterface instanceof ChatMessageDelegate) {
            ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
            return replaceNumLive(delegate, num);
        }
        if (chatMessageInterface instanceof ChommentModelDelegateWrapper) {
            ChommentModelDelegateWrapper delegate = (ChommentModelDelegateWrapper) chatMessageInterface;
            return replaceNumVOD(delegate, num);
        }

        Log.i(TAG, "replaceNum: " + chatMessageInterface + " is not a ChatMessageDelegate");
        return num;
    }

    private static Integer replaceNumLive(ChatMessageDelegate delegate, Integer num) {
        if (delegate.mChatMessage == null) {
            Log.w(TAG, "replaceNum: delegate.mChatMessage is null " + delegate, new Exception());
            return num;
        }
        if (delegate.mChatMessage.messageType == null) {
            Log.w(TAG, "replaceNum: delegate.mChatMessage.messageType is null " + delegate.mChatMessage, new Exception());
            return num;
        }

        if (delegate.mChatMessage.messageType.equals("bttv-highlighted-message")
                || (delegate.getUserName() != null && Highlight.shouldHighlightChannel(delegate.getUserName()))
                || (delegate.getDisplayName() != null && Highlight.shouldHighlightChannel(delegate.getDisplayName()))
        ) {
            num = ResUtil.getResourceId(Res.colors.bttv_sonic);
        }
        return num;
    }

    private static Integer replaceNumVOD(ChommentModelDelegateWrapper delegateWrapper, Integer num) {
        if (!delegateWrapper.BTTVshouldHighlight()) {
            return num;
        }
        return ResUtil.getResourceId(Res.colors.bttv_sonic);
    }

    private static void loadSet() {
        if (highlightSet != null) {
            return;
        }
        highlightSet = ResUtil.getStringSetFromSettings(Settings.HighlightedKeyWords);
    }

    public static void remove(String word) {
        highlightSet.remove(word.toLowerCase());
        Settings.HighlightedKeyWords.entry.set(Data.ctx, new UserPreferences.Entry.StringSetValue(highlightSet));
    }

    public static boolean add(String word) {
        boolean val = highlightSet.add(word.toLowerCase());
        Settings.HighlightedKeyWords.entry.set(Data.ctx, new UserPreferences.Entry.StringSetValue(highlightSet));
        return val;
    }

    public static boolean shouldHighlight(String word) {
        loadSet();
        if (word.startsWith("<") || word.endsWith(">"))
            return false;
        return highlightSet.contains(word.toLowerCase());
    }

    public static boolean shouldHighlightChannel(String name) {
        loadSet();
        return highlightSet.contains("<" + name.toLowerCase() + ">");
    }

    public static boolean isEmpty() {
        loadSet();
        return highlightSet.isEmpty();
    }

    public static void openDialog(Activity activity) {
        loadSet();

        // load layouts
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        View v = activity.getLayoutInflater().inflate(ResUtil.getResourceId(Res.layouts.bttv_highlight_dialog), null);
        b.setView(v);
        b.setCancelable(true);
        ListView list = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_highlight_dia_list));
        TextView emptyTV = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_highlight_dia_list_empty));

        // load data
        String[] asArr = highlightSet.toArray(new String[0]);
        ArrayList<String> asList = new ArrayList<>(Arrays.asList(asArr));

        HighlightAdapter adapter = new HighlightAdapter(
            activity,
            ResUtil.getResourceId(activity, Res.layouts.bttv_highlight_list_view),
            asList
        );
        adapter.afterRemovedListener = new HighlightAdapter.AfterRemoved() {
            @Override
            public void onAfterRemoved() {
                maybeShowEmptyMessage(asList, list, emptyTV);
            }
        };
        list.setAdapter(adapter);

        // close button
        b.setPositiveButton(ResUtil.getStringId("close"), null);

        // on Enter handler
        EditText eT = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_highlight_dia_input));
        eT.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return dialogOnAdd(v, adapter, asList, list, emptyTV);
            }
        });

        // on Button handler
        ImageButton submitBtn = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_highlight_submit_btn));
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogOnAdd(eT, adapter, asList, list, emptyTV);
            }
        });

        maybeShowEmptyMessage(asList, list, emptyTV);

        AlertDialog dialog = b.create();
        dialog.show();
    }

    public static boolean dialogOnAdd(TextView v, HighlightAdapter adapter, ArrayList<String> asList, ListView list, TextView emptyTV) {
        for (String w: v.getText().toString().split(" ")) {
            String word = w.toLowerCase();
            if (word.trim().isEmpty()) {
                continue;
            }
            if (Highlight.add(word)) {
                asList.add(word);
                maybeShowEmptyMessage(asList, list, emptyTV);
            }
        }
        adapter.notifyDataSetChanged();
        v.setText("");
        return true;
    }

    public static void maybeShowEmptyMessage(ArrayList<String> asList, ListView list, TextView emptyTV) {
        boolean showEmpty = asList.isEmpty();
        list.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        emptyTV.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }
}
