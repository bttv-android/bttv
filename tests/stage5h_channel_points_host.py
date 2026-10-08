"""Generate isolated JVM fixtures for ChannelPoints one-shot behavior.

Usage: python tests/stage5h_channel_points_host.py <ignored-output-directory>
Compile the generated stubs and runner with the production ChannelPoints.java.
"""

from pathlib import Path
import sys


SOURCES = {
    "android/view/View.java": """package android.view;
public class View {
    public interface OnClickListener { void onClick(View view); }
}
""",
    "bttv/settings/Settings.java": """package bttv.settings;
public enum Settings { AutoRedeemChannelPointsEnabled }
""",
    "bttv/ResUtil.java": """package bttv;
public final class ResUtil {
    public static boolean enabled;
    public static boolean getBooleanFromSettings(bttv.settings.Settings setting) { return enabled; }
}
""",
    "bttv/ChannelPointsHostTest.java": """package bttv;
import android.view.View;
public final class ChannelPointsHostTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        ChannelPoints.resetAutoRedeemClaimsForTest();
        check(!ChannelPoints.shouldAutoRedeem("claim-a", false), "OFF attempted a claim");
        check(ChannelPoints.shouldAutoRedeem("claim-a", true), "ON did not claim available bonus");
        check(!ChannelPoints.shouldAutoRedeem("claim-a", true), "duplicate bonus was claimed twice");
        check(ChannelPoints.shouldAutoRedeem("claim-b", true), "new bonus was not claimable");
        check(!ChannelPoints.shouldAutoRedeem("claim-b", true), "second bonus was claimed twice");
        check(!ChannelPoints.shouldAutoRedeem(null, true), "null event was treated as a bonus");
        check(!ChannelPoints.shouldAutoRedeem("", true), "empty event was treated as a bonus");

        final int[] clicks = {0};
        View.OnClickListener listener = view -> clicks[0]++;
        ResUtil.enabled = false;
        ChannelPoints.autoRedeem(listener);
        check(clicks[0] == 0, "legacy OFF path clicked");
        ResUtil.enabled = true;
        ChannelPoints.autoRedeem(listener);
        check(clicks[0] == 1, "legacy ON path did not click exactly once");
        System.out.println("PASS ChannelPoints OFF/ON/one-shot/invalid-event behavior");
    }
}
""",
}


if __name__ == "__main__":
    output = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding="utf-8")
    print(f"Generated {len(SOURCES)} isolated ChannelPoints JVM fixtures in {output}")
