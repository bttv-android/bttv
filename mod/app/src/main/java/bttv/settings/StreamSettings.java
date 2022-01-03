package bttv.settings;

import android.view.View;

import androidx.appcompat.widget.SwitchCompat;

import bttv.Res;
import bttv.ResUtil;
import bttv.Data;

public class StreamSettings {
    public static void setupBTTVStreamSettings(View view) {
        setupEnableBTTVEmotes(view);
        setupEnableFFZEmotes(view);
    }

    private static void setupEnableBTTVEmotes(View view) {
        int id = ResUtil.getResourceId(view.getContext(), Res.ids.bttv_stream_settings_enable_bttv_emotes_toggle);
        SwitchCompat toggle = view.findViewById(id);
        boolean checked = ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled);
        toggle.setChecked(checked);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.BTTVEmotesEnabled.entry.set(
                    v.getContext(),
                    new UserPreferences.Entry.BoolValue(toggle.isChecked())
                );
            }
        });
    }

    private static void setupEnableFFZEmotes(View view) {
        int id = ResUtil.getResourceId(view.getContext(), Res.ids.bttv_stream_settings_enable_ffz_emotes_toggle);
        SwitchCompat toggle = view.findViewById(id);
        boolean checked = ResUtil.getBooleanFromSettings(Settings.FFZEmotesEnabled);
        toggle.setChecked(checked);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.FFZEmotesEnabled.entry.set(
                    v.getContext(),
                    new UserPreferences.Entry.BoolValue(toggle.isChecked())
                );
            }
        });
    }

    private static void setupEnable7TVEmotes(View view) {
        int id = ResUtil.getResourceId(view.getContext(), Res.ids.bttv_stream_settings_enable_7TV_emotes_toggle);
        SwitchCompat toggle = view.findViewById(id);
        boolean checked = ResUtil.getBooleanFromSettings(Settings.SevenTVEmotesEnabled);
        toggle.setChecked(checked);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.SevenTVEmotesEnabled.entry.set(
                    v.getContext(),
                    new UserPreferences.Entry.BoolValue(toggle.isChecked())
                );
            }
        });
    }
}
