package tv.twitch.android.shared.chat.adapter.item;

import android.view.View;

import androidx.annotation.NonNull;

import tv.twitch.android.core.adapters.AbstractTwitchRecyclerViewHolder;

public abstract class ChatMessageViewHolder extends AbstractTwitchRecyclerViewHolder {
  public ChatMessageViewHolder(@NonNull View itemView) {
    super(itemView);
  }
}
