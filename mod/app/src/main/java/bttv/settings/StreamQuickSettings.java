package bttv.settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import java.lang.ref.WeakReference;

import bttv.AnonChat;
import bttv.highlight.Blacklist;
import bttv.highlight.Highlight;

/** The native player Options view owns this binding; Twitch's draft state is untouched. */
public final class StreamQuickSettings {
    private static final Settings[] TOGGLES = {
            Settings.SplitChatEnabled, Settings.ShowDeletedMessagesEnabled,
            Settings.AlwaysShowChatTimestamps, Settings.AnonChatEnabled
    };

    private StreamQuickSettings() {
    }

    public static void bind(View root) {
        if (root == null) return;
        int id = root.getResources().getIdentifier(
                "bttv_quick_settings", "id", root.getContext().getPackageName());
        View section = id == 0 ? null : root.findViewById(id);
        if (section == null) return;
        if (section.getTag() instanceof Controller) {
            ((Controller) section.getTag()).refresh();
            return;
        }
        // All widget lookup is scoped to our section, never Twitch's own controls.
        for (int i = 0; i < TOGGLES.length; i++) {
            if (!(section.findViewWithTag("bttv.quick.switch." + i) instanceof SwitchCompat)) {
                section.setVisibility(View.GONE);
                return;
            }
        }
        Controller controller = new Controller(section);
        section.setTag(controller);
        section.addOnAttachStateChangeListener(controller);
        controller.install(section);
        if (section.isAttachedToWindow()) controller.onViewAttachedToWindow(section);
        else controller.refresh();
    }

    private static final class Controller implements View.OnAttachStateChangeListener,
            SharedPreferences.OnSharedPreferenceChangeListener {
        private final WeakReference<View> section;
        private final SharedPreferences preferences;
        private boolean attached;
        private boolean refreshing;

        Controller(View view) {
            section = new WeakReference<>(view);
            preferences = view.getContext().getApplicationContext().getSharedPreferences("BTTV", 0);
        }

        void install(View view) {
            for (int i = 0; i < TOGGLES.length; i++) {
                final Settings setting = TOGGLES[i];
                SwitchCompat toggle = view.findViewWithTag("bttv.quick.switch." + i);
                TextView title = view.findViewWithTag("bttv.quick.title." + i);
                if (title != null) toggle.setContentDescription(title.getText());
                toggle.setOnCheckedChangeListener((button, checked) -> {
                    if (refreshing || !attached) return;
                    setting.entry.set(button.getContext(), new UserPreferences.Entry.BoolValue(checked));
                    if (setting == Settings.AnonChatEnabled) AnonChat.reconnect(checked);
                });
                View row = view.findViewWithTag("bttv.quick.row." + i);
                if (row != null) row.setOnClickListener(ignored -> toggle.toggle());
            }
            action(view, "highlights", () -> {
                Activity activity = activity();
                if (activity != null) Highlight.getInstance().openDialog(activity);
            });
            action(view, "blacklist", () -> {
                Activity activity = activity();
                if (activity != null) Blacklist.getInstance().openDialog(activity);
            });
            action(view, "settings", () -> {
                Activity activity = activity();
                if (activity != null) SettingsNavigation.open(activity);
            });
        }

        private Activity activity() {
            View view = section.get();
            return !attached || view == null ? null : SettingsNavigation.findActivity(view.getContext());
        }

        private void action(View view, String name, Runnable open) {
            View row = view.findViewWithTag("bttv.quick." + name);
            if (row != null) row.setOnClickListener(ignored -> open.run());
        }

        void refresh() {
            View view = section.get();
            if (view == null) return;
            refreshing = true;
            try {
                for (int i = 0; i < TOGGLES.length; i++) {
                    SwitchCompat toggle = view.findViewWithTag("bttv.quick.switch." + i);
                    boolean checked = ((UserPreferences.Entry.BoolEntry) TOGGLES[i].entry)
                            .get(view.getContext()).get();
                    toggle.setChecked(checked);
                }
            } finally {
                refreshing = false;
            }
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            if (!attached) {
                attached = true;
                preferences.registerOnSharedPreferenceChangeListener(this);
            }
            refresh();
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            attached = false;
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences ignored, String key) {
            if (!attached) return;
            for (Settings setting : TOGGLES) {
                if (key == null || setting.entry.key.equals(key)) {
                    refresh();
                    return;
                }
            }
        }
    }
}
