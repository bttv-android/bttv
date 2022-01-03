package bttv.settings;

import android.view.View;

import androidx.appcompat.widget.SwitchCompat;

import bttv.Res;
import bttv.ResUtil;
import bttv.Data;

public class StreamSettings {
  public static void setupBTTVStreamSettings(View view) {
    setupEnableBTTVEmotes(view);
  }

  private static void setupEnableBTTVEmotes(View view) {
    int id = ResUtil.getResourceId(Data.ctx, Res.ids.bttv_stream_settings_enable_bttv_emotes_toggle);
    SwitchCompat toggle = view.findViewById(id);
    boolean checked = ResUtil.getBooleanFromSettings(Settings.BTTVEmotesEnabled);
    toggle.setChecked(checked);
    toggle.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });
  }
}
