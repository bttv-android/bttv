package tv.twitch.android.models.emotes;

import java.util.List;

public abstract class EmoteSet {
    public abstract List<EmoteModel> getEmotes();
    public abstract java.lang.String getSetId();


    public static final class GenericEmoteSet extends EmoteSet {

        public GenericEmoteSet(String str, List<EmoteModel.Generic> list) {
            throw new IllegalStateException("EmoteSet.GenericEmoteSet constructor called");
        }

        @Override
        public List<EmoteModel> getEmotes() {
            throw new IllegalStateException("EmoteSet.GenericEmoteSet getEmotes() called");
        }

        @Override
        public String getSetId() {
            throw new IllegalStateException("EmoteSet.GenericEmoteSet getSetId() called");
        }
    }
}
