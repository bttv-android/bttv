package bttv;

import java.util.List;

import javax.inject.Inject;

import android.content.Context;
import android.util.Log;
import androidx.fragment.app.FragmentActivity;
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

	private static final String EnableBTTVEmotesEventName = "enable_bttv_emotes";
	private static final String EnableBTTVGifEmotesEventName = "enable_bttv_gif_emotes";
	private static final String EnableFFZEmotesEventName = "enable_ffz_emotes";

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

		settingsModels.add(new ToggleMenuModel("BTTV Emotes", "Why would you disable this?", null,
				UserPreferences.getBTTVEmotesEnabled(ctx), false, null, EnableBTTVEmotesEventName, false, null, null,
				null, SettingsPreferencesController.SettingsPreference.BTTVEmotesEnabled, null, 0b1011110110100, null));
		settingsModels.add(new ToggleMenuModel("BTTV Gif Emotes", null, null,
				UserPreferences.getBTTVGifEmotesEnabled(ctx), false, null, EnableBTTVGifEmotesEventName, false, null,
				null, null, SettingsPreferencesController.SettingsPreference.BTTVEmotesEnabled, null, 0b1011110110100,
				null));
		settingsModels.add(new ToggleMenuModel("FrankerFaceZ Emotes", null, null,
				UserPreferences.getFFZEmotesEnabled(ctx), false, null, EnableFFZEmotesEventName, false, null, null,
				null, SettingsPreferencesController.SettingsPreference.BTTVEmotesEnabled, null, 0b1011110110100, null));

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

	class SettingsPreferencesControllerImpl implements SettingsPreferencesController {
		Context ctx;

		SettingsPreferencesControllerImpl(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		public void updatePreferenceBooleanState(ToggleMenuModel toggleMenuModel, boolean z) {

			switch (toggleMenuModel.getEventName()) {
				case EnableBTTVEmotesEventName:
					UserPreferences.setBTTVEmotesEnabled(ctx, z);
					break;
				case EnableBTTVGifEmotesEventName:
					UserPreferences.setBTTVGifEmotesEnabled(ctx, z);
					break;
				case EnableFFZEmotesEventName:
					UserPreferences.setFFZEmotesEnabled(ctx, z);
					break;
				default:
					Log.w("LBTTVSettingsPC", "updatePreferenceBooleanState() Unknown EventType");
					Log.w("LBTTVSettingsPC", toggleMenuModel.getEventName());
			}
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
