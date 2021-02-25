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
        if (Data.currentBroadcasterId == -1 || !Data.channelHasEmotes(Data.currentBroadcasterId)) {
            Log.w("LBTTVextendList", "will skip extendList");
            return;
        }

        try {

            // BTTV Global
            EmoteHeaderUiModel globalBttvHeader = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(GLOBAL_BTTV_STRING,
                    true, EmotePickerSection.ALL, false, 8, null);
            EmoteUiSet globalBttvEmotes = new EmoteUiSet(globalBttvHeader, getGlobalBttvEmotes());
            original.add(globalBttvEmotes);

            // FFZ Global
            EmoteHeaderUiModel globalFFZHeader = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(GLOBAL_FFZ_STRING,
                    true, EmotePickerSection.ALL, false, 8, null);
            EmoteUiSet globalFFZEmotes = new EmoteUiSet(globalFFZHeader, getGlobalFFZEmotes());
            original.add(globalFFZEmotes);

            // BTTV Channel
            if (Data.availBTTVEmoteSetMap.containsKey(Data.currentBroadcasterId)) {
                EmoteHeaderUiModel chBttvHeader = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(
                        CHANNEL_BTTV_STRING, true, EmotePickerSection.ALL, false, 8, null);
                EmoteUiSet channelBttvEmotes = new EmoteUiSet(chBttvHeader, getChannelBttvEmotes());
                original.add(channelBttvEmotes);
            }

            // FFZ Channel
            if (Data.availFFZEmoteSetMap.containsKey(Data.currentBroadcasterId)) {
                EmoteHeaderUiModel chFFZHeader = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(CHANNEL_FFZ_STRING,
                        true, EmotePickerSection.ALL, false, 8, null);
                EmoteUiSet channelFFZEmotes = new EmoteUiSet(chFFZHeader, getChannelFFZEmotes());
                original.add(channelFFZEmotes);
            }

        } catch (Exception e) {
            Log.e("LBTTVEmotePicker", "adding emotes to picker failed", e);
        }
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
