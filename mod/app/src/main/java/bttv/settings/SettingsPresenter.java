package bttv.settings;

import java.util.List;

import javax.inject.Inject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.FragmentActivity;

import bttv.Credits;
import bttv.highlight.Highlight;
import bttv.settings.abstractions.Dropdown;
import bttv.settings.abstractions.Link;
import bttv.settings.abstractions.Switch;
import tv.twitch.android.core.adapters.HeaderConfig;
import tv.twitch.android.core.adapters.SectionHeaderDisplayConfig;
import tv.twitch.android.models.settings.SettingsDestination;
import tv.twitch.android.settings.base.BaseSettingsPresenter;
import tv.twitch.android.settings.base.SettingsNavigationController;
import tv.twitch.android.settings.base.SettingsTracker;
import tv.twitch.android.shared.ui.menus.SettingsPreferencesController;
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
		Context ctx = getActivity();
		List<MenuModel> settingsModels = getSettingModels();
		settingsModels.clear();

		for (Settings setting : Settings.values()) {
			if (setting.entry.primaryTextResource == null) {
				continue;
			}
			if (setting.entry instanceof UserPreferences.Entry.BoolEntry) {
				settingsModels.add(Switch.fromEntry(ctx, (UserPreferences.Entry.BoolEntry) setting.entry));
			} else if (setting.entry instanceof UserPreferences.Entry.LinkEntry) {
				settingsModels.add(Link.fromEntry(ctx, (UserPreferences.Entry.LinkEntry) setting.entry));
			} else if (setting.entry instanceof UserPreferences.Entry.DropDownEntry) {
				settingsModels.add(Dropdown.fromEntry(ctx, (UserPreferences.Entry.DropDownEntry) setting.entry));
			}
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


	@Override
	public SettingsNavigationController getNavController() {
		final Activity context = getActivity();
		return new SettingsNavigationController() {
			@Override
			public void navigateToSettingFragment(SettingsDestination settingsDestination, Bundle bundle) {
				Log.i("LBTTVDEBUG", "navigateToSettingFragment: " + settingsDestination);
				if (settingsDestination.equals(SettingsDestination.BTTV_HIGHLIGHTS)) {
					Highlight.openDialog(context);
				} else if (settingsDestination.equals(SettingsDestination.BTTV_CREDITS)) {
					Credits.openDialog(context);
				}
			}
		};
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

	}

	@Override
	public SettingsPreferencesController getPrefController() {
		return new SettingsPreferencesControllerImpl(getActivity().getApplicationContext());
	}
}
