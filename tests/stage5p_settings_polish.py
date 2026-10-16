"""Settings presentation/link invariants against stock Twitch and original BTTV."""
import hashlib
import json
from pathlib import Path
import re
import subprocess
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DECODE = ROOT/'.bttv-cache/disass'
BASE = '5057fba3bb2db06be972802e2437a98eabfece06'


def read(path): return (ROOT/path).read_text(encoding='utf-8')


class SettingsPolishTests(unittest.TestCase):
    def test_only_native_entry_and_no_toolbar_side_effects(self):
        nav=read('mod/app/src/main/java/bttv/settings/SettingsNavigation.java')
        body=nav.split('public static void install(',1)[1]
        self.assertNotIn('menu.',body)
        self.assertNotIn('startActivity(',body)
        for name,method in [('ffo','appendLegacy'),('cl2','handleLegacy'),('r2k','drawMain')]:
            patch=read(f'patches/live/{name}.smali.patch')
            self.assertEqual(patch.count('NativeSettingsBridge;->'+method),1)

    def test_actual_native_layouts_typography_toolbar_and_switch(self):
        ui=read('mod/app/src/main/java/bttv/settings/SettingsActivity.java')
        for layout in ('toggle_menu_recycler_item','sub_menu_recycler_item','drop_down_menu_recycler_item'):
            self.assertIn('"'+layout+'"',ui)
            text=(DECODE/f'res/layout/{layout}.xml').read_text()
            self.assertIn('tv.twitch.android.core.ui.kit.principles.typography.TitleSmall',text if layout!='sub_menu_recycler_item' else (DECODE/'res/layout/menu_item_core.xml').read_text())
        self.assertIn('androidx.appcompat.widget.SwitchCompat',(DECODE/'res/layout/toggle_menu_recycler_item.xml').read_text())
        self.assertIn('Theme.Twitch.ToolBarTitle',ui)
        self.assertIn('ic_arrow_left',ui)
        self.assertIn('setNavigationOnClickListener(ignored -> finish())',ui)
        self.assertIn('Spinner spinner = row.findViewById',ui)
        self.assertIn('twitch_spinner_dropdown_item',ui)
        self.assertNotIn('new Button(',ui)
        self.assertNotIn('Feature behavior is enabled',ui)

    def test_preferences_and_feature_code_unchanged(self):
        for path in ('mod/app/src/main/java/bttv/settings/Settings.java',
                     'mod/app/src/main/java/bttv/settings/UserPreferences.java',
                     'mod/app/src/main/java/bttv/AnonChat.java',
                     'patches/AndroidManifest.xml.patch',
                     'patches/settings/tv.twitch.android.settings.SettingsActivity.smali.patch'):
            original=subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT).decode().replace('\r\n','\n')
            self.assertEqual(read(path),original,path)
        ui=read('mod/app/src/main/java/bttv/settings/SettingsActivity.java')
        self.assertLess(ui.index('entry.set(SettingsActivity.this'),ui.index('AnonChat.reconnect(checked)'))
        self.assertIn('HideStories.onSettingChanged()',ui)
        for action in ('Highlight.getInstance().openDialog(this)', 'Blacklist.getInstance().openDialog(this)', 'Credits.openDialog(this)'):
            self.assertIn(action,ui)
        self.assertIn('value.newWithSelected(choices.get(position))',ui)

    def test_scrolling_insets_accessibility_and_local_dark_mode(self):
        ui=read('mod/app/src/main/java/bttv/settings/SettingsActivity.java')
        for expected in ('setFillViewport(true)','setOnApplyWindowInsetsListener',
                         'getSystemWindowInsetTop()', 'getSystemWindowInsetBottom()',
                         'requestApplyInsets()', 'setAccessibilityHeading(true)',
                         'setMinimumHeight(dp(56))', 'setMinHeight(dp(48))',
                         'Configuration.UI_MODE_NIGHT_YES', 'createConfigurationContext(config)'):
            self.assertIn(expected,ui)
        self.assertNotIn('AppCompatDelegate.setDefaultNightMode',ui)
        self.assertNotIn('setOnTouchListener',ui)

    def test_original_credit_destinations_and_attribution_preserved(self):
        original=subprocess.check_output(['git','show',BASE+':.all-contributorsrc'],cwd=ROOT)
        current=(ROOT/'.all-contributorsrc').read_bytes()
        self.assertEqual(current.decode().replace('\r\n','\n'),original.decode().replace('\r\n','\n'))
        data=json.loads(current)
        self.assertTrue(data['contributors'])
        for contributor in data['contributors']:
            self.assertTrue(contributor['login'])
            self.assertTrue(contributor['contributions'])
        historical=subprocess.check_output(['git','show','7fcbe92:mod/app/src/main/java/bttv/Credits.java'],cwd=ROOT).decode()
        self.assertIn('getString("profile")',historical)
        self.assertIn('new LinkSpan(contributor.url, activity)',historical)
        now=read('mod/app/src/main/java/bttv/Credits.java')
        self.assertIn('optString("profile", "")',now)
        self.assertIn('CreditsLinks.destination(contributor.url)',now)
        self.assertIn('new LinkSpan(destination, activity)',now)
        self.assertIn('setMovementMethod(LinkMovementMethod.getInstance())',now)
        self.assertIn('setOnClickListener(ignored -> openLink(activity, destination))',now)
        self.assertIn('LinkedHashMap',now)
        self.assertIn('ActivityNotFoundException | SecurityException',now)
        self.assertNotIn('Log.',now)
        self.assertIn('sStr instanceof Spanned',now)
        # Intro annotations (including original GitHub/FoseFx link) untouched.
        old_strings=ET.fromstring(subprocess.check_output(['git','show',BASE+':mod/app/src/main/res/values/strings.xml'],cwd=ROOT))
        new_strings=ET.parse(ROOT/'mod/app/src/main/res/values/strings.xml')
        for e in old_strings.findall("string"):
            if e.get('name','').startswith('bttv_credits_'):
                self.assertEqual(ET.tostring(e).strip(),ET.tostring(new_strings.find(f"string[@name='{e.get('name')}']")).strip())
        layout=read('mod/app/src/main/res/layout/bttv_credits_dialog.xml')
        self.assertNotIn('Theme.Holo',layout)
        self.assertIn('<ScrollView',layout)
        # Native-only color/style names must not leak into mod XML compilation.
        colors={e.get('name') for e in ET.parse(ROOT/'mod/twitch/src/main/res/values/colors.xml').getroot()}
        self.assertTrue(set(re.findall(r'@color/([\w_]+)',layout)) <= colors)

    def test_preserved_stage5p_and_stage5o_archives(self):
        for name,digest in [
            ('BTTV_Stage5P_B_NativeSettings_Test.apk','315AB1911D114C8BF61ECB3D938C5F9FEE2024F1C05458E397547E2D495969FF'),
            ('BTTV_Stage5O_ChatTimestamps_Test.apk','F03866623A2F437BF5FF7062EE8A582085FA6595BC83A04D0CA7CC5613627F7C')]:
            self.assertEqual(hashlib.sha256((ROOT/'dist/archive'/name).read_bytes()).hexdigest().upper(),digest)


if __name__ == '__main__': unittest.main()
