package bttv.emote;

import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;

/** Original BTTV policy; the caller supplies Twitch's native STATIC decision. */
public final class GifPolicy {
    private GifPolicy() {}

    public static boolean shouldAnimate(boolean nativeStatic) {
        final Res.strings mode;
        try {
            mode = ResUtil.getSelectedDropdownFromSettings(Settings.GifRenderMode);
        } catch (Throwable unavailable) {
            // Never silently animate when the selected preference cannot be read.
            return false;
        }
        return mode == Res.strings.bttv_settings_gif_render_mode_animate_forever
                || (mode == Res.strings.bttv_settings_gif_render_mode_animate && !nativeStatic);
    }
}
