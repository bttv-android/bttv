package android.app;

import android.content.Context;
import android.content.DialogInterface;

public class AlertDialog extends Dialog {
    public static class Builder {
        public Builder(Context context) {

        }

        public Builder setTitle(CharSequence s) {
            return this;
        }

        public Builder setMessage(CharSequence s) {
            return this;
        }

        public Builder setPositiveButton(CharSequence s, DialogInterface.OnClickListener listener) {
            return this;
        }

        public Builder setNegativeButton(CharSequence s, DialogInterface.OnClickListener listener) {
            return this;
        }

        public AlertDialog create() {
            return null;
        }

    }

}
