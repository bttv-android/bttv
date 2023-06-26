package bttv.emote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.util.Log;

import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import tv.twitch.android.models.emotes.EmoteModelType;
import tv.twitch.android.shared.emotes.emotepicker.models.ClickedEmote;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteHeaderUiModel;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteImageDescriptor;
import tv.twitch.android.shared.emotes.emotepicker.models.EmotePickerSection;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteUiModel;
import tv.twitch.android.shared.emotes.emotepicker.models.EmoteUiSet;
import tv.twitch.android.shared.emotes.models.EmoteMessageInput;
import tv.twitch.android.models.emotes.EmoteModel;

public class EmotePicker {

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
            EmoteUiSet set = getUiSet(Res.strings.bttv_emote_picker_channel_bttv, EmotePickerSection.CHANNEL, getChannelBttvEmotes());
            if (!set.getEmotes().isEmpty()) {
                original.add(set);
            }
        }

        if (Emotes.channelEmotesFFZ.containsKey(channel)) {
            EmoteUiSet set = getUiSet(Res.strings.bttv_emote_picker_channel_ffz, EmotePickerSection.CHANNEL, getChannelFFZEmotes());
            if (!set.getEmotes().isEmpty()) {
                original.add(set);
            }
        }

        if (Emotes.channelEmotes7TV.containsKey(channel)) {
            EmoteUiSet set = getUiSet(Res.strings.bttv_emote_picker_channel_7tv, EmotePickerSection.CHANNEL, getChannel7TVEmotes());
            if (!set.getEmotes().isEmpty()) {
                original.add(set);
            }
        }

        original.add(getUiSet(Res.strings.bttv_emote_picker_global_bttv, EmotePickerSection.ALL, getGlobalBttvEmotes()));
        original.add(getUiSet(Res.strings.bttv_emote_picker_global_ffz, EmotePickerSection.ALL, getGlobalFFZEmotes()));
        original.add(getUiSet(Res.strings.bttv_emote_picker_global_7tv, EmotePickerSection.ALL, getGlobal7TVEmotes()));

        original.addAll(twitchGlobalSets);
    }

    private static List<EmoteUiModel> getGlobalBttvEmotes() {
        return getGlobalEmotes(Emotes.globalEmotesBTTV);
    }

    private static List<EmoteUiModel> getChannelBttvEmotes() {
        return getChannelEmotes(Emotes.channelEmotesBTTV);
    }

    private static List<EmoteUiModel> getGlobalFFZEmotes() {
        return getGlobalEmotes(Emotes.globalEmotesFFZ);
    }

    private static List<EmoteUiModel> getChannelFFZEmotes() {
        return getChannelEmotes(Emotes.channelEmotesFFZ);
    }

    private static List<EmoteUiModel> getGlobal7TVEmotes() {
        return getGlobalEmotes(Emotes.globalEmotes7TV);
    }

    private static List<EmoteUiModel> getChannel7TVEmotes() {
        return getChannelEmotes(Emotes.channelEmotes7TV);
    }

    private static List<EmoteUiModel> getGlobalEmotes(Map<String, Emote> emoteMap) {
        ArrayList<EmoteUiModel> list = new ArrayList<>(emoteMap.size());
        for (Emote emote : emoteMap.values()) {
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static List<EmoteUiModel> getChannelEmotes(Map<Integer, Set<String>> emoteSetMap) {
        Set<String> set = emoteSetMap.get(Data.currentBroadcasterId);
        ArrayList<EmoteUiModel> list = new ArrayList<>(set.size());

        for (String code : set) {
            Emote emote = Emotes.getEmote(Data.currentBroadcasterId, code);
            list.add(emoteToModel(emote));
        }
        return list;
    }

    private static EmoteUiSet getUiSet(Res.strings headerRes, EmotePickerSection section, List<EmoteUiModel> emotes) {
        EmoteHeaderUiModel header = new EmoteHeaderUiModel.EmoteHeaderStringResUiModel(
                ResUtil.getResourceId(headerRes),
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
        EmoteModel model = new EmoteModel.Generic(id, emote.code, emote.getAssetType(), EmoteModelType.OTHER);

        EmoteMessageInput input = new EmoteMessageInput(emote.code, id, false);
        ClickedEmote clickedEmote = new ClickedEmote.Unlocked(model, input, null, null, 12, null);

        return new EmoteUiModel(
                id,
                clickedEmote,
                emote.getAssetType(),
                EmoteImageDescriptor.NONE,
                ResUtil.getResourceId("emote_picker_emote_size", "dimen"),
                ResUtil.getResourceId("emote_picker_emote_padding", "dimen")
        );
    }
}
