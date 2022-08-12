package tv.twitch.android.shared.chat.messagefactory.adapteritem;

import android.view.View;

import androidx.annotation.NonNull;

import tv.twitch.android.core.adapters.AbstractTwitchRecyclerViewHolder;

public class UserNoticeRecyclerItem {
  public static final class UserNoticeViewHolder extends AbstractTwitchRecyclerViewHolder {
    public UserNoticeViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    public final android.widget.TextView getChatMessage() {
      throw new IllegalStateException("");
    }
  }
}
