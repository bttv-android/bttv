"""Stage 5Q-D: normal patch application and narrow emote-card integration contracts."""
import importlib.util
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
NEW = {
    'mod/app/src/main/java/bttv/emote/EmoteDetails.java',
    'patches/live/bttvcompat.EmoteDetailsBridge.smali.patch',
    'patches/live/zz_emote_details.patch',
    'tests/stage5q_emote_details.py',
    'tests/stage5q_emote_details_host.py',
}


def read(path):
    return (ROOT / path).read_text(encoding='utf-8')


def method(source, signature):
    start = source.index('.method ' + signature)
    return source[start:source.index('.end method', start) + len('.end method')]


def patched_fixture(destination, details=True):
    spec = importlib.util.spec_from_file_location('production', ROOT / 'tools/stage_production_input.py')
    production = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(production)
    paths = set(production.production_paths(ROOT))
    if details:
        paths.update(NEW)
    paths = sorted((p for p in paths if p.startswith('patches/') and p.endswith('.patch')
                    and (details or p not in NEW)), key=lambda p: (p.count('/'), p))
    for patch in paths:
        for line in read(patch).splitlines():
            if line.startswith('--- a/'):
                name = line[6:]
            elif line.startswith('rename from '):
                name = line[12:]
            else:
                continue
            source = ROOT / '.bttv-cache/disass' / name
            if name == 'smali_classes7/tv/twitch/android/settings/SettingsActivity.smali':
                source = ROOT / '.bttv-cache/disass/smali_classes5/tv/twitch/android/settings/SettingsActivity.smali'
            target = destination / name
            if source.exists() and not target.exists():
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(source.read_text(encoding='utf-8'), encoding='utf-8', newline='\n')
    subprocess.run(['git', 'init', '--quiet', str(destination)], check=True)
    subprocess.run(['git', '-c', 'core.autocrlf=false', 'add', '--all'], cwd=destination, check=True)
    for path in paths:
        for flags in (['--check', '--index'], ['--index']):
            subprocess.run(['git', '-c', 'core.autocrlf=false', 'apply', *flags, str(ROOT / path)],
                           cwd=destination, check=True, capture_output=True)
    return paths


class EmoteDetailsTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        base = ROOT / '.tmp/stage5qd'
        base.mkdir(parents=True, exist_ok=True)
        cls.temp = tempfile.TemporaryDirectory(prefix='contracts-', dir=base)
        cls.path = Path(cls.temp.name)
        cls.current, cls.stock = cls.path / 'current', cls.path / 'baseline'
        cls.current.mkdir(); cls.stock.mkdir()
        cls.patches = patched_fixture(cls.current)
        patched_fixture(cls.stock, details=False)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def file(self, name):
        return (self.current / name).read_text()

    def test_order_and_syntax(self):
        names = self.patches
        self.assertEqual(len(names), 51)
        self.assertLess(names.index('patches/live/zy_gif_modes.patch'), names.index('patches/live/zz_emote_details.patch'))
        self.assertLess(names.index('patches/live/zz_emote_details.patch'), names.index('patches/live/zz_f86_relocation.patch'))
        for path in NEW:
            if path.endswith('.patch'):
                subprocess.run(['git', 'apply', '--numstat', str(ROOT / path)], check=True, stdout=subprocess.DEVNULL)
        self.assertFalse((self.current / 'smali_classes2/f86.smali').exists())
        self.assertTrue((self.current / 'smali_classes7/f86.smali').is_file())

    def test_exact_additive_stage_scope(self):
        names = set(subprocess.check_output(['git', 'diff', '--name-only',
                    'f4f621ae14d990db0bc19fb1f18c8a246a2cf134'], cwd=ROOT, text=True).splitlines())
        self.assertEqual(names, NEW)
        for name in NEW:
            self.assertNotEqual(subprocess.run(['git', 'cat-file', '-e',
                                'f4f621ae14d990db0bc19fb1f18c8a246a2cf134:' + name],
                                cwd=ROOT, capture_output=True).returncode, 0)

    def test_only_common_click_hook_changes(self):
        before = (self.stock / 'smali_classes7/f86.smali').read_text()
        after = self.file('smali_classes7/f86.smali')
        capture = '    iget-object v7, p0, Lf86;->a:Lqz0;\n'
        call = '    invoke-static {p4, p1, v7}, Lbttvcompat/EmoteDetailsBridge;->wrap(Lfmp;Landroid/text/style/ClickableSpan;Lqz0;)Landroid/text/style/ClickableSpan;\n    move-result-object p1\n'
        self.assertEqual(after.count(capture), 1)
        self.assertEqual(after.count(call), 1)
        self.assertEqual(after.replace('    .locals 8\n\n' + capture, '    .locals 7\n', 1).replace('\n' + call, ''), before)
        d = method(after, 'public final d(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Lfmp;ZLjava/lang/Integer;Ltv/twitch/android/core/mvp/viewdelegate/EventDispatcher;)Landroid/text/SpannableString;')
        self.assertEqual(re.findall(r'\bv7\b', d), ['v7', 'v7'])
        self.assertIn('invoke-direct {p1, p6, p2, p3, p7}, Lz97;-><init>', d)
        self.assertIn('const/16 p5, 0x21', d)
        self.assertIn('setSpan(Ljava/lang/Object;III)V', d)
        self.assertIn('move-object/from16 v2, p5', after)
        self.assertIn('GifBridge;->bind(Lr360;Lqz0;Ljava/lang/String;)V', d)

    def test_native_identity_and_reserved_scope(self):
        bridge = self.file('smali_classes7/bttvcompat/EmoteDetailsBridge.smali')
        wrap = method(bridge, 'public static wrap(Lfmp;Landroid/text/style/ClickableSpan;Lqz0;)Landroid/text/style/ClickableSpan;')
        self.assertIn('if-nez v1, :synthetic\n    return-object p1', wrap)
        self.assertIn('EmoteDetails;->isSynthetic', wrap)
        self.assertIn('Lfmp;->b:Ljava/lang/String;', wrap)
        self.assertIn('new-instance v2, Lbttvcompat/EmoteDetailsClick;', wrap)
        click = self.file('smali_classes7/bttvcompat/EmoteDetailsClick.smali')
        self.assertNotIn('Lz97;', click)
        self.assertNotRegex(click, 'EventDispatcher|Lj16;|Lllf;|EmoteCardDialogFragment|->onClick')
        self.assertIn('bttv31://', read('mod/app/src/main/java/bttv/emote/EmoteDetails.java'))
        self.assertNotRegex(bridge, r'bttv31pk|bttv31ac|MovementMethod|setOnLongClickListener')

    def test_native_dexes_and_accepted_hooks_unchanged(self):
        for path in self.stock.rglob('*'):
            if not path.is_file() or '.git' in path.parts:
                continue
            name = path.relative_to(self.stock)
            if name.as_posix() == 'smali_classes7/f86.smali':
                continue
            self.assertEqual(path.read_bytes(), (self.current / name).read_bytes(), str(name))
        z86 = self.file('smali_classes2/z86.smali')
        self.assertIn('.locals 27', z86)
        self.assertIn('move-object/from16 v24, v4', z86)
        self.assertIn('move-object/from16 v25, p1', z86)

    def test_lifecycle_and_recreation(self):
        bridge = self.file('smali_classes7/bttvcompat/EmoteDetailsBridge.smali')
        for guard in ('isAttachedToWindow()Z', 'isShown()Z', 'myLooper()', 'getMainLooper()',
                      'ContextWrapper;->getBaseContext()', 'isFinishing()Z', 'isDestroyed()Z',
                      '->u()Lx9i;', '->W()Z', '->L:Z', '->H(Ljava/lang/String;)', '->showNow('):
            self.assertIn(guard, bridge)
        self.assertLess(bridge.index('->H(Ljava/lang/String;)'), bridge.index('->showNow('))
        self.assertIn('if-eq v0, v1, :done', bridge)
        self.assertIn('const/16 v2, 0x10', bridge)
        sheet = self.file('smali_classes7/bttvcompat/EmoteDetailsSheet.smali')
        self.assertIn('.super Ltv/twitch/android/core/fragments/bottomsheet/TwitchBottomSheetDialogFragment;', sheet)
        self.assertIn('->requireArguments()', sheet)
        self.assertIn('getBoolean(Ljava/lang/String;Z)Z', sheet)
        self.assertIn('->isChangingConfigurations()Z', sheet)
        self.assertIn('->dismissAllowingStateLoss()V', sheet)
        for sig in ('public final onDestroyView()V', 'public final onDismiss(Landroid/content/DialogInterface;)V'):
            self.assertIn('->clearImage()V', method(sheet, sig))
        for source in (bridge, sheet, self.file('smali_classes7/bttvcompat/EmoteDetailsClick.smali')):
            self.assertNotRegex(source, r'\.field[^\n]*static[^\n]*(Activity|View;|Dialog|Context;|Fragment)')
            self.assertNotRegex(source, r'postDelayed|Timer;|->post\(')

    def test_reuses_native_styles_without_fake_actions(self):
        sheet = self.file('smali_classes7/bttvcompat/EmoteDetailsSheet.smali')
        for rid in ('0x7f0e01ac', '0x7f0b07c8', '0x7f0b07bb', '0x7f0b07c2', '0x7f070160', '0x7f0701c5'):
            self.assertIn(rid, sheet)
            self.assertIn('id="' + rid + '"', read('.bttv-cache/disass/res/values/public.xml'))
        self.assertIn('removeAllViews()V', sheet)
        self.assertIn('setAccessibilityHeading(Z)V', sheet)
        self.assertIn('setContentDescription(', sheet)
        self.assertNotRegex(sheet, r'Lymf;|Lksd;|Lkkf;|ChannelModel|EmoteCardTrackingMetadata')
        bridge = self.file('smali_classes7/bttvcompat/EmoteDetailsBridge.smali')
        self.assertIn('setTextIsSelectable(Z)V', bridge)
        self.assertIn('setMaxLines(I)V', bridge)
        self.assertIn('setEllipsize(', bridge)

    def test_gif_request_and_cleanup(self):
        bridge = self.file('smali_classes7/bttvcompat/EmoteDetailsBridge.smali')
        image = method(bridge, 'public static image(Ltv/twitch/android/shared/ui/elements/image/NetworkImageWidget;Ljava/lang/String;Z)V')
        self.assertIn('const-class v1, Landroid/graphics/drawable/Drawable;', image)
        self.assertIn('GifBridge;->configure(Li0x;Z)Li0x;', image)
        self.assertLess(image.index('GifBridge;->configure'), image.index('->I(Ljava/lang/Object;)'))
        self.assertIn('->G(Landroid/widget/ImageView;)V', image)
        self.assertNotRegex(image, r'Lqz0;->|stop\(\)')
        clear = method(bridge, 'public static clear(Landroid/widget/ImageView;)V')
        for part in ('Animatable;->stop()V', 'Drawable;->setCallback(', 'Ll1x;->c(Lz430;)V', 'setImageDrawable('):
            self.assertIn(part, clear)
        self.assertIn('getApplicationContext()', clear)
        self.assertIn('new-instance v0, Lwte;', clear)

    def test_no_remote_requests_traces_or_scope_expansion(self):
        for name in ('EmoteDetailsBridge', 'EmoteDetailsClick', 'EmoteDetailsSheet'):
            code = self.file(f'smali_classes7/bttvcompat/{name}.smali')
            self.assertNotRegex(code, r'Log;|BTTV31TRACE|BTTV31PUSH|HttpURLConnection|OkHttp|Lllf;|Lxjf;|Lz86;')
        java = read('mod/app/src/main/java/bttv/emote/EmoteDetails.java')
        self.assertNotRegex(java, r'openConnection|\.fetch\(|\.load\(|ensureChannel|\.put\(|currentBroadcaster|Log\.')
        self.assertNotIn('getEmoteById', java)
        self.assertIn('data.id.equals(entry.id)', java)
        self.assertIn('data.code.equals(entry.code)', java)
        self.assertIn('data.url.equals(entry.url)', java)
        for path in NEW:
            self.assertTrue((ROOT / path).is_file(), path)


if __name__ == '__main__':
    unittest.main()
