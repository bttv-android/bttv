package bttv.emote;

import android.util.Log;

import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import io.reactivex.Single;
import tv.twitch.android.core.strings.StringResource;
import tv.twitch.android.models.emotes.EmoteCardModelResponse;
import tv.twitch.android.models.emotes.EmoteCardModel;
import tv.twitch.android.models.emotes.EmoteCardType;


public class EmoteCardUtil {

    public static class BTTVEmoteCardType extends EmoteCardType.GenericEmoteCardType.Global {
        public static final BTTVEmoteCardType INSTANCE = new BTTVEmoteCardType();
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
        EmoteCardModel model = new EmoteCardModel.GenericEmoteCardModel(
                emoteId,
                emote.code,
                emote.getAssetType(),
                BTTVEmoteCardType.INSTANCE
        );
        EmoteCardModelResponse resp = new EmoteCardModelResponse.Success(model);
        return Single.just(resp);
    }

    // called in Ltv/twitch/android/shared/chat/emotecard/EmoteCardPresenter.getEmoteTypeStringResource()
    public static StringResource getEmoteDescriptionOrFallback(EmoteCardModel.GenericEmoteCardModel emoteCardModel, StringResource fallback) {
        try {
            String string = getEmoteDescription(emoteCardModel.emoteId);
            if (string != null) {
                return StringResource.Companion.fromString(string);
            }
        } catch (Throwable t) {
            Log.e("LBTTVEmoteCardUtil", "getEmoteDescriptionOrFallback: ", t);
        }
        Log.w("LBTTVEmoteCardUtil", "getEmoteDescriptionOrFallback: returning fallback string");
        return fallback;
    }

    private static String getEmoteDescription(String emoteId) {
        try {
            String fallback = ResUtil.getLocaleString(Data.ctx, Res.strings.bttv_emote_added_by_bttv);
            String id = EmoteUrlUtil.extractBTTVId(emoteId);
            if (id == null) {
                return null;
            }
            Emote emote = Emotes.getEmoteById(id);

            if (emote == null) {
                Log.w("LBTTVEmoteCardUtil", "could not find emote for BTTVEmoteCardModel, emoteId: " + emoteId);
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
            return null;
        }
    }
}
