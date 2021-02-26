/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.emotes.models;

import kotlin.jvm.internal.DefaultConstructorMarker;

public abstract class EmotePickerEmoteModel {
    public abstract java.lang.String getId();

    public abstract java.lang.String getToken();

    private EmotePickerEmoteModel() {
    }

    public EmotePickerEmoteModel(DefaultConstructorMarker defaultConstructorMarker) {
        this();
    }

    public static final class Generic extends EmotePickerEmoteModel {

        @Override
        public java.lang.String getId() {
            return "";
        }

        @Override
        public java.lang.String getToken() {
            return "";
        }

        public Generic(java.lang.String id, java.lang.String token) {
            super(null);
        }
    }

}