"""Stage 5Q-E: exact one-condition scope and preserved renderer-compatible order."""
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASE = '83ef02b363c0169d5d0870fdae27894579d1f2b3'
LOGIC = 'mod/app/src/main/java/bttv/emote/EmoteAutocompleteLogic.java'
JUNIT = 'mod/app/src/test/java/bttv/emote/EmoteAutocompleteLogicTest.java'
OLD = 'if (previous == null || (candidate.channel && !previous.channel)) {'
NEW = 'if (previous == null) {'


def read(path):
    return (ROOT / path).read_text(encoding='utf-8')


def original(path):
    return subprocess.check_output(['git', 'show', BASE + ':' + path], cwd=ROOT).decode().replace('\r\n', '\n')


class ConsistencyTests(unittest.TestCase):
    def test_exact_single_production_condition(self):
        before = original(LOGIC)
        self.assertEqual(before.count(OLD), 1)
        self.assertEqual(read(LOGIC), before.replace(OLD, NEW))

    def test_only_two_junit_expectations_change(self):
        before = original(JUNIT)
        before = before.replace('channelWinsExactDuplicateInEitherOrder', 'firstEligibleWinsExactDuplicateInInputOrder')
        before = before.replace('            assertSame(channel, result.get(0));', '            assertSame(input.get(0), result.get(0));')
        before = before.replace('mergedChannelWinsGlobalCollision', 'mergedFirstEligibleWinsGlobalCollision')
        before = before.replace('void mergedFirstEligibleWinsGlobalCollision() {',
                                'void mergedFirstEligibleWinsGlobalCollision() {\n        EmoteAutocompleteLogic.Suggestion global = suggestion("Omega", false);')
        before = before.replace('Arrays.asList(suggestion("Omega", false), channel)', 'Arrays.asList(global, channel)')
        before = before.replace('        assertSame(channel, result.get(0));', '        assertSame(global, result.get(0));')
        self.assertEqual(read(JUNIT), before)

    def test_all_other_tracked_files_unchanged(self):
        changed = set(subprocess.check_output(['git', 'diff', '--name-only', BASE], cwd=ROOT, text=True).splitlines())
        self.assertLessEqual(changed, {LOGIC, JUNIT, 'tests/stage5q_emote_consistency.py',
                                      'tests/stage5q_emote_consistency_host.py'})
        self.assertEqual(read('patches/live/z86.smali.patch'), original('patches/live/z86.smali.patch'))
        self.assertEqual(read('patches/live/f86.smali.patch'), original('patches/live/f86.smali.patch'))

    def test_combined_native_ranking_and_ten_cap_preserved(self):
        code = read(LOGIC)
        self.assertIn('Math.min(10, result.size())', code)
        self.assertIn('match(prefix, thirdParty, Integer.MAX_VALUE)', code)
        self.assertIn('old.nativeItem == null', code)
        self.assertIn('unique.get(candidate.code)', code)
        self.assertIn('regionMatches(true', code)

    def test_catalog_order_is_existing_interleaved_order(self):
        code = read('mod/app/src/main/java/bttv/emote/EmoteAutocompleteCatalog.java')
        terms = ['Emotes.globalEmotesFFZ', 'Emotes.Source.FFZ', 'Emotes.globalEmotesBTTV',
                 'Emotes.Source.BTTV', 'Emotes.globalEmotes7TV', 'Emotes.Source.STV']
        self.assertEqual(sorted(code.index(t) for t in terms), [code.index(t) for t in terms])
        self.assertIn('Integer.MAX_VALUE', code)
        self.assertNotIn('ensureChannel(', code)

    def test_preserved_catalog_picker_details_and_lifecycle_sources(self):
        for name in ('EmoteAutocompleteCatalog', 'LiveEmotes', 'LiveEmotePicker', 'EmoteDetails',
                     'LiveEmoteChannels', 'LiveEmoteGlobals', 'LiveEmoteAutocomplete', 'GifPolicy',
                     'PickerRuntime', 'PickerInsertion', 'PickerSyntheticId', 'AutocompleteSyntheticId',
                     'EmoteCatalogEvents'):
            path = 'mod/app/src/main/java/bttv/emote/' + name + '.java'
            self.assertEqual(read(path), original(path), path)

    def test_patch_inventory_and_normal_parser(self):
        paths = subprocess.check_output(['git', 'ls-files', 'patches/*.patch'], cwd=ROOT, text=True).splitlines()
        self.assertEqual(len(paths), 51)
        for path in paths:
            self.assertEqual(read(path), original(path))
            subprocess.run(['git', 'apply', '--numstat', str(ROOT / path)], cwd=ROOT,
                           check=True, stdout=subprocess.DEVNULL)

    def test_no_network_or_device_actions_in_focused_fixture(self):
        fixture = read('tests/stage5q_emote_consistency_host.py')
        for forbidden in ('openConnection(', 'subprocess.', 'adb.exe', 'os.system(', 'requests.get('):
            self.assertNotIn(forbidden, fixture)
        self.assertIn('LiveEmoteGlobals.calls==0', fixture)
        self.assertIn('EmoteAutocompleteCatalog.find', fixture)
        self.assertIn('LiveEmotePicker.winner', fixture)
        self.assertIn('EmoteDetails.create', fixture)


if __name__ == '__main__':
    unittest.main()
