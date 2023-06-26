/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.emotes.emotepicker.models;

import java.util.List;

import kotlin.jvm.internal.BTTVDefaultConstructorMarker;
import tv.twitch.android.models.emotes.EmoteModel;
import tv.twitch.android.shared.emotes.emotepicker.EmotePickerTrackingMetadata;
import tv.twitch.android.shared.emotes.models.EmoteMessageInput;

public abstract class ClickedEmote {

    public ClickedEmote() {}

    public ClickedEmote(BTTVDefaultConstructorMarker BTTVDefaultConstructorMarker) {
        this();
    }

    public static final class Unlocked extends ClickedEmote {

        public final EmoteModel getEmote() {
            return null;
        }

        public final EmoteMessageInput getEmoteMessageInput() {
            return null;
        }

        public final EmotePickerTrackingMetadata getTrackingMetadata() {
            return null;
        }

        public Unlocked(EmoteModel emote, EmoteMessageInput emoteMessageInput,
                        EmotePickerTrackingMetadata emotePickerTrackingMetadata,
                        List<ClickedEmote.Unlocked> list, int i,
                        BTTVDefaultConstructorMarker BTTVDefaultConstructorMarker) {
            this(emote, emoteMessageInput, emotePickerTrackingMetadata, list);
        }

        public final List<ClickedEmote.Unlocked> getModifiedEmotes() {
            return null;
        }

        public Unlocked(EmoteModel emote, EmoteMessageInput emoteMessageInput,
                        EmotePickerTrackingMetadata emotePickerTrackingMetadata,
                        List<ClickedEmote.Unlocked> list) {
            super(null);
        }
    }
}
