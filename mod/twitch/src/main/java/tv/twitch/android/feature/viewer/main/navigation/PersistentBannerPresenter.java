package tv.twitch.android.feature.viewer.main.navigation;

public class PersistentBannerPresenter {
    public interface PersistentBannerPresenterListener {
        void installBannerShown();

        void installUpdate();

        void updateDismissed();
    }

    public void setShouldShowUpdateBanner(boolean b) {

    }

    public void setListener(PersistentBannerPresenterListener lnr){

    }
}
