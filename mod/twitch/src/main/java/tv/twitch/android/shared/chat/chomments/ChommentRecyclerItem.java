package tv.twitch.android.shared.chat.chomments;

import android.view.View;

import androidx.annotation.NonNull;

import tv.twitch.android.core.adapters.AbstractTwitchRecyclerViewHolder;
import tv.twitch.android.core.adapters.RecyclerAdapterItem;

public class ChommentRecyclerItem implements RecyclerAdapterItem {
    public static class ChommentItemViewHolder extends AbstractTwitchRecyclerViewHolder {
        public ChommentItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
