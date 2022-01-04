package bttv.settings;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;

import bttv.Res;
import bttv.ResUtil;
import bttv.Data;

public class StreamSettings {
    public static void setupBTTVStreamSettings(View view) {
        setupEnableBTTVEmotes(view);
        setupGifEmotes(view);
        setupEnableFFZEmotes(view);
        setupEnable7TVEmotes(view);
        setupEnableAutoRedeem(view);
        setupEnableDeletedMessages(view);
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

    private static void setupGifEmotes(View view) {
        Context context = view.getContext();

        UserPreferences.Entry.DropDownEntry entry = (UserPreferences.Entry.DropDownEntry) Settings.GifRenderMode.entry;
        UserPreferences.Entry.DropDownValue dropDownValue = entry.get(context);
        ArrayList<Res.strings> values = dropDownValue.get();
        int defaultSelected = values.indexOf(dropDownValue.getSelected());

        int spinnerId = ResUtil.getResourceId(context, Res.ids.bttv_stream_settings_gif_mode_spinner);
        Spinner spinner = view.findViewById(spinnerId);

        int listViewLayoutId = ResUtil.getResourceId(context, Res.layouts.twitch_spinner_dropdown_item);

        class Adapter extends ArrayAdapter<Res.strings> {
            public Adapter(@NonNull Context context, int resource, @NonNull ArrayList<Res.strings> objects) {
                super(context, resource, objects);
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return viewWithCorrectText(position, super.getView(position, convertView, parent));
            }

            @NonNull
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return viewWithCorrectText(position, super.getDropDownView(position, convertView, parent));
            }

            @NonNull
            private View viewWithCorrectText(int position, @NonNull View supersView) {
                Res.strings stringAtPos = values.get(position);
                String string = ResUtil.getLocaleString(context, stringAtPos);

                if (supersView instanceof TextView) {
                    TextView tv = (TextView) supersView;
                    tv.setText(string);
                }
                return supersView;
            }
        }

        spinner.setAdapter(new Adapter(context, listViewLayoutId, values));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                entry.set(context, dropDownValue.newWithSelected(values.get(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.w("LBTTV", "onNothingSelected: ");
            }
        });
        spinner.setSelection(defaultSelected);
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

    private static void setupEnableAutoRedeem(View view) {
        int id = ResUtil.getResourceId(view.getContext(), Res.ids.bttv_stream_settings_enable_auto_redeem_toggle);
        SwitchCompat toggle = view.findViewById(id);
        boolean checked = ResUtil.getBooleanFromSettings(Settings.AutoRedeemChannelPointsEnabled);
        toggle.setChecked(checked);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.AutoRedeemChannelPointsEnabled.entry.set(
                        v.getContext(),
                        new UserPreferences.Entry.BoolValue(toggle.isChecked())
                );
            }
        });
    }

    private static void setupEnableDeletedMessages(View view) {
        int id = ResUtil.getResourceId(view.getContext(), Res.ids.bttv_stream_settings_enable_show_deleted_messages_toggle);
        SwitchCompat toggle = view.findViewById(id);
        boolean checked = ResUtil.getBooleanFromSettings(Settings.ShowDeletedMessagesEnabled);
        toggle.setChecked(checked);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.ShowDeletedMessagesEnabled.entry.set(
                    v.getContext(),
                    new UserPreferences.Entry.BoolValue(toggle.isChecked())
                );
            }
        });
    }


}
