"""Static checks against the actual Twitch 31 Home renderer and normal patches."""
from pathlib import Path
import os
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DECODE = ROOT / '.bttv-cache/disass'


def read(path):
    return (ROOT / path).read_text(encoding='utf-8')


class HideStoriesStaticTests(unittest.TestCase):
    def test_existing_settings_persistence_and_default(self):
        settings = read('mod/app/src/main/java/bttv/settings/Settings.java')
        entry = settings.split('HideTwitchStoriesEnabled(', 1)[1].split('\n    ),', 1)[0]
        self.assertIn('"bttv_hide_twitch_stories"', entry)
        self.assertIn('new Entry.BoolValue(false)', entry)
        prefs = read('mod/app/src/main/java/bttv/settings/UserPreferences.java')
        self.assertIn('ctx.getSharedPreferences("BTTV", 0)', prefs)
        self.assertIn('editor.putBoolean(key, value)', prefs)
        self.assertIn('editor.apply()', prefs)
        ui = read('mod/app/src/main/java/bttv/settings/SettingsActivity.java')
        self.assertIn('for (Settings setting : Settings.values())', ui)
        self.assertLess(ui.index('entry.set(SettingsActivity.this'), ui.index('HideStories.onSettingChanged()'))
        self.assertIn('entry == Settings.HideTwitchStoriesEnabled.entry', ui)
        strings = ET.parse(ROOT/'mod/app/src/main/res/values/strings.xml')
        self.assertEqual(strings.find("string[@name='bttv_settings_hide_twitch_stories']").text,
                         'Hide Twitch Stories')

    def test_real_home_model_to_view_chain(self):
        fragment = (DECODE/'smali/tv/twitch/android/feature/discovery/feed/tab/DiscoveryFeedHomeFragment.smali').read_text()
        mapper = (DECODE/'smali_classes2/l5b.smali').read_text()
        view = (DECODE/'smali/y1e.smali').read_text()
        self.assertIn('invoke-direct {p2, p1}, Ly1e;-><init>(Landroid/content/Context;)V', fragment)
        self.assertIn('Lu1e;->D2(Ly1e;)V', fragment)
        self.assertIn('Lt1e;->b:Z', mapper)
        self.assertIn('Lw1e;-><init>(ZZZZLjava/util/List;Z)V', mapper)
        self.assertIn('Ly1e;->w(Lw1e;)V', mapper)
        self.assertIn('iget-boolean v1, p1, Lw1e;->a:Z', view)
        self.assertIn('State(hasStoriesShelf=', (DECODE/'smali/w1e.smali').read_text())

    def test_stock_absent_branch_removes_whole_parent_and_spacing(self):
        view = (DECODE/'smali/y1e.smali').read_text().split('.method public final w(Lw1e;)V', 1)[1]
        self.assertIn('invoke-virtual {v8, v10}, Landroid/view/View;->setVisibility(I)V', view)
        self.assertIn('invoke-virtual {v7, v9}, Landroid/view/View;->setVisibility(I)V', view)
        self.assertIn('new-instance v10, Lw0e;', view)
        absent = (DECODE/'smali/w0e.smali').read_text()
        self.assertIn('iget-object v0, p0, Lw63;->d:Ljava/lang/Object;', absent)
        self.assertIn('const/16 v2, 0x8', absent)
        self.assertIn('invoke-virtual {p0, v2}, Landroid/view/View;->setVisibility(I)V', absent)
        self.assertIn('iput v4, v11, Landroid/view/ViewGroup$MarginLayoutParams;->topMargin:I', view)
        layout = ET.parse(DECODE/'res/layout/discovery_feed_home_view.xml').getroot()
        android = '{http://schemas.android.com/apk/res/android}'
        app = '{http://schemas.android.com/apk/res-auto}'
        by_id = {e.get(android+'id'): e for e in layout.iter()}
        self.assertEqual(by_id['@id/stories_shelf_container'].get(android+'layout_height'), 'wrap_content')
        self.assertEqual(by_id['@id/mdf_pager'].get(app+'layout_constraintTop_toBottomOf'), '@id/stories_shelf_barrier')
        self.assertEqual(by_id['@id/stories_shelf_barrier'].get(app+'constraint_referenced_ids'),
                         'stories_shelf_container,stories_shelf_compose')

    def test_patches_apply_normally_and_preserve_every_stock_instruction(self):
        (ROOT/'.tmp').mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=ROOT/'.tmp', prefix='stage5n-patch-') as folder:
            folder = Path(folder)
            (folder/'smali').mkdir()
            original = (DECODE/'smali/y1e.smali').read_text()
            (folder/'smali/y1e.smali').write_text(original, newline='\n')
            for name in ('bttvcompat.NativeStoriesBridge.smali.patch', 'y1e.smali.patch', 'zz_y1e_relocation.patch'):
                patch = ROOT/'patches/live'/name
                env = {**os.environ, 'GIT_CEILING_DIRECTORIES': str(ROOT/'.tmp')}
                for args in (['--check'], []):
                    subprocess.run(['git', 'apply', *args, str(patch)], cwd=folder, env=env, check=True)
            self.assertFalse((folder/'smali/y1e.smali').exists())
            final = (folder/'smali_classes7/y1e.smali').read_text()
            extra = ('    invoke-static {p0, p1, v1}, Lbttv/HideStories;->onBind(Ljava/lang/Object;Ljava/lang/Object;Z)Z\n'
                     '    move-result v1\n\n')
            self.assertEqual(final.count(extra), 1)
            self.assertEqual(final.replace(extra, ''), original)
            self.assertIn('.locals 13', final.split('.method public final w(Lw1e;)V', 1)[1])
            bridge = (folder/'smali_classes7/bttvcompat/NativeStoriesBridge.smali').read_text()
            self.assertIn('isAttachedToWindow()Z', bridge)
            self.assertIn('invoke-virtual {p0, p1}, Ly1e;->w(Lw1e;)V', bridge)
            self.assertTrue(bridge.endswith('.end method\n'))

    def test_no_network_mutation_or_polling(self):
        helper = read('mod/app/src/main/java/bttv/HideStories.java')
        for forbidden in ('HttpURLConnection', 'okhttp', 'Timer', 'Thread(', 'Log.', 'setVisibility',
                          'removeView', 'clear()', 'getDeclared', 'currentBroadcasterId'):
            self.assertNotIn(forbidden, helper)
        self.assertIn('WeakHashMap<>', helper)
        self.assertIn('return stockHasStories;', helper)
        self.assertNotIn('iput', read('patches/live/y1e.smali.patch'))
        self.assertNotIn('BTTV31PUSH', helper)


if __name__ == '__main__':
    unittest.main()
