"""Native Settings entry invariants against the actual Twitch 31 sources."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DECODE = ROOT/'.bttv-cache/disass'
BASE = '5057fba3bb2db06be972802e2437a98eabfece06'
PATCHES = ('bttvcompat.NativeSettingsBridge.smali.patch', 'cl2.smali.patch',
           'ffo.smali.patch', 'r2k.smali.patch')


def read(path): return (ROOT/path).read_text(encoding='utf-8')


def apply_patches(folder):
    (folder/'smali_classes5').mkdir()
    for name in ('cl2', 'ffo', 'r2k'):
        target = folder/f'smali_classes5/{name}.smali'
        target.write_text((DECODE/f'smali_classes5/{name}.smali').read_text(), newline='\n')
    for patch in PATCHES:
        for args in (['--check'], []):
            subprocess.run(['git', 'apply', *args, str(ROOT/'patches/live'/patch)], cwd=folder,
                           check=True, env={**os.environ, 'GIT_CEILING_DIRECTORIES': str(ROOT/'.tmp')})


class NativeSettingsTests(unittest.TestCase):
    def test_normal_application_and_all_stock_instructions_preserved(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.tmp', prefix='stage5p-patch-') as folder:
            folder=Path(folder)
            apply_patches(folder)
            extras = {
                'ffo': '    invoke-static {v3, v0}, Lbttvcompat/NativeSettingsBridge;->appendLegacy(Landroid/app/Activity;Ljava/util/List;)V\n',
                'cl2': ('    invoke-static {p0, p1}, Lbttvcompat/NativeSettingsBridge;->handleLegacy(Ljava/lang/Object;Ljava/lang/Object;)Z\n'
                        '    move-result v0\n    if-eqz v0, :bttv_native_stock\n    return-void\n    :bttv_native_stock\n\n'),
                'r2k': '    invoke-static {v9}, Lbttvcompat/NativeSettingsBridge;->drawMain(Lqw9;)V\n\n'}
            for name, extra in extras.items():
                final=(folder/f'smali_classes5/{name}.smali').read_text()
                self.assertEqual(final.count(extra),1,name)
                self.assertEqual(final.replace(extra,''),(DECODE/f'smali_classes5/{name}.smali').read_text(),name)
            final=(folder/'smali_classes5/r2k.smali').read_text()
            self.assertIn(extras['r2k']+'    invoke-static/range {v13 .. v19}, Lr2k;->i(',final)
            self.assertTrue((folder/'smali_classes7/bttvcompat/NativeSettingsBridge.smali').read_text().endswith('.end method\n'))
            self.assertTrue((folder/'smali_classes7/bttvcompat/NativeSettingsClick.smali').exists())

    def test_legacy_native_model_identity_and_exact_listener_guard(self):
        model=(DECODE/'smali_classes6/a120.smali').read_text()
        self.assertIn('.class public final La120;',model)
        self.assertNotIn(' equals(',model)
        self.assertNotIn(' hashCode(',model)
        bridge=read('patches/live/bttvcompat.NativeSettingsBridge.smali.patch')
        self.assertIn('La120;-><init>(Ljava/lang/String;Ljava/lang/CharSequence;Ljava/lang/String;Lb6z;Z)V',bridge)
        self.assertIn('const/4 v3, 0x0',bridge)  # displayed auxiliary field stays null
        self.assertIn('const/4 v4, 0x0',bridge)  # no invented native enum destination
        for guard in ('hasNativeRow(Ljava/util/List;)Z','isNativeRow(Ljava/lang/Object;)Z',
                      'instance-of v0, v1, Lffo;', 'Lcl2;->b:I', 'Ldl2;->f:Landroidx/fragment/app/n;'):
            self.assertIn(guard,bridge)
        self.assertNotIn('Lb6z;->',bridge)
        navigation=read('mod/app/src/main/java/bttv/settings/SettingsNavigation.java')
        self.assertIn('Map<Object, Boolean> nativeRows = new WeakHashMap<>()',navigation)
        self.assertIn('nativeRows.put(row, Boolean.TRUE)',navigation)

    def test_v2_native_row_context_and_weak_callback(self):
        bridge=read('patches/live/bttvcompat.NativeSettingsBridge.smali.patch')
        self.assertIn('Lvp0;->b:Ld6f;',bridge)
        self.assertIn('Lfsi;->m(Landroidx/compose/runtime/g;)Ljava/lang/Object;',bridge)
        self.assertIn('Lr2k;->w(Ljava/lang/String;Ljava/lang/String;Lngi;ILwqj;Lqw9;II)V',bridge)
        self.assertNotIn('Lr2k;->v(',bridge)
        self.assertIn('const v3, 0x7f080646',bridge)
        self.assertIn('const/16 v7, 0x12',bridge)
        self.assertIn('+.field private final activity:Ljava/lang/ref/WeakReference;',bridge)
        self.assertNotIn('+.field static',bridge)
        self.assertNotIn('+.field private final activity:Landroid/app/Activity;',bridge)
        self.assertIn('Ltf50;->a:Ltf50;',bridge)
        # Do not swallow exceptions once the native row has begun Compose work.
        self.assertLess(bridge.index(':try_end_prepare'),bridge.index('Lr2k;->w('))

    def test_native_navigation_and_existing_preferences_preserved(self):
        path='mod/app/src/main/java/bttv/settings/SettingsNavigation.java'
        current=read(path)
        # B2 intentionally retires the toolbar entry and redesigns only the BTTV page.
        retired=current[current.index('    public static void install('):]
        self.assertNotIn('menu.add(',retired)
        self.assertNotIn('menu.remove',retired)
        for path in ('mod/app/src/main/java/bttv/settings/Settings.java',
                     'mod/app/src/main/java/bttv/settings/UserPreferences.java',
                     'patches/AndroidManifest.xml.patch',
                     'patches/settings/tv.twitch.android.settings.SettingsActivity.smali.patch'):
            old=subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT).decode().replace('\r\n','\n')
            self.assertEqual(read(path),old,path)
        self.assertNotIn('FLAG_ACTIVITY_NEW_TASK',current)
        self.assertIn('new Intent(activity, SettingsActivity.class)',current)

    def test_label_and_native_layout_and_stock_destinations(self):
        strings=ET.parse(ROOT/'mod/app/src/main/res/values/strings.xml')
        self.assertEqual(strings.find("string[@name='bttv_settings_entry_title']").text,'BTTV Settings')
        holder=(DECODE/'smali_classes6/c120.smali').read_text()
        self.assertIn('const p0, 0x7f0e048a',holder)
        self.assertIn('name="sub_menu_recycler_item" id="0x7f0e048a"', (DECODE/'res/values/public.xml').read_text())
        self.assertIn('name="ic_settings" id="0x7f080646"',(DECODE/'res/values/public.xml').read_text())
        # Three cross-DEX calls only. No hooks into near-full DEX1/2 or Chat Settings.
        for name in ('cl2','ffo','r2k'):
            self.assertIn('a/smali_classes5/',read(f'patches/live/{name}.smali.patch'))


if __name__ == '__main__': unittest.main()
