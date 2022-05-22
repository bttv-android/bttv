package tv.twitch.android.shared.chat.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import tv.twitch.android.core.adapters.AbstractTwitchRecyclerViewHolder;

public class MessageRecyclerItem implements tv.twitch.android.core.adapters.RecyclerAdapterItem {
    public static class ChatMessageViewHolder extends AbstractTwitchRecyclerViewHolder {
        public ChatMessageViewHolder(@NonNull View itemView, boolean bool) {
            super(itemView);
        }

        public final TextView getMessageTextView() {
            throw new IllegalStateException("MessageRecyclerItem.ChatMessageViewHolder.getMessageTextView mock was called");
        }
    }
}
