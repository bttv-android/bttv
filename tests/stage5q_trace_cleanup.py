"""Stage 5Q-B source/patch equivalence checks; no device or APK build."""
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASE = '6b4da18df4a64b615413f8528dd129f3aea6e8af'
JAVA = 'mod/app/src/main/java/bttv/'
REMOVED = {f'patches/diagnostics/{name}.smali.patch' for name in ('z86', 'f86', 'qz0', 'aaj')}
NEW_TESTS = {'tests/stage5q_trace_cleanup.py', 'tests/stage5q_trace_cleanup_host.py'}
CHANGED = {JAVA + name for name in ('Data.java', 'emote/LiveEmotes.java',
    'emote/LiveEmoteChannels.java', 'emote/LiveEmoteAutocomplete.java',
    'highlight/StringSetUIAdapter.java')} | REMOVED | {'patches/live/z86.smali.patch', 'tests/stage5e_static.py', 'tests/stage5g_static.py', 'tools/stage_production_input.py'} | NEW_TESTS

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def old(path):
    return subprocess.check_output(['git', 'show', f'{BASE}:{path}'], cwd=ROOT).decode().replace('\r\n', '\n')

def method(text, name):
    found = re.search(r'^ +(?:[\w.<>?,\[\]]+ +)+' + name + r'\(', text, re.M)
    assert found, name
    start = found.start()
    body = text.index('{', start)
    depth = 1
    end = body + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return text[start:end]

def before_trace_cleanup(path, current):
    """Normalize ONLY the explicitly approved cleanup, after exact source validation.

    Older feature tests can retain their full-file comparisons without accepting
    arbitrary edits to functional methods or silently updating their baselines.
    """
    previous = old(path)
    expected = previous
    if path.endswith('/LiveEmoteAutocomplete.java'):
        changes = [
            ('    private static final String TAG = "BTTV-Autocomplete";\n',
             '    private static final String TAG = "BTTV-Autocomplete";\n    // Shared compile-time switch for emote lifecycle traces, not a user preference.\n    static final boolean DEBUG_LOGGING = false;\n'),
            ('existing.log("ATTACH duplicate ignored");', 'if (DEBUG_LOGGING) existing.log("ATTACH duplicate ignored");'),
            ('created.log("ATTACH new active=" + created.active);', 'if (DEBUG_LOGGING) created.log("ATTACH new active=" + created.active);'),
            ('owner.log("CHANNEL " + previous + " -> " + next);', 'if (DEBUG_LOGGING) owner.log("CHANNEL " + previous + " -> " + next);'),
            ('owner.log("NATIVE bound");', 'if (DEBUG_LOGGING) owner.log("NATIVE bound");'),
            ('            String event = "NATIVE merge native=" + count + " thirdParty=" + images.size()\n                    + " total=" + merged.size();\n', ''),
            ('            owner.log(event);', '            if (DEBUG_LOGGING) {\n                owner.log("NATIVE merge native=" + count + " thirdParty=" + images.size()\n                        + " total=" + merged.size());\n            }'),
            ('log(previouslyAttached ? "REATTACH" : "ATTACHED");', 'if (DEBUG_LOGGING) log(previouslyAttached ? "REATTACH" : "ATTACHED");'),
            ('                log("DETACH");', '                if (DEBUG_LOGGING) log("DETACH");'),
            ('        void log(String event) {\n', '        void log(String event) {\n            if (!DEBUG_LOGGING) return;\n'),
        ]
        for before, after in changes:
            assert expected.count(before) == 1, before
            expected = expected.replace(before, after)
    elif path.endswith('/LiveEmotes.java'):
        expected = expected.replace(method(expected, 'isChannel') + '\n', '')
        expected = re.sub(r'            boolean channelMatch = lookup\.isChannel\(emote\);.*?\+ " id=" \+ emote\.id\);\n', '', expected, flags=re.S)
        expected = re.sub(r'        Log\.i\(TAG, "THIRD_PARTY_URL.*?\);\n', '', expected, flags=re.S)
    elif path.endswith('/LiveEmoteChannels.java'):
        for before, after in (
            ('if (previous == null) {', 'if (previous == null && LiveEmoteAutocomplete.DEBUG_LOGGING) {'),
            ('                    log(providerName(source) + " channel " + channelId + " n=" + parsed.size());',
             '                    if (LiveEmoteAutocomplete.DEBUG_LOGGING) {\n                        log(providerName(source) + " channel " + channelId + " n=" + parsed.size());\n                    }'),
            ('log(providerName(source) + " channel " + id + " " + detail);', 'log(providerName(source) + " channel " + detail);'),
            ('            log("channel ready " + id);', '            if (LiveEmoteAutocomplete.DEBUG_LOGGING) {\n                log("channel ready " + id);\n            }'),
            ('log(providerName(source) + " channel " + id + " http=404 (empty)");', 'log(providerName(source) + " channel http=404 (empty)");'),
        ):
            assert expected.count(before) == 1, before
            expected = expected.replace(before, after)
    elif path == 'patches/live/z86.smali.patch':
        first = old('patches/diagnostics/z86.smali.patch').split('@@ -1975', 1)[0].split('@@ -1180', 1)[1]
        first = '@@ -1180' + '\n'.join(line.rstrip() for line in first.splitlines()) + '\n'
        expected = expected.replace('@@ -2550', first + '@@ -2550', 1)
    else:
        raise AssertionError('No cleanup normalization for ' + path)
    assert current == expected, path + ': changes outside the exact trace cleanup'
    return previous

