package bttv.settings;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import bttv.Credits;
import bttv.Res;
import bttv.ResUtil;
import bttv.highlight.Blacklist;
import bttv.highlight.Highlight;

/** A settings surface independent of Twitch's version-specific presenter graph. */
public final class SettingsActivity extends Activity {
    private int primaryTextColor;
    private int secondaryTextColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        primaryTextColor = resolveColor("text_base", Color.WHITE);
        secondaryTextColor = resolveColor("text_alt", 0xffadadb8);
        getWindow().setStatusBarColor(resolveColor("background_base", Color.BLACK));
        setContentView(createContent());
    }

    private View createContent() {
        LinearLayout root = vertical();
        root.setBackgroundColor(resolveColor("background_base", Color.BLACK));
        root.addView(createToolbar(), matchWrap());

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = vertical();
        int gutter = dp(20);
        content.setPadding(gutter, dp(8), gutter, dp(32));

        TextView explanation = text(
                "Settings are saved now. Feature behavior is enabled as each Twitch 31 port stage is completed.",
                14,
                secondaryTextColor
        );
        explanation.setPadding(0, dp(8), 0, dp(16));
        content.addView(explanation, matchWrap());

        for (Settings setting : Settings.values()) {
            if (!isVisible(setting)) {
                continue;
            }
            UserPreferences.Entry entry = setting.entry;
            if (entry instanceof UserPreferences.Entry.BoolEntry) {
                content.addView(createSwitch((UserPreferences.Entry.BoolEntry) entry), matchWrap());
            } else if (entry instanceof UserPreferences.Entry.DropDownEntry) {
                content.addView(createDropDown((UserPreferences.Entry.DropDownEntry) entry), matchWrap());
            } else if (entry instanceof UserPreferences.Entry.LinkEntry) {
                content.addView(createLink((UserPreferences.Entry.LinkEntry) entry), matchWrap());
            }
        }

        scroll.addView(content, matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        ));
        return root;
    }

    private View createToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(8), dp(16), dp(8));
        toolbar.setBackgroundColor(resolveColor("background_body", resolveColor("background_base", Color.BLACK)));

        Button back = new Button(this);
        back.setText("\u2039");
        back.setTextSize(28);
        back.setTextColor(primaryTextColor);
        back.setContentDescription("Back");
        back.setMinWidth(dp(48));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View ignored) {
                finish();
            }
        });
        toolbar.addView(back, wrapWrap());

        TextView title = text("BTTV", 20, primaryTextColor);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(12), 0, 0, 0);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1.0f));
        return toolbar;
    }

    private View createSwitch(final UserPreferences.Entry.BoolEntry entry) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(10), 0, dp(10));

        Switch toggle = new Switch(this);
        toggle.setText(localize(entry.primaryTextResource));
        toggle.setTextSize(16);
        toggle.setTextColor(primaryTextColor);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        toggle.setChecked(entry.get(this).get());
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ignored, boolean checked) {
                entry.set(SettingsActivity.this, new UserPreferences.Entry.BoolValue(checked));
            }
        });
        row.addView(toggle, matchWrap());
        addDescription(row, entry.secondaryTextResource);
        return row;
    }

    private View createDropDown(final UserPreferences.Entry.DropDownEntry entry) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(10), 0, dp(10));
        row.addView(text(localize(entry.primaryTextResource), 16, primaryTextColor), matchWrap());
        addDescription(row, entry.secondaryTextResource);

        final UserPreferences.Entry.DropDownValue value = entry.get(this);
        final ArrayList<Res.strings> choices = value.get();
        List<String> labels = new ArrayList<>(choices.size());
        for (Res.strings choice : choices) {
            labels.add(localize(choice));
        }
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int selected = choices.indexOf(value.getSelected());
        spinner.setSelection(selected < 0 ? 0 : selected, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> ignored, View view, int position, long id) {
                entry.set(SettingsActivity.this, value.newWithSelected(choices.get(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> ignored) {
            }
        });
        row.addView(spinner, matchWrap());
        return row;
    }

    private View createLink(final UserPreferences.Entry.LinkEntry entry) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setText(localize(entry.primaryTextResource));
        button.setTextSize(16);
        button.setTextColor(primaryTextColor);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View ignored) {
                open(entry.action);
            }
        });
        return button;
    }

    private void open(SettingsAction action) {
        switch (action) {
            case HIGHLIGHTS:
                Highlight.getInstance().openDialog(this);
                break;
            case BLACKLIST:
                Blacklist.getInstance().openDialog(this);
                break;
            case CREDITS:
                Credits.openDialog(this);
                break;
            case UPDATE_CHECK:
                // Updater behavior is intentionally deferred to a later port stage.
                break;
        }
    }

    private boolean isVisible(Settings setting) {
        if (setting.entry.primaryTextResource == null) {
            return false;
        }
        return setting != Settings.AutoUpdateChecksEnabled
                && setting != Settings.InvokeUpdateCheck
                && setting != Settings.DevToolsEnabled;
    }

    private void addDescription(LinearLayout row, Res.strings description) {
        if (description == null) {
            return;
        }
        TextView view = text(localize(description), 13, secondaryTextColor);
        view.setPadding(0, dp(4), 0, 0);
        row.addView(view, matchWrap());
    }

    private String localize(Res.strings value) {
        return ResUtil.getLocaleString(this, value);
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int resolveColor(String name, int fallback) {
        int id = getResources().getIdentifier(name, "color", getPackageName());
        return id == 0 ? fallback : getResources().getColor(id);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }
}
