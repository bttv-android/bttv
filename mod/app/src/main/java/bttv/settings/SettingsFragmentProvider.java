package bttv.settings;

public class SettingsFragmentProvider implements javax.inject.Provider<SettingsFragmentBinding.Factory> {

    private static SettingsFragmentProvider instance;

    public static SettingsFragmentProvider getInstance() {
        if (instance == null) {
            instance = new SettingsFragmentProvider();
        }
        return instance;
    }

    @Override
    public SettingsFragmentBinding.Factory get() {
        return new SettingsFragmentBindingFactory();
    }

}
