"""Stage 5Q-C: original GIF policy, exact synthetic boundaries and patch preflight."""
import ast
import hashlib
import importlib.util
from pathlib import Path
import re
import struct
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PATCH = 'patches/live/zy_gif_modes.patch'
POLICY = 'mod/app/src/main/java/bttv/emote/GifPolicy.java'
BASE = 'add9c08557bb6c0c47afaf0e41431d702edc1969'

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def production_patches():
    spec = importlib.util.spec_from_file_location('production', ROOT / 'tools/stage_production_input.py')
    helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(helper)
    paths = set(helper.production_paths(ROOT)) | {PATCH}
    return sorted((p for p in paths if p.startswith('patches/') and p.endswith('.patch')),
                  key=lambda p: (p.count('/'), p))

def patched_fixture(destination):
    patches = production_patches()
    for patch in patches:
        for line in read(patch).splitlines():
            if line.startswith('--- a/'):
                relative = line[6:]
            elif line.startswith('rename from '):
                relative = line[12:]
            else:
                continue
            source = ROOT / '.bttv-cache/disass' / relative
            if relative == 'smali_classes7/tv/twitch/android/settings/SettingsActivity.smali':
                source = ROOT / '.bttv-cache/disass/smali_classes5/tv/twitch/android/settings/SettingsActivity.smali'
            target = destination / relative
            if not target.exists() and source.exists():
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(source.read_text(), encoding='utf-8', newline='\n')
    subprocess.run(['git', 'init', '--quiet', str(destination)], check=True)
    subprocess.run(['git', '-c', 'core.autocrlf=false', 'add', '--all'], cwd=destination, check=True)
    for patch in patches:
        for options in (['--check', '--index'], ['--index']):
            subprocess.run(['git', '-c', 'core.autocrlf=false', 'apply', *options, str(ROOT / patch)],
                           cwd=destination, check=True, capture_output=True)
    return len(patches)

def method(source, signature):
    start = source.index('.method ' + signature)
    return source[start:source.index('.end method', start) + len('.end method')]

class GifModeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory(prefix='gif-validation-', dir=ROOT / '.tmp')
        cls.fixture = Path(cls.temporary.name)
        cls.patch_count = patched_fixture(cls.fixture)
        cls.bridge = (cls.fixture / 'smali_classes7/bttvcompat/GifBridge.smali').read_text()

    @classmethod
    def tearDownClass(cls):
        cls.temporary.cleanup()

    def file(self, path):
        return (self.fixture / path).read_text()

    def test_original_policy_not_once(self):
        original = subprocess.check_output(['git', 'show', 'master:mod/app/src/main/java/bttv/emote/Glide.java'], cwd=ROOT, text=True)
        self.assertIn('endsWith("?static=true")', original)
        self.assertIn('bttv_settings_gif_render_mode_animate_forever', original)
        self.assertNotIn('setLoopCount', original)
        current = read(POLICY)
        self.assertIn('ResUtil.getSelectedDropdownFromSettings(Settings.GifRenderMode)', current)
        self.assertIn('&& !nativeStatic', current)
        self.assertNotIn('getString(', current)
        preferences = read('mod/app/src/main/java/bttv/settings/UserPreferences.java')
        self.assertIn('defValue.getSelected().name()', preferences)
        self.assertIn('Res.strings.valueOf(selected)', preferences)

    def test_existing_production_files_unchanged(self):
        changed = subprocess.check_output(['git', 'diff', '--name-only', BASE], cwd=ROOT, text=True).splitlines()
        additions = {POLICY, PATCH, 'tests/stage5q_gif_modes.py', 'tests/stage5q_gif_modes_host.py'}
        self.assertTrue(set(changed) <= additions, 'Existing feature source must not change')
        for path in changed:
            self.assertNotEqual(subprocess.run(['git', 'cat-file', '-e', BASE + ':' + path], cwd=ROOT,
                                              capture_output=True).returncode, 0)
        self.assertEqual(self.patch_count, 49)
        patches = production_patches()
        self.assertLess(patches.index(PATCH), patches.index('patches/live/zz_f86_relocation.patch'))
        self.assertGreater(patches.index(PATCH), patches.index('patches/live/z86.smali.patch'))

    def test_synthetic_chat_scope_and_native_setting(self):
        bind = method(self.bridge, 'public static bind(Lr360;Lqz0;Ljava/lang/String;)V')
        self.assertLess(bind.index('startsWith'), bind.index('iput-boolean'))
        self.assertIn('"bttv31://"', bind)
        self.assertIn('Lqz0;->a:Len6;', self.bridge)
        self.assertIn('Len6;->p()Z', self.bridge)
        self.assertIn('xor-int/lit8 v0, v0, 0x1', self.bridge)
        span = self.file('smali_classes2/r360.smali')
        self.assertEqual(span.count('.field public bttvThirdParty:Z'), 1)
        self.assertEqual(span.count('.field public bttvNativeStatic:Z'), 1)
        target = self.file('smali_classes2/q9j.smali')
        self.assertIn('Animatable;->start()V', target)
        self.assertEqual(target.count('Animatable;->stop()V'), 2)
        self.assertEqual(target.count('setCallback'), 3)

    def test_static_is_worker_bitmap_decode_not_stop_after_start(self):
        configure = method(self.bridge, 'public static configure(Li0x;Z)Li0x;')
        self.assertIn('GifPolicy;->shouldAnimate(Z)Z', configure)
        self.assertIn('if-nez v0, :done', configure)
        self.assertIn('const-class v0, Landroid/graphics/Bitmap;', configure)
        self.assertIn('Lzk2;->d(Ljava/lang/Class;)Lzk2;', configure)
        self.assertNotRegex(configure, r'->(?:start|stop|draw|decodeBitmap|decodeStream)\(')
        self.assertNotRegex(self.bridge, r'https?://|\?static=true|BitmapFactory|new-instance.*(?:Cache|Thread|Executor)')
        # Real Twitch asBitmap uses exactly this decode-class option.
        native = read('.bttv-cache/disass/smali/l1x.smali')
        init = native[native.index('.method static constructor'):native.index('.end method')]
        self.assertIn('Landroid/graphics/Bitmap;', init)
        self.assertIn('Lzk2;->d(Ljava/lang/Class;)Lzk2;', init)

    def test_chat_preserves_tokens_badges_style_and_bounds(self):
        f86 = self.file('smali_classes7/f86.smali')
        self.assertIn('move-object/from16 v2, p5', f86)
        self.assertIn('LiveHighlightBridge;->replace', f86)
        self.assertIn('LiveEmotes;->urlForMarker', f86)
        self.assertEqual(f86.count('GifBridge;->bind('), 1)
        hook = '    iget-object v3, p0, Lf86;->a:Lqz0;\n    invoke-static {v2, v3, v1}, Lbttvcompat/GifBridge;->bind(Lr360;Lqz0;Ljava/lang/String;)V'
        self.assertIn(hook, f86)
        # v4 remains the URL argument to f86.f's v1..v6 range invocation.
        # v3 is overwritten on BOTH successors before any later read.
        self.assertNotIn('iget-object v4, p0, Lf86;->a:Lqz0;', f86)
        tail = f86.split(hook, 1)[1].split('.end method', 1)[0]
        self.assertIn('iget-object v3, p4, Lfmp;->b:Ljava/lang/String;', tail)
        self.assertIn('new-instance v3, Ldu4;', tail)
        self.assertIn('invoke-static/range {v1 .. v6}, Lf86;->f', tail)
        aaj = self.file('smali_classes2/aaj.smali')
        self.assertEqual(aaj.count('GifBridge;->request('), 1)
        self.assertLess(aaj.index('GifBridge;->request('), aaj.index('Li0x;->I(Ljava/lang/Object;)Li0x;', aaj.index('GifBridge;->request(')))
        self.assertIn('Lq9j;->e:Landroid/graphics/drawable/Drawable$Callback;', aaj)
        z86 = self.file('smali_classes2/z86.smali')
        self.assertIn('.locals 27', z86)

    def test_picker_keeps_identity_and_stock_recycling(self):
        bridge = self.file('smali_classes7/bttvcompat/NativePickerBridge.smali')
        image = method(bridge, 'public static pickerImage(Ltv/twitch/android/shared/ui/elements/image/NetworkImageWidget;Ljava/lang/Object;Ljava/lang/String;Lqz0;)Z')
        self.assertIn('"bttv31pk:"', image)
        self.assertIn('GifBridge;->image(', image)
        self.assertIn('Ll1x;->c(Lz430;)V', image)
        self.assertIn('setImageDrawable', image)
        self.assertIn(':stock\n    const/4 v0, 0x0', image)
        self.assertIn('{v13, v7, v14, v10}', self.file('smali_classes5/wjf.smali'))
        self.assertEqual(self.bridge.count('Li0x;->I('), 1)
        self.assertEqual(self.bridge.count('Li0x;->G('), 1)

    def test_autocomplete_uses_validated_identity_no_ranking_changes(self):
        qt1 = self.file('smali_classes2/qt1.smali')
        self.assertIn('invoke-static {p1, v2, p0}, Lbttvcompat/GifBridge;->autocomplete', qt1)
        self.assertIn('if-nez v4, :bttv_ac_image_done', qt1)
        self.assertIn('Lqz0;->c(', qt1)
        self.assertIn('NetworkImageWidget;->setImageURL(', qt1)
        self.assertIn('LiveEmoteAutocomplete;->resolveNativeImageUrl', self.bridge)

    def test_no_new_traces_http_or_global_native_hook(self):
        patch = read(PATCH)
        self.assertNotRegex(patch, r'Landroid/util/Log;|BTTV31TRACE|BTTV31PUSH|HttpURLConnection|OkHttp')
        self.assertNotIn('diff --git a/smali_classes2/qz0.smali', patch)
        self.assertNotIn('diff --git a/smali_classes2/z86.smali', patch)
        self.assertNotIn('getBounds', patch)
        subprocess.run(['git', 'apply', '--numstat', str(ROOT / PATCH)], check=True, stdout=subprocess.DEVNULL)

if __name__ == '__main__':
    unittest.main()
