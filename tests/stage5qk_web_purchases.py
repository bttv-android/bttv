"""Optional native purchase redirect invariants; no device or payment operations."""
from pathlib import Path
import re
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DECODE = ROOT/'.bttv-cache/disass'
PATCHES = ['bho.smali.patch', 'bttvcompat.WebPurchaseBridge.smali.patch',
           'dj20.smali.patch', 'iwn.smali.patch', 'zz_dj20_relocation.patch']


def read(path):
    return (ROOT/path).read_text(encoding='utf-8')


def methods(text):
    return {part.splitlines()[0]: part.split('.end method', 1)[0]
            for part in text.split('.method ')[1:]}


class WebPurchaseTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix='stage5qk-', dir=ROOT/'.tmp')
        cls.fixture = Path(cls.temp.name)
        for relative in ['smali/bho.smali', 'smali/iwn.smali', 'smali_classes2/dj20.smali']:
            p = cls.fixture/relative
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text((DECODE/relative).read_text(), encoding='utf-8', newline='\n')
        subprocess.run(['git', 'init', '--quiet'], cwd=cls.fixture, check=True)
        subprocess.run(['git', '-c', 'core.autocrlf=false', 'add', '--',
                        'smali/bho.smali', 'smali/iwn.smali', 'smali_classes2/dj20.smali'], cwd=cls.fixture, check=True)
        for patch in PATCHES:
            path = str(ROOT/'patches/live'/patch)
            subprocess.run(['git', 'apply', '--numstat', path], check=True, capture_output=True)
            for flags in [['--check', '--index'], ['--index']]:
                subprocess.run(['git', '-c', 'core.autocrlf=false', 'apply', *flags, path],
                               cwd=cls.fixture, check=True, capture_output=True)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_bits_is_before_native_navigation_and_exact_stock_tail(self):
        old = (DECODE/'smali/bho.smali').read_text()
        new = (self.fixture/'smali/bho.smali').read_text()
        a, b = methods(old), methods(new)
        self.assertEqual(set(a), set(b))
        for signature in a:
            if not signature.startswith('public C('):
                self.assertEqual(a[signature], b[signature])
            else:
                self.assertEqual(a[signature].split('    .line 1', 1)[1],
                                 b[signature].split('    .line 1', 1)[1])
                self.assertIn('invoke-static {p1}', b[signature])
                self.assertLess(b[signature].index('tryBits'), b[signature].index('Lx5r;->d('))
                self.assertIn('if-eqz v0, :bttv_bits_native\n\n    return-void', b[signature])

    def test_subscription_identity_relocation_and_stock_body(self):
        self.assertFalse((self.fixture/'smali_classes2/dj20.smali').exists())
        old = methods((DECODE/'smali_classes2/dj20.smali').read_text())
        new = methods((self.fixture/'smali_classes7/dj20.smali').read_text())
        for sig, body in old.items():
            if ' final c(' in sig:
                self.assertEqual(body.replace(' final c(', ' final bttvNativeC(', 1),
                                 new[sig.replace(' final c(', ' final bttvNativeC(', 1)])
                wrapper = new[sig]
                self.assertIn('if-ne p3, v0, :bttv_subscription_native', wrapper)
                self.assertIn('SubscribePageType', wrapper)
                self.assertIn('invoke-virtual/range {p0 .. p6}', wrapper)
                self.assertEqual(wrapper.count('trySubscribe'), 1)
            elif ' final b(' in sig:
                self.assertEqual(body.split('    .line 1', 1)[1].replace('Ldj20;->c(', 'Ldj20;->bttvNativeC('),
                                 new[sig].split('    .line 1', 1)[1])
                self.assertEqual(new[sig].count('trySubscribe'), 1)
                self.assertNotIn('Ldj20;->c(', new[sig])
            else:
                self.assertEqual(body, new[sig])
        bridge = (self.fixture/'smali_classes7/bttvcompat/WebPurchaseBridge.smali').read_text()
        self.assertIn('SubscriptionChannelModel;->getName()', bridge)
        self.assertIn('.catch Ljava/lang/Throwable;', bridge)
        self.assertNotIn('getId(', bridge)

    def test_landscape_bits_before_luap_and_exact_original_after_hook(self):
        original = (DECODE/'smali/iwn.smali').read_text()
        text = (self.fixture/'smali/iwn.smali').read_text()
        start = text.index('    iget-object v2, v0, Lqwn;->m:', text.index('check-cast v3, Lko5;'))
        end = text.index('    iget-object v2, v0, Lqwn;->o:Lkmd;', start)
        self.assertEqual(text[:start]+text[end:], original)
        hook = text[start:end]
        self.assertIn('tryBits(Landroid/app/Activity;)Z', hook)
        self.assertIn('return-object v2', hook)
        self.assertLess(start, text.index('Luap;->J2(', start))

    def test_new_settings_are_independent_off_and_no_inert_gift_toggle(self):
        text = read('mod/app/src/main/java/bttv/settings/Settings.java')
        for name, key in [('WebBitsPurchasesEnabled', 'bttv_web_bits_purchases'),
                          ('WebSubscriptionsEnabled', 'bttv_web_subscriptions')]:
            body = text.split(name+'(', 1)[1].split('\n    ),', 1)[0]
            self.assertIn('new Entry.BoolEntry(', body)
            self.assertIn('new Entry.BoolValue(false)', body)
            self.assertIn('"'+key+'"', body)
            self.assertIn('case '+name+':', read('mod/app/src/main/java/bttv/settings/SettingsSections.java'))
        self.assertNotIn('WebGift', text)
        labels = ET.parse(ROOT/'mod/app/src/main/res/values/strings.xml').getroot()
        self.assertIsNotNone(labels.find("string[@name='bttv_settings_web_bits_purchases']"))
        self.assertIsNotNone(labels.find("string[@name='bttv_settings_web_subscriptions']"))

    def test_existing_settings_and_resources_preserved_exactly(self):
        def original(path):
            return subprocess.check_output(['git', 'show', 'HEAD:'+path], cwd=ROOT).decode().replace('\r\n', '\n')
        settings = 'mod/app/src/main/java/bttv/settings/Settings.java'
        text = re.sub(r'    Web(?:BitsPurchasesEnabled|SubscriptionsEnabled)\(.*?\n    \),\n', '', read(settings), flags=re.S)
        self.assertEqual(text, original(settings))
        sections = 'mod/app/src/main/java/bttv/settings/SettingsSections.java'
        text = read(sections).replace('            case WebBitsPurchasesEnabled:\n', '').replace('            case WebSubscriptionsEnabled:\n', '')
        self.assertEqual(text, original(sections))
        path = 'mod/app/src/main/res/values/strings.xml'
        current = ET.fromstring(read(path))
        for entry in ET.fromstring(original(path)):
            if entry.tag == 'string':
                self.assertEqual(ET.tostring(entry).strip(),
                                 ET.tostring(current.find("string[@name='"+entry.get('name')+"']")).strip())

    def test_privacy_browser_safety_and_no_global_billing_changes(self):
        java = read('mod/app/src/main/java/bttv/commerce/WebPurchaseRedirect.java')
        for bad in ['Log.', 'HttpURLConnection', 'okhttp', 'Cookie', 'WebView',
                    'currentBroadcaster', 'MANUFACTURER', 'Clipboard', 'getToken', 'auth/handoff']:
            self.assertNotIn(bad, java)
        for required in ['Intent.ACTION_VIEW', 'CATEGORY_BROWSABLE', 'GET_RESOLVED_FILTER',
                         'countDataAuthorities() == 0', 'WeakHashMap', 'isDestroyed()',
                         '.setComponent(', 'https://www.twitch.tv/bits',
                         'https://www.twitch.tv/subs/', 'Locale.ROOT']:
            self.assertIn(required, java)
        # Accepted features and all native billing/product logic stay byte-for-byte at HEAD.
        for path in ['patches/live/fbj.smali.patch', 'patches/live/f86.smali.patch',
                     'patches/live/bttvcompat.LiveBadgeBridge.smali.patch',
                     'mod/app/src/main/java/bttv/AnonChat.java',
                     'mod/app/src/main/java/bttv/badge/ChatterinoHttp.java',
                     'patches/AndroidManifest.xml.patch', 'buildsource', 'mod/app/build.gradle']:
            original = subprocess.check_output(['git', 'show', 'HEAD:'+path], cwd=ROOT).decode().replace('\r\n', '\n')
            self.assertEqual(read(path), original, path)


if __name__ == '__main__':
    unittest.main(verbosity=2)
