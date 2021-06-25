package tv.twitch.android.shared.ui.menus.dropdown;

import android.view.View;
import android.widget.ArrayAdapter;

import tv.twitch.android.shared.ui.menus.core.MenuModel;

public class DropDownMenuModel<T> extends MenuModel.SingleItemMenu {
    public DropDownMenuModel(
            ArrayAdapter<T> arrayAdapter,
            int selectedOption,
            String primaryText,
            String secondaryText,
            String auxiliaryText,
            View.OnClickListener onClickListener,
            DropDownMenuModel.DropDownMenuItemSelection<T> dropDownMenuItemSelection) {
        throw new IllegalStateException("DropDownMenuModel() was called");
    }

    public interface DropDownMenuItemSelection<T> {
        void onDropDownItemSelected(DropDownMenuModel<T> dropDownMenuModel, int i);
    }
}
