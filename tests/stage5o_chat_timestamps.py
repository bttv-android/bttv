"""Verify the actual Twitch 31 timestamp flag/format/span chain, not a mock mapper."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
DECODE = ROOT / '.bttv-cache/disass'
BASE = 'a82265242eb84ce538aeccedabaac378198d9f88'


def read(path):
    return (ROOT / path).read_text(encoding='utf-8')


class ChatTimestampTests(unittest.TestCase):
    def test_settings_default_persistence_and_ui(self):
        settings = read('mod/app/src/main/java/bttv/settings/Settings.java')
        entry = settings.split('AlwaysShowChatTimestamps(', 1)[1].split('\n    ),', 1)[0]
        self.assertIn('"bttv_always_show_chat_timestamps"', entry)
        self.assertIn('new Entry.BoolValue(false)', entry)
        self.assertIn('for (Settings setting : Settings.values())', read('mod/app/src/main/java/bttv/settings/SettingsActivity.java'))
        prefs = read('mod/app/src/main/java/bttv/settings/UserPreferences.java')
        self.assertIn('editor.putBoolean(key, value)', prefs)
        self.assertIn('editor.apply()', prefs)

    def test_live_and_loaded_message_callback(self):
        dispatch = (DECODE/'smali_classes2/d96.smali').read_text()
        self.assertIn('instance-of v2, v1, Ll26;', dispatch)
        self.assertIn('check-cast v6, Ll26;', dispatch)
        self.assertIn('check-cast v1, Lf26;', dispatch)
        self.assertIn('check-cast v6, Ll26;', dispatch.split('check-cast v1, Lf26;', 1)[1])
        self.assertGreaterEqual(dispatch.count('Lx46;->a(Ll26;ZLx86;ZLo66;'), 2)
        x46 = (DECODE/'smali_classes2/x46.smali').read_text()
        self.assertIn('Lv46;-><init>(Lkotlin/jvm/functions/Function1;Ln66;Lx46;Ll26;ZLx86;Z', x46)
        callback = (DECODE/'smali_classes2/v46.smali').read_text()
        self.assertIn('iput-boolean p7, p0, Lv46;->g:Z', callback)
        self.assertIn('invoke-static/range {v0 .. v9}, Lx46;->b(', callback)

    def test_native_seconds_format_style_and_single_span(self):
        transform = (DECODE/'smali_classes2/z86.smali').read_text()
        self.assertIn('Lm66;->getTimestampSeconds()J', transform)
        model = (DECODE/'smali_classes2/l26.smali').read_text()
        self.assertIn('iput-object p9, p0, Ll26;->i:Ljava/lang/Long;', model)
        boundary = (DECODE/'smali_classes2/x46.smali').read_text().split('.method public static b(', 1)[1].split('.end method', 1)[0]
        self.assertIn('iget-object v1, v0, Ll26;->i:Ljava/lang/Long;', boundary)
        self.assertIn('if-eqz p6, :cond_5', boundary)
        self.assertIn('move-object/from16 v18, v1', boundary)
        renderer = (DECODE/'smali_classes2/f86.smali').read_text()
        self.assertIn('const-string p2, "H:mm"', renderer)
        self.assertIn('if-eqz p14, :cond_6', renderer)
        self.assertIn('mul-long v6, v6, v16', renderer)
        self.assertIn('const-wide/16 v16, 0x3e8', renderer)
        self.assertIn('Ljava/util/Date;->setTime(J)V', renderer)
        self.assertIn('Ljava/text/DateFormat;->format(Ljava/util/Date;)', renderer)
        self.assertIn('const v6, 0x7f0606c2', renderer)
        self.assertIn('Landroid/text/style/ForegroundColorSpan;-><init>(I)V', renderer)
        # No new formatting or span code: the hook changes one boolean only.
        patch = read('patches/live/v46.smali.patch')
        added = [s[1:] for s in patch.splitlines() if s.startswith('+') and not s.startswith('+++')]
        self.assertEqual(added, ['    invoke-static {v6}, Lbttv/ChatTimestamps;->shouldShow(Z)Z', '    move-result v6', ''])

    def test_normal_application_relocation_and_register_types(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.tmp', prefix='stage5o-patch-') as folder:
            folder = Path(folder)
            (folder/'smali_classes2').mkdir()
            original = (DECODE/'smali_classes2/v46.smali').read_text()
            (folder/'smali_classes2/v46.smali').write_text(original, newline='\n')
            for name in ('v46.smali.patch', 'zz_v46_relocation.patch'):
                for args in (['--check'], []):
                    subprocess.run(['git', 'apply', *args, str(ROOT/'patches/live'/name)], cwd=folder, check=True,
                                   env={**os.environ, 'GIT_CEILING_DIRECTORIES': str(ROOT/'.tmp')})
            self.assertFalse((folder/'smali_classes2/v46.smali').exists())
            final = (folder/'smali_classes7/v46.smali').read_text()
            extra = '    invoke-static {v6}, Lbttv/ChatTimestamps;->shouldShow(Z)Z\n    move-result v6\n\n'
            self.assertEqual(final.count(extra), 1)
            self.assertEqual(final.replace(extra, ''), original)
            self.assertIn('.locals 10', final)
            self.assertIn('iget-boolean v6, p0, Lv46;->g:Z\n'+extra, final)

    def test_existing_features_and_hooks_unchanged(self):
        paths = ['patches/live/f86.smali.patch', 'patches/live/bttv.highlight.LiveHighlightBridge.smali.patch',
                 'patches/live/xdw.smali.patch', 'patches/live/jc2.smali.patch', 'patches/live/vw90.smali.patch',
                 'mod/app/src/main/java/bttv/AnonChat.java', 'mod/app/src/main/java/bttv/HideStories.java',
                 'mod/app/src/main/java/bttv/SplitChat.java', 'patches/AndroidManifest.xml.patch']
        for path in paths:
            old = subprocess.check_output(['git', 'show', BASE+':'+path], cwd=ROOT).decode().replace('\r\n', '\n')
            self.assertEqual(read(path), old, path)

    def test_helper_does_not_construct_text_or_change_models(self):
        helper = read('mod/app/src/main/java/bttv/ChatTimestamps.java')
        self.assertIn('if (stockVisible)', helper)
        self.assertIn('catch (Throwable ignored)', helper)
        for forbidden in ('Date', 'Span', 'List', 'Log.', 'Thread', 'Timer(', 'Http', 'setTime', 'rebind'):
            self.assertNotIn(forbidden, helper)


if __name__ == '__main__':
    unittest.main()
