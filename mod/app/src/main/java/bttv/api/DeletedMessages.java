package bttv.api;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.StyleSpan;

import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;


public class DeletedMessages {

    public static Spanned reSpan(Spanned original) {
        if (!ResUtil.getBooleanFromSettings(Settings.ShowDeletedMessagesEnabled)) {
            return null;
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(original);
        String removedByModText = ResUtil.getLocaleString(Data.ctx, Res.strings.bttv_removed_by_mod);
        SpannableString postFix = new SpannableString("(" + removedByModText + ")");
        postFix.setSpan(new StyleSpan(Typeface.ITALIC), 0, postFix.length(), 0);
        spannableStringBuilder.append(postFix);

        return new SpannedString(spannableStringBuilder);
    }

    public static Spanned restoreNativeContainer(Spanned container, Object placeholder, Spanned restoredBody) {
        int start = container.getSpanStart(placeholder);
        int end = container.getSpanEnd(placeholder);
        if (start < 0 || end <= start) {
            return restoredBody;
        }

        SpannableStringBuilder result = new SpannableStringBuilder(container);
        result.removeSpan(placeholder);
        result.replace(start, end, restoredBody);
        return new SpannedString(result);
    }
}
