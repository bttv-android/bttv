package bttv;

import android.view.View.OnClickListener;

import java.util.HashSet;
import java.util.Set;

import bttv.settings.Settings;

public class ChannelPoints {
    private static final int MAX_RECORDED_CLAIMS = 128;
    private static final Set<String> autoRedeemedClaimIds = new HashSet<>();

    public static void autoRedeem(OnClickListener onClickListener) {
        if (ResUtil.getBooleanFromSettings(Settings.AutoRedeemChannelPointsEnabled)) {
            onClickListener.onClick(null);
        }
    }

    public static boolean shouldAutoRedeem(String claimId) {
        return shouldAutoRedeem(
                claimId,
                ResUtil.getBooleanFromSettings(Settings.AutoRedeemChannelPointsEnabled)
        );
    }

    static synchronized boolean shouldAutoRedeem(String claimId, boolean enabled) {
        if (!enabled || claimId == null || claimId.isEmpty()) {
            return false;
        }

        if (!autoRedeemedClaimIds.add(claimId)) {
            return false;
        }

        if (autoRedeemedClaimIds.size() > MAX_RECORDED_CLAIMS) {
            autoRedeemedClaimIds.clear();
            autoRedeemedClaimIds.add(claimId);
        }

        return true;
    }

    static synchronized void resetAutoRedeemClaimsForTest() {
        autoRedeemedClaimIds.clear();
    }
}
