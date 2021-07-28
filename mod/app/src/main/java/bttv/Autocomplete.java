package bttv;

import java.util.ArrayList;
import java.util.List;

import bttv.emote.Emote;
import bttv.emote.Emotes;
import tv.twitch.android.shared.emotes.models.EmoteSet;
import tv.twitch.android.shared.emotes.models.EmoteModelAssetType;
import tv.twitch.android.shared.emotes.models.EmotePickerEmoteModel;

public class Autocomplete {

    public static void addOurEmotes(List<EmoteSet> list) {
        for (EmoteSet set : list) {
            if (set.getSetId().equals("bttv_emote_set_id")) {
                return;
            }
        }
        int channelId = Data.currentBroadcasterId;
        if (list.isEmpty()) {
            return;
        }
        ArrayList<String> codes = Emotes.getAllEmotes(channelId);
        List<EmotePickerEmoteModel.Generic> generics = new ArrayList<>(codes.size());

        for (String code : codes) {
            Emote e = Emotes.getEmote(channelId, code);
            if (e == null)
                continue; // should not happen
            generics.add(new EmotePickerEmoteModel.Generic("BTTV-" + e.id, e.code, EmoteModelAssetType.STATIC));
        }
        EmoteSet set = new EmoteSet.GenericEmoteSet("bttv_emote_set_id", generics);
        list.add(set);
    }
}
