package bttv.emote;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.Log;

import bttv.Data;
import bttv.Util;
import tv.twitch.android.shared.emotes.emotepicker.EmotePickerPresenter.ClickedEmote;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteHeaderUiModel;
import tv.twitch.android.shared.emotes.emotepicker.models.EmotePickerSection;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteUiModel;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteUiSet;
import tv.twitch.android.shared.emotes.models.EmoteMessageInput;
import tv.twitch.android.shared.emotes.models.EmotePickerEmoteModel;

public class EmotePicker {

    @SuppressWarnings("unused")
    public static void extendList(List<EmoteUiSet> original) {
        int channel = Data.currentBroadcasterId;
        Context context = Data.ctx;

        if (channel == -1 || !Emotes.channelHasEmotes(context, channel)) {
            Log.w("LBTTVextendList", "will skip extendList");
            return;
        }
        if (Data.ctx == null) {
            Log.e("LBTTVextendList", "Data.ctx is null, will skip extendList");
            return;
        }

        try {
            List<EmoteUiSet> preGlobalSets = new ArrayList<>(original.size());
            List<EmoteUiSet> twitchGlobalSets = new ArrayList<>(original.size());

            for (EmoteUiSet set : original) {
                EmoteHeaderUiModel header = set.getHeader();

                // ALL is the last section
                if (header.getEmotePickerSection() == EmotePickerSection.ALL) {
                    twitchGlobalSets.add(set);
                } else {
                    preGlobalSets.add(set);
                }
            }

            original.clear();
            original.addAll(preGlobalSets);

            if (Emotes.channelEmotesBTTV.containsKey(channel)) {
                EmoteUiSet set = getUiSet("bttv_emote_picker_channel_bttv", EmotePickerSection.CHANNEL, getChannelBttvEmotes());
                if (!set.getEmotes().isEmpty()) {
                    original.add(set);
                }
            }

            if (Emotes.channelEmotesFFZ.containsKey(channel)) {
                EmoteUiSet set = getUiSet("bttv_emote_picker_channel_ffz", EmotePickerSection.CHANNEL, getChannelFFZEmotes());
                if (!set.getEmotes().isEmpty()) {
                    original.add(set);
                }
            }

            if (Emotes.channelEmotes7TV.containsKey(channel)) {
                EmoteUiSet set = getUiSet("bttv_emote_picker_channel_7tv", EmotePickerSection.CHANNEL, getChannel7TVEmotes());
                if (!set.getEmotes().isEmpty()) {
                    original.add(set);
                }
            }

            original.add(getUiSet("bttv_emote_picker_global_bttv", EmotePickerSection.ALL, getGlobalBttvEmotes()));
            original.add(getUiSet("bttv_emote_picker_global_ffz", EmotePickerSection.ALL, getGlobalFFZEmotes()));
            original.add(getUiSet("bttv_emote_picker_global_7tv", EmotePickerSection.ALL, getGlobal7TVEmotes()));

            original.addAll(twitchGlobalSets);

        } catch (Exception e) {
            Log.e("LBTTVEmotePicker", "adding emotes to picker failed", e);
        }
    }

    private static List<EmoteUiModel> getGlobalBttvEmotes() {
        ArrayList<EmoteUiModel> list = new ArrayList<>(Emotes.globalEmotesBTTV.size());
        for (Emote emote : Emotes.globalEmotesBTTV.values()) {
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getChannelBttvEmotes() {
        Set<String> set = Emotes.channelEmotesBTTV.get(Data.currentBroadcasterId);
        ArrayList<EmoteUiModel> list = new ArrayList<>(set.size());

        for (String code : set) {
            Emote emote = Emotes.getEmote(code);
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getGlobalFFZEmotes() {
        ArrayList<EmoteUiModel> list = new ArrayList<>(Emotes.globalEmotesFFZ.size());
        for (Emote emote : Emotes.globalEmotesFFZ.values()) {
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getChannelFFZEmotes() {
        Set<String> set = Emotes.channelEmotesFFZ.get(Data.currentBroadcasterId);
        ArrayList<EmoteUiModel> list = new ArrayList<>(set.size());

        for (String code : set) {
            Emote emote = Emotes.getEmote(code);
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getGlobal7TVEmotes() {
        ArrayList<EmoteUiModel> list = new ArrayList<>(Emotes.globalEmotes7TV.size());
        for (Emote emote : Emotes.globalEmotes7TV.values()) {
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getChannel7TVEmotes() {
        Set<String> set = Emotes.channelEmotes7TV.get(Data.currentBroadcasterId);
        ArrayList<EmoteUiModel> list = new ArrayList<>(set.size());

        for (String code : set) {
            Emote emote = Emotes.getEmote(code);
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static EmoteUiSet getUiSet(String headerResName, EmotePickerSection section, List<EmoteUiModel> emotes) {
        EmoteHeaderUiModel header = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(
                Util.getResourceId(headerResName, "string"),
                true,
                section,
                false,
                0b1000,
                null
        );
        return new EmoteUiSet(header, emotes);
    }

    private static EmoteUiModel emoteToModel(Emote emote) {
        String id = "BTTV-" + emote.id;
        EmotePickerEmoteModel model = new EmotePickerEmoteModel.Generic(id, emote.code);

        EmoteMessageInput input = new EmoteMessageInput(emote.code, id, false);
        ClickedEmote clickedEmote = new ClickedEmote.Unlocked(model, input, null, null, 12, null);
        return new EmoteUiModel(id, false, false, clickedEmote);
    }

}
