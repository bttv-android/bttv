package bttv.settings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import bttv.AnonChat;
import bttv.Credits;
import bttv.HideStories;
import bttv.Res;
import bttv.ResUtil;
import bttv.highlight.Blacklist;
import bttv.highlight.Highlight;
import bttv.updater.UpdateDialog;

/** Existing BTTV preferences presented with Twitch 31's native View resources. */
public final class SettingsActivity extends Activity {
    private int primaryTextColor;
    private int secondaryTextColor;
    private UpdateDialog updaterDialog;
    private boolean updaterVisible;

    @Override
    protected void attachBaseContext(Context base) {
        // Local to this screen/dialogs: do not change Twitch's global theme.
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | Configuration.UI_MODE_NIGHT_YES;
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updaterVisible = savedInstanceState != null && savedInstanceState.getBoolean("bttv.updater.open", false);
        primaryTextColor = resolveColor("text_base", Color.WHITE);
        secondaryTextColor = resolveColor("text_alt", 0xffadadb8);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarContrastEnforced(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        View content = createContent();
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(content);
        content.requestApplyInsets();
    }

    private View createContent() {
        LinearLayout root = vertical();
        root.setBackgroundColor(resolveColor("background_body", 0xff0e0e10));
        root.addView(createToolbar(), matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = vertical();
        content.setPadding(0, 0, 0, dp(16));

        for (SettingsSections section : SettingsSections.values()) {
            TextView heading = text(localize(section.title), 13, resolveColor("text_link", 0xffbf94ff));
            heading.setTypeface(getResources().getFont(resource("roobert_semibold", "font")));
            heading.setPadding(dp(16), dp(24), dp(16), dp(8));
            heading.setAccessibilityHeading(true);
            content.addView(heading, matchWrap());
            for (Settings setting : Settings.values()) {
                if (SettingsSections.sectionOf(setting) != section) {
                    continue;
                }
                UserPreferences.Entry entry = setting.entry;
                View row;
                if (entry instanceof UserPreferences.Entry.BoolEntry) {
                    row = createSwitch((UserPreferences.Entry.BoolEntry) entry);
                } else if (entry instanceof UserPreferences.Entry.DropDownEntry) {
                    row = createDropDown((UserPreferences.Entry.DropDownEntry) entry);
                } else {
                    row = createLink((UserPreferences.Entry.LinkEntry) entry);
                }
                row.setTag(setting.name());
                content.addView(row, matchWrap());
                View divider = new View(this);
                divider.setBackgroundColor(resolveColor("border_base", 0xff26262c));
                content.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
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
        Toolbar toolbar = new Toolbar(new ContextThemeWrapper(this,
                resource("Theme.Twitch.ToolbarStyle", "style")));
        toolbar.setMinimumHeight(dp(56));
        toolbar.setBackgroundColor(resolveColor("background_body", 0xff0e0e10));
        toolbar.setTitle(localize(Res.strings.bttv_settings_entry_title));
        toolbar.setTitleTextAppearance(this, resource("Theme.Twitch.ToolBarTitle", "style"));
        toolbar.setTitleTextColor(primaryTextColor);
        toolbar.setNavigationIcon(resource("ic_arrow_left", "drawable"));
        toolbar.setNavigationContentDescription(getString(resource("abc_action_bar_up_description", "string")));
        toolbar.setNavigationOnClickListener(ignored -> finish());
        return toolbar;
    }

    private View createSwitch(final UserPreferences.Entry.BoolEntry entry) {
        View row = nativeRow("toggle_menu_recycler_item", entry);
        CompoundButton toggle = row.findViewById(resource("toggle", "id"));
        toggle.setMinHeight(dp(48));
        toggle.setMinWidth(dp(48));
        toggle.setContentDescription(localize(entry.primaryTextResource));
        toggle.setChecked(entry.get(this).get());
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton ignored, boolean checked) {
                entry.set(SettingsActivity.this, new UserPreferences.Entry.BoolValue(checked));
                if (entry == Settings.AnonChatEnabled.entry) {
                    AnonChat.reconnect(checked);
                } else if (entry == Settings.HideTwitchStoriesEnabled.entry) {
                    HideStories.onSettingChanged();
                }
            }
        });
        row.setOnClickListener(ignored -> toggle.toggle());
        return row;
    }

    private View createDropDown(final UserPreferences.Entry.DropDownEntry entry) {
        LinearLayout row = (LinearLayout) nativeRow("drop_down_menu_recycler_item", entry);
        row.findViewById(resource("button", "id")).setVisibility(View.GONE);

        final UserPreferences.Entry.DropDownValue value = entry.get(this);
        final ArrayList<Res.strings> choices = value.get();
        List<String> labels = new ArrayList<>(choices.size());
        for (Res.strings choice : choices) {
            labels.add(localize(choice));
        }
        Spinner spinner = row.findViewById(resource("spinner", "id"));
        spinner.setMinimumHeight(dp(48));
        spinner.setContentDescription(localize(entry.primaryTextResource));
        spinner.setBackgroundTintList(ColorStateList.valueOf(resolveColor("text_link", 0xffbf94ff)));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                resource("twitch_spinner_dropdown_item", "layout"),
                labels
        );
        adapter.setDropDownViewResource(resource("twitch_spinner_dropdown_item", "layout"));
        spinner.setAdapter(adapter);
        int selected = choices.indexOf(value.getSelected());
        spinner.setSelection(selected < 0 ? 0 : selected, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> ignored, View view, int position, long id) {
                if (position >= 0 && position < choices.size()) {
                    entry.set(SettingsActivity.this, value.newWithSelected(choices.get(position)));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> ignored) {
            }
        });
        return row;
    }

    private View createLink(final UserPreferences.Entry.LinkEntry entry) {
        LinearLayout row = (LinearLayout) nativeRow("sub_menu_recycler_item", entry);
        android.widget.ImageView chevron = new android.widget.ImageView(this);
        chevron.setImageResource(resource("ic_arrow_right", "drawable"));
        chevron.setImageTintList(ColorStateList.valueOf(secondaryTextColor));
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(24)));
        row.setOnClickListener(ignored -> open(entry.action));
        return row;
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
                updaterVisible = true;
                showUpdater();
                break;
        }
    }

    private void showUpdater() {
        if (updaterDialog == null) {
            updaterDialog = new UpdateDialog(this, () -> { updaterVisible = false; updaterDialog = null; });
            updaterDialog.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updaterVisible) showUpdater();
    }

    @Override
    protected void onStop() {
        if (updaterDialog != null) { updaterDialog.dispose(); updaterDialog = null; }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("bttv.updater.open", updaterVisible);
        super.onSaveInstanceState(state);
    }

    private View nativeRow(String layout, UserPreferences.Entry entry) {
        View row = getLayoutInflater().inflate(resource(layout, "layout"), null);
        row.setMinimumHeight(dp(56));
        row.setBackgroundColor(resolveColor("background_base", 0xff18181b));
        TextView title = row.findViewById(resource("menu_item_title", "id"));
        title.setText(localize(entry.primaryTextResource));
        title.setTextColor(primaryTextColor);
        TextView description = row.findViewById(resource("menu_item_description", "id"));
        description.setText(localize(entry.secondaryTextResource));
        description.setTextColor(secondaryTextColor);
        description.setVisibility(entry.secondaryTextResource == null ? View.GONE : View.VISIBLE);
        View summary = row.findViewById(resource("section_summary", "id"));
        if (summary != null) {
            summary.setVisibility(View.GONE);
        }
        if (row instanceof LinearLayout) {
            ((LinearLayout) row).setGravity(Gravity.CENTER_VERTICAL);
        }
        return row;
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

    private int resource(String name, String type) {
        return getResources().getIdentifier(name, type, getPackageName());
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

}
