"""Stage 5Q-F source scope, native badge preservation and privacy contracts."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASE = '2ffc408cbab9fcd73d0801196eb40e6e923d0818'
NEW = {'mod/app/src/main/java/bttv/badge/BadgeCatalog.java',
       'mod/app/src/main/java/bttv/badge/BadgeImages.java',
       'patches/live/bttvcompat.LiveBadgeBridge.smali.patch',
       'tests/stage5q_badges.py', 'tests/stage5q_badges_host.py'}
MODIFIED = {'mod/app/src/main/java/bttv/Data.java', 'patches/live/f86.smali.patch'}

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def before(path):
    return subprocess.check_output(['git', 'show', BASE + ':' + path], cwd=ROOT).decode().replace('\r\n', '\n')

class BadgeTests(unittest.TestCase):
    def test_exact_production_scope(self):
        changed = set(subprocess.check_output(['git', 'diff', '--name-only', BASE], cwd=ROOT, text=True).splitlines())
        self.assertEqual(changed, MODIFIED)
        unknown = set(subprocess.check_output(['git', 'ls-files', '--others', '--exclude-standard'], cwd=ROOT, text=True).splitlines())
        self.assertEqual(unknown - NEW, {p for p in unknown if '/notifications/' in p or '/notifications_diag/' in p
                         or '/notifications_reason/' in p or p.startswith('tests/stage5m_fis403')
                         or p.startswith('tests/stage5m_notifications')})
        self.assertEqual(len(unknown - NEW), 18)
        self.assertEqual(unknown & NEW, NEW)

    def test_data_only_starts_isolated_loader(self):
        line = '        bttv.badge.BadgeImages.initialize(Data.ctx);\n'
        self.assertEqual(read('mod/app/src/main/java/bttv/Data.java'),
                         before('mod/app/src/main/java/bttv/Data.java').replace(
                             '        Data.ctx = applicationContext == null ? ctx : applicationContext;\n',
                             '        Data.ctx = applicationContext == null ? ctx : applicationContext;\n' + line))

    def test_f86_only_badge_run_changed(self):
        text = read('patches/live/f86.smali.patch')
        self.assertEqual(text.count('LiveBadgeBridge;->augment('), 1)
        self.assertIn('iget-object v14, v10, Ly86;->a:Ljava/lang/String;', text)
        self.assertIn('invoke-static {v1, v0, v14}', text)
        self.assertIn('move-object/from16 v2, p5', text)
        self.assertNotIn('move-object/from16 v2, p2', text)
        # Removing exactly the new hunk restores every prior instruction unchanged.
        without = re.sub(r'@@ -734,6 \+744,12 @@\n.*?(?=@@ -3352)', '', text, flags=re.S)
        without = without.replace('@@ -3352,8 +3368,13 @@', '@@ -3352,8 +3352,13 @@')
        without = without.replace('@@ -3363,8 +3384,10 @@', '@@ -3363,8 +3368,10 @@')
        self.assertEqual(without, before('patches/live/f86.smali.patch'))

    def test_bridge_preserves_native_spans_and_uses_native_metrics(self):
        text = read('patches/live/bttvcompat.LiveBadgeBridge.smali.patch')
        for required in ('Landroid/text/SpannableStringBuilder;-><init>(Ljava/lang/CharSequence;)V',
                         'Laaj;->g(Landroid/content/Context;Lr360;)I', 'new-instance v4, Ldu4;',
                         'Landroid/text/style/ImageSpan;-><init>(Landroid/graphics/drawable/Drawable;)V',
                         'new-instance v0, Landroid/graphics/drawable/BitmapDrawable;',
                         'BadgeImages$Marker;', 'return-object p1', '.catch Ljava/lang/Throwable;'):
            self.assertIn(required, text)
        self.assertLess(text.index('const-string v8, " "'), text.index('->setSpan('))
        for forbidden in ('->toString()', 'removeSpan', '->clear(', 'Lwhp;', 'Lqz0;',
                          'Lbttv/Network;', 'Lfmp;', 'bttv31://', 'EventDispatcher', 'Ljava/net/'):
            self.assertNotIn(forbidden, text)

    def test_catalog_is_numeric_global_and_additive(self):
        text = read('mod/app/src/main/java/bttv/badge/BadgeCatalog.java')
        for value in ('/3/cached/badges/twitch', '/v1/badges/ids', 'providerId', 'MAX_ASSIGNMENTS = 100_000',
                      'MAX_DEFINITIONS = 64', 'MAX_PER_AUTHOR = 8', 'Collections.unmodifiableMap',
                      'Collections.unmodifiableList', 'authorId', 'ff000000L'):
            self.assertIn(value, text)
        self.assertNotIn('getString("replaces")', text)
        for forbidden in ('currentBroadcasterId', 'getUsername', 'getDisplayName', 'Lbttv', 'okhttp', 'WebSocket'):
            self.assertNotIn(forbidden, text)

    def test_loader_is_bounded_private_and_not_bound_to_rows(self):
        text = read('mod/app/src/main/java/bttv/badge/BadgeImages.java')
        for required in ('setInstanceFollowRedirects(false)', 'setConnectTimeout(7_000)', 'setReadTimeout(10_000)',
                         'MAX_ASSET = 256 * 1024', 'MAX_DIMENSION = 256', 'PROVIDER_CACHE_BYTES = 2 * 1024 * 1024',
                         'workers.shutdown()', 'attempt < 3', 'EXPIRES_MS', 'generation != ticket',
                         'new ArrayBlockingQueue<Runnable>(2)', 'target.copy(Bitmap.Config.ARGB_8888, false)'):
            self.assertIn(required, text)
        lookup = text.split('public static List<ReadyBadge> ready(', 1)[1].split('static Response fetch(', 1)[0]
        for forbidden in ('fetch(', 'load(', 'initialize(', 'openConnection', 'Thread.sleep'):
            self.assertNotIn(forbidden, lookup)
        for forbidden in ('Log.', 'System.out', 'printStackTrace', 'TextView', 'ViewHolder', 'Activity',
                          'SharedPreferences', 'okhttp', 'scheduleAtFixedRate', 'Authorization', 'Cookie'):
            self.assertNotIn(forbidden, text)

    def test_svg_is_sanitized_before_androidsvg(self):
        text = read('mod/app/src/main/java/bttv/badge/BadgeImages.java')
        self.assertIn('SVG.getFromString(safeSvg(bytes))', text)
        self.assertIn('Xml.newPullParser()', text)
        self.assertIn('xml.contains("<!")', text)
        self.assertIn('xml.contains("<?")', text)
        self.assertIn('xml.indexOf(\'&\')', text)
        self.assertIn('parser.getAttributeNamespace', text)
        self.assertIn('pathChars > 24_000', text)
        self.assertIn('canvas.drawRoundRect', text)
        self.assertNotIn('Glide', text)

    def test_no_native_dex_hooks_or_notification_inputs(self):
        for path in ('patches/live/z86.smali.patch', 'patches/live/aaj.smali.patch',
                     'patches/live/q9j.smali.patch', 'patches/live/zz_f86_relocation.patch',
                     'patches/live/zy_gif_modes.patch', 'patches/live/zz_emote_details.patch',
                     'mod/app/src/main/java/bttv/Badges.java', 'mod/app/src/main/java/bttv/AnonChat.java',
                     'tools/stage_production_input.py', 'patches/AndroidManifest.xml.patch'):
            self.assertEqual(read(path), before(path), path)
        for path in ('patches/live/bttvcompat.LiveBadgeBridge.smali.patch', 'patches/live/f86.smali.patch'):
            subprocess.run(['git', 'apply', '--numstat', path], cwd=ROOT, check=True, capture_output=True)

if __name__ == '__main__':
    unittest.main()
