"""Run with Python 3 from the repository; no device/build/network access."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASE = '8ee95b6a5b7dcc4b3002bb2549388e30c561f4db'


def read(path):
    return (ROOT / path).read_text(encoding='utf-8').replace('\r\n', '\n')


def baseline(path):
    return subprocess.check_output(['git', 'show', f'{BASE}:{path}'], cwd=ROOT).decode().replace('\r\n', '\n')


def added_patch(path):
    return '\n'.join(line[1:] for line in read(path).splitlines() if line.startswith('+') and not line.startswith('+++'))


class PickerStaticTests(unittest.TestCase):
    def test_four_caller_abis_only(self):
        expected = {
            'lpf': ['wrap(Ljava/lang/Object;Lio/reactivex/f;)Lio/reactivex/f;'],
            'blp': ['pickerInsert(Ljava/lang/Object;Ljava/lang/String;)Z'],
            'wjf': ['pickerUrl(Ljava/lang/Object;)Ljava/lang/String;',
                    'pickerImage(Ltv/twitch/android/shared/ui/elements/image/NetworkImageWidget;Ljava/lang/Object;Ljava/lang/String;)Z'],
            'b6b': ['pickerClick(Ljava/lang/Object;Ljava/lang/Object;)Z'],
        }
        for name, abis in expected.items():
            text = added_patch(f'patches/live/{name}.smali.patch')
            calls = re.findall(r'Lbttvcompat/NativePickerBridge;->([^\s]+)', text)
            self.assertEqual(calls, abis)
            self.assertNotIn('.locals', text)
            self.assertNotIn('.registers', text)

    def test_post_scheduler_hook_and_stock_fallback(self):
        text = read('patches/live/lpf.smali.patch')
        self.assertLess(text.index('RxHelperKt;->b'), text.index('NativePickerBridge;->wrap'))
        self.assertIn('move-result-object p1', text)
        self.assertIn('.catch Ljava/lang/Throwable;', text)
        self.assertNotRegex(text, r'(?m)^-(?!--)(?=.*\S).+$')
        self.assertIn('const/16 v1, 0x9', read('patches/live/bttvcompat.NativePickerBridge.smali.patch'))

    def test_stock_binder_and_click_branches_not_replaced(self):
        for path in ('wjf', 'b6b', 'blp'):
            self.assertNotRegex(read(f'patches/live/{path}.smali.patch'), r'(?m)^-(?!--)(?=.*\S).+$')
        self.assertIn('if-nez v14, :bttv_picker_image_ready', added_patch('patches/live/wjf.smali.patch'))
        self.assertIn('if-nez v2, :bttv_picker_image_done', added_patch('patches/live/wjf.smali.patch'))
        self.assertIn('if-nez v3, :goto_6', added_patch('patches/live/b6b.smali.patch'))
        self.assertIn('if-eqz v0, :bttv_picker_native_insert', added_patch('patches/live/blp.smali.patch'))
        bridge = read('patches/live/bttvcompat.NativePickerBridge.smali.patch')
        self.assertNotIn('Ltqf;', bridge)
        self.assertIn('Lyof;-><init>(Lqnf;)V', bridge)
        self.assertIn('sget-object v1, Lunf;->ANIMATED:Lunf;', bridge)
        self.assertIn('Lcom/bumptech/glide/a;->e(Landroid/content/Context;)Ll1x;', bridge)
        self.assertIn('Li0x;->G(Landroid/widget/ImageView;)V', bridge)

    def test_existing_C2_patch_kept_verbatim(self):
        path='patches/live/blp.smali.patch'
        self.assertTrue(read(path).startswith(baseline(path)))
        for path in ('patches/live/ot1.smali.patch', 'patches/live/qt1.smali.patch',
                     'patches/live/bttvcompat.NativeAutocompleteBridge.smali.patch'):
            self.assertEqual(read(path),baseline(path))

    def test_C2_behavior_methods_unchanged(self):
        path='mod/app/src/main/java/bttv/emote/LiveEmoteAutocomplete.java'
        old,new=baseline(path),read(path)
        # Remove just the new read-only API block and its two observer calls.
        start=new.index('    // Read-only picker lifecycle surface')
        end=new.index('    private static boolean mainThread()',start)
        new=new[:start]+new[end:]
        new=re.sub(r'(?m)^ +notifyLifecycle\(\);\n', '', new)
        self.assertEqual(old,new)

    def test_no_network_prefix_query_or_send(self):
        files=['LiveEmotePicker','PickerRuntime','PickerInsertion','PickerSyntheticId','EmoteCatalogEvents']
        text='\n'.join(read(f'mod/app/src/main/java/bttv/emote/{name}.java') for name in files)
        for forbidden in ('HttpURLConnection','okhttp','bttv.Network','currentBroadcasterId',
                          'Emotes.loadEmotes','EmoteAutocompleteCatalog.find','performEditorAction',
                          'dispatchKeyEvent','sendKeyEvent','PopupWindow','TextWatcher'):
            self.assertNotIn(forbidden,text)
        self.assertIn('Editable',text)
        self.assertIn('text.replace(plan.start, plan.end, plan.replacement)',text)

    def test_no_unrelated_production_changes(self):
        paths=['build','buildsource','disassemble','Dockerfile','patch','initworkspace',
               'patches/AndroidManifest.xml.patch','patches/live/f86.smali.patch',
               'patches/live/z86.smali.patch','mod/app/src/main/java/bttv/emote/LiveEmotes.java',
               'mod/app/src/main/java/bttv/emote/LiveEmoteGlobals.java',
               'mod/app/src/main/java/bttv/emote/EmoteAutocompleteLogic.java',
               'mod/app/src/main/java/bttv/emote/EmoteAutocompleteCatalog.java']
        paths.remove('mod/app/src/main/java/bttv/emote/LiveEmotes.java')
        for path in paths:
            if (ROOT/path).exists(): self.assertEqual(read(path),baseline(path),path)
        self.assertFalse(list((ROOT/'patches').rglob('c3-*.patch')))
        for path in (ROOT/'patches').rglob('*.patch'):
            self.assertNotIn('EmoteCardTrace',read(path.relative_to(ROOT)))

    def test_chat_animation_is_third_party_only(self):
        path = 'mod/app/src/main/java/bttv/emote/LiveEmotes.java'
        old, new = baseline(path), read(path)
        start = new.index('    /** True only for URLs emitted by the bttv31 live-chat marker bypass. */')
        end = new.index('    private static synchronized void requestGlobals()', start)
        self.assertEqual(old, new[:start] + new[end:])
        for prefix in ('https://cdn.betterttv.net/', 'https://cdn.frankerfacez.com/',
                       'https://cdn.7tv.app/'):
            self.assertIn(prefix, new[start:end])
        target = added_patch('patches/live/q9j.smali.patch')
        self.assertEqual(target.count('LiveEmotes;->isThirdPartyUrl'), 2)
        self.assertEqual(target.count('Animatable;->start()V'), 1)
        self.assertEqual(target.count('Animatable;->stop()V'), 2)
        self.assertEqual(target.count('setCallback'), 3)
        self.assertNotIn('bttv31pk:', target)

    def test_lifecycle_and_weak_pending_guards(self):
        runtime=read('mod/app/src/main/java/bttv/emote/PickerRuntime.java')
        self.assertIn('session.lifecycle.matches(input)',runtime)
        self.assertIn('uniqueLifecycle(channel)',runtime)
        self.assertIn('NativePickerBridge.subscribe(upstream, emitter, stream)',runtime)
        self.assertIn('output = augment(session, original)',runtime)
        self.assertIn('stopListeners()',runtime)
        identity=read('mod/app/src/main/java/bttv/emote/PickerInsertion.java')
        self.assertIn('get() == ((Key) other).get()',identity)
        self.assertIn('ReferenceQueue<String>',identity)
        self.assertIn('new String(canonicalCode)',identity)
        self.assertIn('WeakReference<T> owner',identity)

    def test_publication_outside_channel_lock(self):
        text=read('mod/app/src/main/java/bttv/emote/LiveEmoteChannels.java')
        self.assertIn('request.loading = false;\n                    }\n                    EmoteCatalogEvents.published',text)
        events=read('mod/app/src/main/java/bttv/emote/EmoteCatalogEvents.java')
        self.assertIn('catch (Throwable ignored)',events)


if __name__ == '__main__': unittest.main(verbosity=2)
