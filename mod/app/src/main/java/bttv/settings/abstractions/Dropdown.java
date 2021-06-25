package bttv.settings.abstractions;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import bttv.Res;
import bttv.ResUtil;
import bttv.settings.UserPreferences;
import tv.twitch.android.core.adapters.TwitchArrayAdapter;
import tv.twitch.android.core.adapters.TwitchArrayAdapterModel;
import tv.twitch.android.shared.ui.menus.dropdown.DropDownMenuModel;

public class Dropdown {
    public static DropDownMenuModel<TwitchArrayAdapterModel> simple(Context ctx,
                                                                    String primaryText,
                                                                    String secondaryText,
                                                                    ArrayList<Res.strings> entries,
                                                                    int selectedIdx,
                                                                    DropDownMenuModel.DropDownMenuItemSelection<TwitchArrayAdapterModel> selectionHandler) {
        return new DropDownMenuModel<>(
                new TwitchArrayAdapter<>(
                        ctx,
                        toAdapterModels(ctx, entries),
                        ResUtil.getResourceId(ctx, "twitch_spinner_dropdown_item", "layout")
                ),
                selectedIdx,
                primaryText,
                secondaryText,
                null,
                null,
                selectionHandler
        );
    }

    public static DropDownMenuModel<TwitchArrayAdapterModel> fromEntry(Context context, UserPreferences.Entry.DropDownEntry entry) {
        UserPreferences.Entry.DropDownValue value = entry.get(context);
        ArrayList<Res.strings> values = value.get();
        int selected = values.indexOf(value.getSelected());

        if (selected < 0) {
            Log.w("LBTTVDropDown", "arraylist does not contain selected! " + value);
            selected = 0;
        }

        return simple(
                context,
                ResUtil.getLocaleString(context, entry.primaryTextResource),
                ResUtil.getLocaleString(context, entry.secondaryTextResource),
                values,
                selected,
                new DropDownMenuModel.DropDownMenuItemSelection<TwitchArrayAdapterModel>() {
                    @Override
                    public void onDropDownItemSelected(DropDownMenuModel<TwitchArrayAdapterModel> dropDownMenuModel, int i) {
                        entry.set(context, value.newWithSelected(values.get(i)));
                    }
                }
        );
    }

    private static ArrayList<TwitchArrayAdapterModel> toAdapterModels(final Context context, ArrayList<Res.strings> entries) {
        ArrayList<TwitchArrayAdapterModel> models = new ArrayList<>(entries.size());
        for (Res.strings string : entries) {
            models.add(new TwitchArrayAdapterModel() {
                @Override
                public String getDisplayString(Context context) {
                    return ResUtil.getLocaleString(context, string);
                }
            });
        }
        return models;
    }
}
