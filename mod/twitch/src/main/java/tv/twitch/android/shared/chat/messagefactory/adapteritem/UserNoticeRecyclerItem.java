package tv.twitch.android.shared.chat.messagefactory.adapteritem;

import android.view.View;

import androidx.annotation.NonNull;

import tv.twitch.android.core.adapters.AbstractTwitchRecyclerViewHolder;
import tv.twitch.android.core.mvp.viewdelegate.EventDispatcher;

public class UserNoticeRecyclerItem {
  public static final class UserNoticeViewHolder extends AbstractTwitchRecyclerViewHolder {
    public UserNoticeViewHolder(@NonNull View itemView, EventDispatcher eventDispatcher) {
      super(itemView);
    }
  }
}