class TraceCleanupTests(unittest.TestCase):
    def test_scope(self):
        names = set(subprocess.check_output(['git', 'diff', '--name-only', BASE], cwd=ROOT, text=True).splitlines())
        names.update(path for path in NEW_TESTS if (ROOT / path).is_file())
        self.assertEqual(names, CHANGED)

    def test_no_obsolete_trace_patches(self):
        for path in REMOVED:
            self.assertFalse((ROOT / path).exists())
        for path in (ROOT / 'patches').rglob('*.patch'):
            if path.parent.name.startswith('notifications_'):
                continue
            for text in ('TOKEN text=', 'PRESENTATION text=', 'RENDER text=', 'URL_INPUT id=', 'GLIDE url=', 'BTTV31TRACE'):
                self.assertNotIn(text, path.read_text(), str(path))

    def test_token_matching_url_and_fetch_logic_unchanged(self):
        path = JAVA + 'emote/LiveEmotes.java'
        current, previous = read(path), old(path)
        before_trace_cleanup(path, current)
        for name in ('find', 'allowed', 'split', 'findMatch', 'isOuterPunctuation', 'requestGlobals', 'isThirdPartyUrl'):
            self.assertEqual(method(current, name), method(previous, name), name)
        self.assertNotIn('THIRD_PARTY_MATCH', current)
        self.assertNotIn('THIRD_PARTY_URL', current)
        self.assertNotIn('isChannel(', current)  # This traversal was exclusively for the deleted trace.
        before = method(previous, 'urlForMarker')
        before = re.sub(r'        Log\.i\(TAG, "THIRD_PARTY_URL.*?\);\n', '', before, flags=re.S)
        self.assertEqual(method(current, 'urlForMarker'), before)
        before = method(previous, 'splitLoaded')
        before = re.sub(r'            boolean channelMatch = lookup\.isChannel\(emote\);.*?\+ " id=" \+ emote\.id\);\n', '', before, flags=re.S)
        self.assertEqual(method(current, 'splitLoaded'), before)

    def test_channel_fetch_policy_and_provider_failures(self):
        path = JAVA + 'emote/LiveEmoteChannels.java'
        current, previous = read(path), old(path)
        before_trace_cleanup(path, current)
        for name in ('validChannelId', 'enabled', 'snapshot', 'load7TV', 'parseBTTV', 'parseFFZ', 'parse7TVUser', 'parseArray'):
            self.assertEqual(method(current, name), method(previous, name), name)
        for snippet in ('request.loading || request.loaded', 'request.loading = true;',
                        'request.loaded = true;', 'request.retryAt = SystemClock.elapsedRealtime() + RETRY_AFTER_MS;',
                        'new ArrayBlockingQueue<Runnable>(64)', '3, 3, 30L', 'Collections.unmodifiableMap(new HashMap<>(parsed))'):
            self.assertIn(snippet, current)
        self.assertIn('previous == null && LiveEmoteAutocomplete.DEBUG_LOGGING', current)
        self.assertIn('if (LiveEmoteAutocomplete.DEBUG_LOGGING)', current)
        self.assertIn('log(providerName(source) + " channel " + detail)', current)
        self.assertNotIn('log(providerName(source) + " channel " + id', current)
        self.assertIn('failure.getClass().getSimpleName()', current)
        self.assertIn('http=404 (empty)', current)

    def test_autocomplete_guards_precede_construction(self):
        current = read(JAVA + 'emote/LiveEmoteAutocomplete.java')
        before_trace_cleanup(JAVA + 'emote/LiveEmoteAutocomplete.java', current)
        self.assertIn('static final boolean DEBUG_LOGGING = false;', current)
        for line in current.splitlines():
            if any(call in line for call in ('existing.log(', 'created.log(')):
                self.assertIn('if (DEBUG_LOGGING)', line)
        self.assertIn('if (DEBUG_LOGGING) owner.log("CHANNEL ', current)
        self.assertIn('if (DEBUG_LOGGING) owner.log("NATIVE bound")', current)
        self.assertIn('if (DEBUG_LOGGING) {\n                owner.log("NATIVE merge', current)
        self.assertIn('if (!DEBUG_LOGGING) return;', current)
        self.assertIn('failure("mergeNativeResults", failure)', current)
        for name in ('invalidate', 'resolveNativeImageUrl', 'observeLifecycle', 'notifyLifecycle', 'uniqueLifecycle'):
            self.assertEqual(method(current, name), method(old(JAVA + 'emote/LiveEmoteAutocomplete.java'), name))
        for path in ('emote/EmoteAutocompleteLogic.java', 'emote/EmoteAutocompleteCatalog.java',
                     'emote/PickerRuntime.java', 'emote/LiveEmotePicker.java', 'settings/UserPreferences.java'):
            self.assertEqual(read(JAVA + path), old(JAVA + path), path)

    def test_private_preferences_and_startup(self):
        path = JAVA + 'highlight/StringSetUIAdapter.java'
        expected = old(path).replace('import android.util.Log;\n', '').replace('                Log.i(TAG, "onClick: " + word);\n', '')
        self.assertEqual(read(path), expected)
        path = JAVA + 'Data.java'
        expected = old(path).replace('        Log.i("LBTTVDataSetContext", "context now is " + ((Data.ctx == null) ? "null" : "not null"));\n', '')
        self.assertEqual(read(path), expected)

    def test_normal_patch_application_equivalence_and_registers(self):
        with tempfile.TemporaryDirectory(prefix='trace-equivalence-', dir=ROOT / '.tmp') as temp:
            temp = Path(temp)
            for name in ('z86', 'f86', 'qz0', 'aaj'):
                relative = f'smali_classes2/{name}.smali'
                stock = read('.bttv-cache/disass/' + relative)
                for variant in ('before', 'after'):
                    target = temp / variant / relative
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_text(stock, encoding='utf-8', newline='\n')
                    subprocess.run(['git', 'init', '--quiet', str(temp / variant)], check=True)
                    patches = []
                    if variant == 'before':
                        patches.append(old(f'patches/diagnostics/{name}.smali.patch'))
                    live = f'patches/live/{name}.smali.patch'
                    if (ROOT / live).exists():
                        patches.append(old(live) if variant == 'before' else read(live))
                    for patch in patches:
                        for args in (['--check'], []):
                            subprocess.run(['git', 'apply'] + args + ['-'], input=patch.encode(), cwd=temp / variant, check=True)
                expected = (temp / 'before' / relative).read_text()
                for hunk in re.split(r'^@@.*?@@.*\n', old(f'patches/diagnostics/{name}.smali.patch'), flags=re.M)[1:]:
                    additions = ''.join(line[1:] for line in hunk.splitlines(keepends=True) if line.startswith('+'))
                    if additions.strip().startswith('.locals'):
                        if name != 'z86':
                            removed = ''.join(line[1:] for line in hunk.splitlines(keepends=True) if line.startswith('-'))
                            expected = expected.replace(additions, removed, 1)
                    else:
                        self.assertTrue(additions in expected, name + ': missing trace block ' + repr(additions[:120]))
                        expected = expected.replace(additions, '', 1)
                self.assertTrue((temp / 'after' / relative).read_text() == expected, name + ': changed non-trace instructions')
                if name == 'z86':
                    start = stock.index('.method public final a(Ljava/lang/String;Lzun;Ljava/lang/String;Lf56;)Ll26;')
                    original = stock[start:stock.index('.end method', start)]
                    self.assertFalse(re.search(r'\bv(?:2[4-9]|[3-9]\d)\b', original), 'raw parameter aliases in stock')
                    final = (temp / 'after' / relative).read_text()
                    self.assertIn('.locals 27', final[start:])
                    self.assertIn('invoke-static/range {v24 .. v25}, Lbttv/emote/LiveEmoteBridge;->rewrite', final)

    def test_production_staging_excludes_notification_work(self):
        source = read('tools/stage_production_input.py')
        self.assertIn('["git", "ls-files", "--deleted", "-z"]', source)
        self.assertIn('p not in deleted', source)
        for prefix in ('patches/notifications_diag/', 'patches/notifications_reason/', 'mod/app/src/main/java/bttv/notifications/'):
            self.assertIn('"' + prefix + '"', source)
        for path in (ROOT / 'patches').rglob('*.patch'):
            if path.parent.name.startswith('notifications_'):
                continue
            subprocess.run(['git', 'apply', '--numstat', str(path)], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)

if __name__ == '__main__':
    unittest.main()
