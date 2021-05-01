package bttv;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.util.Log;
import tv.twitch.android.shared.emotes.emotepicker.EmotePickerPresenter.ClickedEmote;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteHeaderUiModel;
import tv.twitch.android.shared.emotes.emotepicker.models.EmotePickerSection;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteUiModel;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteUiSet;
import tv.twitch.android.shared.emotes.models.EmoteMessageInput;
import tv.twitch.android.shared.emotes.models.EmotePickerEmoteModel;

public class EmotePicker {
    final static int GLOBAL_BTTV_STRING = 0x7f13fffb;
    final static int CHANNEL_BTTV_STRING = 0x7f13fffc;
    final static int GLOBAL_FFZ_STRING = 0x7f13fffd;
    final static int CHANNEL_FFZ_STRING = 0x7f13fffe;

    public static void extendList(List<EmoteUiSet> original) {
        int channel = Data.currentBroadcasterId;
        if (channel == -1 || !Data.channelHasEmotes(channel)) {
            Log.w("LBTTVextendList", "will skip extendList");
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

            if (Data.availBTTVEmoteSetMap.containsKey(channel)) {
                EmoteUiSet set = getChannelBTTVUiSet();
                if (!set.getEmotes().isEmpty()) {
                    original.add(set);
                }
            }

            if (Data.availFFZEmoteSetMap.containsKey(channel)) {
                EmoteUiSet set = getChannelFFZUiSet();
                if (!set.getEmotes().isEmpty()) {
                    original.add(set);
                }
            }

            original.add(getGlobalBttvUiSet());
            original.add(getGlobalFFZUiSet());

            original.addAll(twitchGlobalSets);

        } catch (Exception e) {
            Log.e("LBTTVEmotePicker", "adding emotes to picker failed", e);
        }
    }

    private static EmoteUiSet getGlobalBttvUiSet() {
        EmoteHeaderUiModel globalBttvHeader = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(GLOBAL_BTTV_STRING,
                true, EmotePickerSection.ALL, false, 8, null);
        EmoteUiSet globalBttvEmotes = new EmoteUiSet(globalBttvHeader, getGlobalBttvEmotes());
        return globalBttvEmotes;
    }

    private static EmoteUiSet getGlobalFFZUiSet() {
        EmoteHeaderUiModel globalFFZHeader = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(GLOBAL_FFZ_STRING, true,
                EmotePickerSection.ALL, false, 8, null);
        EmoteUiSet globalFFZEmotes = new EmoteUiSet(globalFFZHeader, getGlobalFFZEmotes());
        return globalFFZEmotes;
    }

    private static EmoteUiSet getChannelBTTVUiSet() {
        EmoteHeaderUiModel chBttvHeader = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(CHANNEL_BTTV_STRING, true,
                EmotePickerSection.CHANNEL, false, 8, null);
        EmoteUiSet channelBttvEmotes = new EmoteUiSet(chBttvHeader, getChannelBttvEmotes());
        return channelBttvEmotes;
    }

    private static EmoteUiSet getChannelFFZUiSet() {
        EmoteHeaderUiModel chFFZHeader = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(CHANNEL_FFZ_STRING, true,
                EmotePickerSection.CHANNEL, false, 8, null);
        EmoteUiSet channelFFZEmotes = new EmoteUiSet(chFFZHeader, getChannelFFZEmotes());
        return channelFFZEmotes;
    }

    private static List<EmoteUiModel> getGlobalBttvEmotes() {
        ArrayList<EmoteUiModel> list = new ArrayList<>(Data.globalBTTVEmotes.size());
        for (Emote emote : Data.globalBTTVEmotes.values()) {
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getChannelBttvEmotes() {
        Set<String> set = Data.availBTTVEmoteSetMap.get(Data.currentBroadcasterId);
        ArrayList<EmoteUiModel> list = new ArrayList<>(set.size());

        for (String code : set) {
            Emote emote = Data.getEmote(code);
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getGlobalFFZEmotes() {
        ArrayList<EmoteUiModel> list = new ArrayList<>(Data.globalFFZEmotes.size());
        for (Emote emote : Data.globalFFZEmotes.values()) {
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getChannelFFZEmotes() {
        Set<String> set = Data.availFFZEmoteSetMap.get(Data.currentBroadcasterId);
        ArrayList<EmoteUiModel> list = new ArrayList<>(set.size());

        for (String code : set) {
            Emote emote = Data.getEmote(code);
            list.add(emoteToModel(emote));
        }
        return list;

    }

    private static EmoteUiModel emoteToModel(Emote emote) {
        String id = "BTTV-" + emote.id;
        EmotePickerEmoteModel model = new EmotePickerEmoteModel.Generic(id, emote.code);

        EmoteMessageInput input = new EmoteMessageInput(emote.code, id, false);
        ClickedEmote clickedEmote = new ClickedEmote.Unlocked(model, input, null, null, 12, null);
        return new EmoteUiModel(id, false, false, clickedEmote);
    }

}
