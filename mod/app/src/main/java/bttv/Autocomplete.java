package bttv;

import java.util.ArrayList;
import java.util.List;

import bttv.emote.Emote;
import bttv.emote.Emotes;
import tv.twitch.android.models.emotes.EmoteModelType;
import tv.twitch.android.models.emotes.EmoteSet;
import tv.twitch.android.models.emotes.EmoteModel;

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
        List<EmoteModel.Generic> generics = new ArrayList<>(codes.size());

        for (String code : codes) {
            Emote e = Emotes.getEmote(channelId, code);
            if (e == null)
                continue; // should not happen
            generics.add(
                new EmoteModel.Generic(
                    "BTTV-" + e.id,
                    e.code,
                    e.getAssetType(),
                    EmoteModelType.OTHER)
            );
        }
        EmoteSet set = new EmoteSet.GenericEmoteSet("bttv_emote_set_id", generics);
        list.add(set);
    }
}
