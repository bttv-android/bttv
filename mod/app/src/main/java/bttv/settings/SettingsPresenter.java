package bttv.settings;

import java.util.List;

import javax.inject.Inject;

import android.content.Context;
import android.util.Log;
import androidx.fragment.app.FragmentActivity;

import bttv.settings.abstractions.Switch;
import tv.twitch.android.core.adapters.HeaderConfig;
import tv.twitch.android.core.adapters.SectionHeaderDisplayConfig;
import tv.twitch.android.settings.base.BaseSettingsPresenter;
import tv.twitch.android.settings.base.SettingsTracker;
import tv.twitch.android.shared.ui.menus.SettingsPreferencesController;
import tv.twitch.android.shared.ui.menus.checkable.CheckableGroupModel;
import tv.twitch.android.shared.ui.menus.core.MenuAdapterBinder;
import tv.twitch.android.shared.ui.menus.core.MenuModel;
import tv.twitch.android.shared.ui.menus.core.MenuSection;
import tv.twitch.android.shared.ui.menus.togglemenu.ToggleMenuModel;

public final class SettingsPresenter extends BaseSettingsPresenter {

	@Inject
	public SettingsPresenter(FragmentActivity fragmentActivity, MenuAdapterBinder menuAdapterBinder,
			SettingsTracker settingsTracker) {
		super(fragmentActivity, menuAdapterBinder, settingsTracker);
	}

	@Override
	public void onActive() {
		super.onActive();
		// TODO: load settings from SP
	}

	@Override
	public String getToolbarTitle() {
		return "BTTV";
	}

	@Override
	public void updateSettingModels() {
		Context ctx = getActivity().getApplicationContext();
		List<MenuModel> settingsModels = getSettingModels();
		settingsModels.clear();

		for (Settings setting : Settings.values()) {
			settingsModels.add(Switch.fromEntry(ctx, setting.entry));
		}

		bindSettings();
	}

	@Override
	public void bindSettings() {
		MenuAdapterBinder binder = getAdapterBinder();
		binder.getAdapter().removeAllSections();

		MenuSection menuSection = new MenuSection(getSettingModels(), null, null, 0b110, null);
		HeaderConfig headerCfg = new HeaderConfig(SectionHeaderDisplayConfig.IF_CONTENT,
				"Fine tune your BTTV experience", null, 0, 0, null, null, false, null, null, 0b1111111100, null);
		menuSection.updateHeaderConfig(headerCfg);
		binder.bindModels(getSettingModels(), getMSettingActionListener(), menuSection, null);
	}

	static class SettingsPreferencesControllerImpl implements SettingsPreferencesController {
		Context ctx;

		SettingsPreferencesControllerImpl(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		public void updatePreferenceBooleanState(ToggleMenuModel toggleMenuModel, boolean z) {
			String key = toggleMenuModel.getEventName();
			Settings setting = Settings.get(key);

			if (setting == null) {
				Log.w("LBTTVSettingsPC", "updatePreferenceBooleanState() Unknown key");
				Log.w("LBTTVSettingsPC", toggleMenuModel.getEventName());
				return;
			}
			if (!(setting.entry instanceof UserPreferences.Entry.BoolEntry)) {
				Log.w("LBTTVSettingsPC", "updatePreferenceBooleanState() not a BoolEntry");
				Log.w("LBTTVSettingsPC", setting.toString());
			}

			setting.entry.set(ctx, new UserPreferences.Entry.BoolValue(z));
		}

		@Override
		public void updatePreferenceCheckedState(CheckableGroupModel checkableGroupModel) {
			Log.d("LBTTVSettingsPC",
					"updatePreferenceCheckedState() checkableGroupModel: " + checkableGroupModel.toString());
		}

	}

	@Override
	public SettingsPreferencesController getPrefController() {
		return new SettingsPreferencesControllerImpl(getActivity().getApplicationContext());
	}
}
