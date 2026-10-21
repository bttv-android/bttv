"""Actual profile/card hook contracts; no device or purchase operations."""
from pathlib import Path
import difflib
import json
import subprocess
import tempfile
import unittest

R = Path(__file__).resolve().parents[1]
D = R/'.bttv-cache/disass'
NAMES = ['BrowserPurchaseBridge', 'ProfilePurchaseSource', 'ViewerPurchaseSource']


class BrowserUiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix='web-ui-', dir=R/'.tmp')
        cls.f = Path(cls.temp.name)
        subprocess.run(['git', 'init', '--quiet'], cwd=cls.f, check=True)
        for name in ['nar', 'uwt']:
            p = cls.f/f'smali_classes5/{name}.smali'
            p.parent.mkdir(exist_ok=True)
            p.write_text((D/f'smali_classes5/{name}.smali').read_text(encoding='utf-8'), encoding='utf-8', newline='\n')
        subprocess.run(['git', '-c', 'core.autocrlf=false', 'add', '--', 'smali_classes5'], cwd=cls.f, check=True)
        for name in ['nar', 'uwt'] + ['bttvcompat.'+n for n in NAMES]:
            p = R/f'patches/live/{name}.smali.patch'
            subprocess.run(['git', 'apply', '--numstat', str(p)], check=True, capture_output=True)
            for flags in [['--check', '--index'], ['--index']]:
                subprocess.run(['git', '-c', 'core.autocrlf=false', 'apply', *flags, str(p)], cwd=cls.f, check=True, capture_output=True)

    @classmethod
    def tearDownClass(cls): cls.temp.cleanup()

    def read(self, name):
        return (self.f/f'smali_classes7/bttvcompat/{name}.smali').read_text()

    def test_only_two_additive_native_hooks_no_register_expansion(self):
        for name, added in [
            ('nar', '\n    invoke-static {p0, p1}, Lbttvcompat/BrowserPurchaseBridge;->profile(Ljava/lang/Object;Ljava/lang/Object;)V\n'),
            ('uwt', '\n    move-object/from16 v1, p0\n    iget-object v1, v1, Luwt;->b:Ljava/lang/Object;\n    invoke-static {v1, v0}, Lbttvcompat/BrowserPurchaseBridge;->viewer(Ljava/lang/Object;Ljava/lang/Object;)V\n')]:
            old = (D/f'smali_classes5/{name}.smali').read_text(encoding='utf-8')
            new = (self.f/f'smali_classes5/{name}.smali').read_text()
            self.assertEqual(new.count(added), 1)
            self.assertEqual(new.replace(added, ''), old)
        # nar p0=v13, p1=v14: both fit invoke-35c, before p1's later reuse.
        self.assertIn('.locals 13', (self.f/'smali_classes5/nar.smali').read_text().split('.method public final D2(')[1].split('.end method')[0])
        # uwt v1 is overwritten immediately by stock instanceof; all original branches intact.
        self.assertIn('viewer(Ljava/lang/Object;Ljava/lang/Object;)V\n\n    .line 4', (self.f/'smali_classes5/uwt.smali').read_text())

    def test_profile_is_actual_channel_header_not_player_or_billing(self):
        fragment = (D/'smali_classes5/tv/twitch/android/feature/profile/header/ChannelProfileHeaderFragment.smali').read_text()
        self.assertIn('Lqar;-><init>', fragment)
        source = self.read('ProfilePurchaseSource')
        for expected in ['Lnar;->S:Ltv/twitch/android/models/channel/ChannelModel;', '->getName()', '->isPartner()', '->isAffiliate()', '->isMonetized()']:
            self.assertIn(expected, source)
        self.assertNotIn('getDisplayName', source)
        self.assertNotIn('isLive', source)
        layout = (D/'res/layout/profile_card_no_divider.xml').read_text()
        self.assertIn('@id/other_user_action_container', layout)
        self.assertIn('@id/follow_button', layout)
        self.assertIn('@id/notifications_button', layout)
        self.assertIn('Lqar;->s:Landroidx/constraintlayout/widget/ConstraintLayout;', self.read('BrowserPurchaseBridge'))

    def test_viewer_snapshot_is_loaded_local_and_revalidated_at_tap(self):
        s = self.read('ViewerPurchaseSource')
        for expected in ['->M()Lhk80;', '->getView()Landroid/view/View;', 'Lhk80;->p:Lhf00;', '->getValue()', 'Lhk80;->s:', 'Lhk80;->u:', 'Lhk80;->A:', 'if-ne v1, v2, :invalid', 'if-ne v0, v1, :invalid']:
            self.assertIn(expected, s)
        for forbidden in ['Lhk80;->B:', 'Lhk80;->C:', 'Lhk80;->v:', 'Data;', 'Log;', 'getSourceChannel']:
            self.assertNotIn(forbidden, s)
        b = self.read('BrowserPurchaseBridge')
        self.assertIn('instance-of v2, p1, Lsj80;', b)
        self.assertIn(':empty\n    const/4 v2, 0x0', b)
        self.assertEqual(b.count('.catch Ljava/lang/Throwable;'), 2)

    def test_bits_and_existing_subscribe_functions_unchanged(self):
        before = json.loads((R/'.tmp/stage5qk/phase4/before.json').read_text())
        import hashlib
        for path, h in before['phase2'].items():
            if path.endswith(('WebPurchaseRedirect.java', 'Res.java', 'strings.xml')): continue
            self.assertEqual(hashlib.sha256((R/path).read_bytes()).hexdigest().upper(), h, path)
        old = (R/'.tmp/stage5qk/phase4/production-input/mod/app/src/main/java/bttv/commerce/WebPurchaseRedirect.java').read_text()
        current = (R/'mod/app/src/main/java/bttv/commerce/WebPurchaseRedirect.java').read_text()
        addition = current[current.index('    /** Explicit web navigation:'):current.index('    static boolean validLogin')]
        self.assertEqual(current.replace(addition, ''), old)

    def test_only_official_navigation_no_native_state_writes(self):
        java = (R/'mod/app/src/main/java/bttv/commerce/BrowserPurchaseUi.java').read_text()
        for forbidden in ['tryBits(', 'trySubscribe(', 'Settings.', 'Log.', 'Clipboard', 'Cookie', 'HttpURLConnection', 'WebView', 'Data.', 'GiftPageType', 'purchaser']:
            self.assertNotIn(forbidden, java)
        self.assertIn('openChannelWeb(activity, login)', java)
        self.assertIn('!login.equals(source.login())', java)
        self.assertIn('row.getParent() != parent', java)
        for name in NAMES:
            for line in self.read(name).splitlines():
                if 'iput' in line:
                    self.assertIn('Lbttvcompat/', line)
        for name in ['bm60', 'yej', 'zl60', 'yl60', 'gv4', 'qkn']:
            self.assertFalse((R/f'patches/live/{name}.smali.patch').exists())


if __name__ == '__main__': unittest.main(verbosity=2)
