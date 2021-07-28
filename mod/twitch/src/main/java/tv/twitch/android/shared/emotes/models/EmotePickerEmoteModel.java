/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.shared.emotes.models;

public abstract class EmotePickerEmoteModel {
    public abstract String getId();

    public abstract String getToken();

    public static final class Generic extends EmotePickerEmoteModel {

        @Override
        public String getId() {
            return "";
        }

        @Override
        public String getToken() {
            return "";
        }

        public Generic(String id, String token, EmoteModelAssetType assetType) {
        }
    }

}
