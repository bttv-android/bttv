package tv.twitch.android.shared.chat.chomments;

import android.view.View;

import androidx.annotation.NonNull;

import tv.twitch.android.core.adapters.RecyclerAdapterItem;

public class ChommentRecyclerItem implements RecyclerAdapterItem {
    public static class ChommentItemViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        public ChommentItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        public final android.widget.TextView getChommentTextView() {
            throw new IllegalStateException("ChommentRecyclerItem.ChommentItemViewHolder.getChommentTextView mock was called");
        }
    }
}
