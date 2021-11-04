package tv.twitch.android.shared.chat.messagefactory.adapteritem;

import android.view.View;

import androidx.annotation.NonNull;

public class UserNoticeRecyclerItem {
  public static final class UserNoticeViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
    public UserNoticeViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    public final android.widget.TextView getChatMessage() {
      throw new IllegalStateException("");
    }
  }
}
