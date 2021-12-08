package bttv.emote;

import android.util.Log;

import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import io.reactivex.Single;
import tv.twitch.android.models.emotes.EmoteCardModelResponse;
import tv.twitch.android.models.emotes.EmoteCardModel;
import tv.twitch.android.models.emotes.EmoteModelAssetType;


public class EmoteCardUtil {

    public static class BTTVEmoteCardModel extends EmoteCardModel.GlobalEmoteCardModel {
        String emoteId;
        public BTTVEmoteCardModel(String emoteId, String token) {
            super(emoteId, token, EmoteModelAssetType.ANIMATED);
            this.emoteId = emoteId;
        }

        // called in Ltv/twitch/android/shared/chat/emotecard/EmoteCardViewDelegate.renderGenericEmoteCard()
        public String getEmoteDesc() {
            try {
                String fallback = ResUtil.getLocaleString(Data.ctx, Res.strings.bttv_emote_added_by_bttv);
                String id = EmoteUrlUtil.extractBTTVId(this.emoteId);
                Emote emote = Emotes.getEmoteById(id);

                if (emote == null) {
                    Log.w("LBTTVEmoteCardUtil", "could not find emote for BTTVEmoteCardModel, emoteId: " + this.emoteId);
                    return fallback;
                }

                String owner = emote.owner;

                if (owner == null)
                    return fallback; // can happen (and will happen for BTTV emotes)

                Res.strings templateStrings;
                switch (emote.source) {
                    case STV:
                        templateStrings = Res.strings.bttv_emote_added_by_bttv_stv;
                        break;
                    case FFZ:
                        templateStrings = Res.strings.bttv_emote_added_by_bttv_ffz;
                        break;
                    default:
                        return fallback;
                }

                String template = ResUtil.getLocaleString(Data.ctx, templateStrings);
                return String.format(template, owner);
            } catch (Throwable t) {
                Log.e("LBTTVEmoteCardUtil", "getEmoteDesc(): ", t);
                return "An error has occurred";
            }
        }
    }

    // called in Ltv/twitch/android/shared/chat/api/EmoteCardApi.getEmoteCardModelResponse()
    public static Single<EmoteCardModelResponse> getEmoteResponseOrNull(String emoteId) {
        if (!emoteId.startsWith("BTTV-")) {
            return null;
        }
        String realId = emoteId.split("BTTV-")[1];
        Emote emote = Emotes.getEmoteById(realId);
        if (emote == null) { // triggers error dialog
            return null;
        }
        EmoteCardModel model = new BTTVEmoteCardModel(emoteId, emote.code);
        EmoteCardModelResponse resp = new EmoteCardModelResponse.Success(
                model);
        return Single.just(resp);
    }
}
