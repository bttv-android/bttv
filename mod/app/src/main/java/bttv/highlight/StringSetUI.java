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

import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import bttv.settings.UserPreferences;

/** Parent class for both the Highlight and Blacklist features */
public abstract class StringSetUI {
    static Set<String> stringSet = null;

    private final Settings stringSetSetting;
    private final Res.layouts dialogLayoutRes;

    StringSetUI(Settings stringSetSetting, Res.layouts dialogLayoutRes) {
        this.stringSetSetting = stringSetSetting;
        this.dialogLayoutRes = dialogLayoutRes;
    }

    void loadSet() {
        if (stringSet != null) {
            return;
        }
        stringSet = ResUtil.getStringSetFromSettings(this.stringSetSetting);
    }


    void remove(String word) {
        stringSet.remove(word.toLowerCase());
        stringSetSetting.entry.set(Data.ctx, new UserPreferences.Entry.StringSetValue(stringSet));
    }

    boolean add(String word) {
        boolean val = stringSet.add(word.toLowerCase());
        stringSetSetting.entry.set(Data.ctx, new UserPreferences.Entry.StringSetValue(stringSet));
        return val;
    }


    public boolean isEmpty() {
        loadSet();
        return stringSet.isEmpty();
    }

    void maybeShowEmptyMessage(ArrayList<String> asList, ListView list, TextView emptyTV) {
        boolean showEmpty = asList.isEmpty();
        list.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        emptyTV.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }

    public void openDialog(Activity activity) {
        loadSet();

        // load layouts
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        View v = activity.getLayoutInflater().inflate(ResUtil.getResourceId(dialogLayoutRes), null);
        b.setView(v);
        b.setCancelable(true);
        ListView list = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_highlight_dia_list));
        TextView emptyTV = v.findViewById(ResUtil.getResourceId(activity, Res.ids.bttv_highlight_dia_list_empty));

        // load data
        String[] asArr = stringSet.toArray(new String[0]);
        ArrayList<String> asList = new ArrayList<>(Arrays.asList(asArr));

        StringSetUIAdapter adapter = new StringSetUIAdapter(
                activity,
                ResUtil.getResourceId(activity, Res.layouts.bttv_highlight_list_view),
                asList,
                this
        );
        adapter.afterRemovedListener = new StringSetUIAdapter.AfterRemoved() {
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


    boolean dialogOnAdd(TextView v, StringSetUIAdapter adapter, ArrayList<String> asList, ListView list, TextView emptyTV) {
        for (String w: v.getText().toString().split(" ")) {
            String word = w.toLowerCase();
            if (word.trim().isEmpty()) {
                continue;
            }
            if (add(word)) {
                asList.add(word);
                maybeShowEmptyMessage(asList, list, emptyTV);
            }
        }
        adapter.notifyDataSetChanged();
        v.setText("");
        return true;
    }
}
