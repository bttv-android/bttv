/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.emotes.emotepicker.models;

import kotlin.jvm.internal.BTTVDefaultConstructorMarker;

public abstract class EmoteHeaderUiModel {

    public abstract EmotePickerSection getEmotePickerSection();

    public abstract boolean isProfileUrlVisible();

    public abstract boolean isTopBorderVisible();

    private EmoteHeaderUiModel() {
    }

    public EmoteHeaderUiModel(BTTVDefaultConstructorMarker BTTVDefaultConstructorMarker) {
        this();
    }

    public static final class EmoteHeaderStringResUiModel extends EmoteHeaderUiModel {

        public final int getTitle() {
            return 123;
        }

        @Override
        public boolean isTopBorderVisible() {
            return false;
        }

        @Override
        public EmotePickerSection getEmotePickerSection() {
            return null;
        }

        public EmoteHeaderStringResUiModel(int title, boolean isTopBorderVisible, EmotePickerSection emotePickerSection,
                boolean isProfileUrlVisible, int i2, BTTVDefaultConstructorMarker BTTVDefaultConstructorMarker) {
            this(title, isTopBorderVisible, emotePickerSection, isProfileUrlVisible);
        }

        @Override
        public boolean isProfileUrlVisible() {
            return false;
        }

        public EmoteHeaderStringResUiModel(int title, boolean isTopBorderVisible, EmotePickerSection emotePickerSection,
                boolean isProfileUrlVisible) {
            super(null);
        }
    }

}
