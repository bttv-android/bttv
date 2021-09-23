package bttv.api;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.StyleSpan;

import bttv.ResUtil;
import bttv.settings.Settings;


public class DeletedMessages {

    public static Spanned reSpan(Spanned original) {
        if (!ResUtil.getBooleanFromSettings(Settings.ShowDeletedMessagesEnabled)) {
            return null;
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(original);
        SpannableString postFix = new SpannableString("(removed by mod)");
        postFix.setSpan(new StyleSpan(Typeface.ITALIC), 0, postFix.length(), 0);
        spannableStringBuilder.append(postFix);

        return new SpannedString(spannableStringBuilder);
    }
}
