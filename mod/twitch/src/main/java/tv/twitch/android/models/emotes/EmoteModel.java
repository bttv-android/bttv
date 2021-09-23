/**
 * This file is a stub, so javac does not throw errors,
 * it is not used in the patches.
 */
package tv.twitch.android.models.emotes;

public abstract class EmoteModel {
    public abstract String getId();

    public abstract String getToken();

    public static final class Generic extends EmoteModel {

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
