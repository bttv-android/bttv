"""Manual updater production scope and integration contracts; no network/device operations."""
import hashlib
from pathlib import Path
import subprocess
import unittest

ROOT=Path(__file__).resolve().parents[1]
BASE='b6b7d4073a1d5cd61dbde9b3c381f752759d8337'
MODIFIED={'buildsource','mod/app/build.gradle','mod/app/src/main/java/bttv/settings/SettingsActivity.java',
          'mod/app/src/main/java/bttv/settings/SettingsSections.java','patches/AndroidManifest.xml.patch'}
NEW={f'mod/app/src/main/java/bttv/updater/{n}.java' for n in
     ['UpdateRelease','UpdateTransfer','UpdateSession','UpdateVerifier','UpdateController','UpdateDialog']}|{
     'mod/app/libs/apksig-sdk30.0.3.jar','mod/app/src/main/res/raw/bttv_apksig_license.txt','mod/app/libs/NOTICE-apksig.md',
     'tests/stage5q_updater.py','tests/stage5q_updater_host.py'}
def read(p):return (ROOT/p).read_text(encoding='utf-8')
def old(p):return subprocess.check_output(['git','show',BASE+':'+p],cwd=ROOT).decode().replace('\r\n','\n')
class UpdaterTests(unittest.TestCase):
    def test_scope_and_untouched_features(self):
        self.assertEqual(set(subprocess.check_output(['git','diff','--name-only'],cwd=ROOT,text=True).splitlines()),MODIFIED)
        for p in ['bttv.manifest.json','build','disassemble','mod/app/src/main/java/bttv/Data.java',
                  'mod/app/src/main/java/bttv/AnonChat.java','mod/app/src/main/java/bttv/settings/Settings.java',
                  'mod/app/src/main/java/bttv/settings/UserPreferences.java','patches/live/f86.smali.patch','patches/live/z86.smali.patch']:
            self.assertEqual(read(p),old(p),p)
        self.assertFalse(subprocess.check_output(['git','diff','--cached','--name-only'],cwd=ROOT).strip())
    def test_only_required_d8_root_helper_copy(self):
        p='buildsource'
        addition='''
# The updater's SDK apksig verifier needs D8's root-level Math backport at min-api21.
# Keep compiler-generated helpers alongside the injected code, not silently dropped.
find "$TMP_DIR" -maxdepth 1 -type f -name '$r8$backportedMethods$*.smali' -exec cp -n {} "$FINAL_TARGET/" \\;
'''
        self.assertEqual(read(p),old(p).replace('mv "$TMP_DIR/com" "$FINAL_TARGET/."\n','mv "$TMP_DIR/com" "$FINAL_TARGET/."\n'+addition))
    def test_settings_only_existing_manual_entry(self):
        ui=read('mod/app/src/main/java/bttv/settings/SettingsActivity.java')
        self.assertIn('case UPDATE_CHECK:',ui); self.assertIn('new UpdateDialog(this',ui)
        self.assertIn('case InvokeUpdateCheck:',read('mod/app/src/main/java/bttv/settings/SettingsSections.java'))
        self.assertNotIn('case AutoUpdateChecksEnabled:',read('mod/app/src/main/java/bttv/settings/SettingsSections.java'))
        self.assertIn('updaterDialog.dispose()',ui); self.assertIn('onSaveInstanceState',ui)
    def test_manifest_only_install_permission(self):
        p='patches/AndroidManifest.xml.patch'
        expected=old(p).replace('@@ -1,4 +1,14 @@','@@ -1,4 +1,15 @@').replace(
            '+    platformBuildVersionName="17">\n','+    platformBuildVersionName="17">\n+    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>\n')
        self.assertEqual(read(p),expected)
        subprocess.run(['git','apply','--numstat',p],cwd=ROOT,check=True,stdout=subprocess.DEVNULL)
    def test_legacy_remains_dormant(self):
        for n in ['Updater','UpdaterActivity','UpdaterJobService','Notifications']:
            p=f'mod/app/src/main/java/bttv/updater/{n}.java';self.assertEqual(read(p),old(p))
        for p in NEW:
            if '/java/' not in p:continue
            source=read(p)
            for forbidden in ['okhttp3','okio.','Markwon','PersistentBannerPresenter','JobScheduler','setPeriodic','bttv.Network','BadgeImages','SharedPreferences','Log.']:
                self.assertNotIn(forbidden,source,p)
    def test_origin_and_fail_closed_verifier(self):
        parser=read('mod/app/src/main/java/bttv/updater/UpdateRelease.java')
        self.assertIn('https://api.github.com/repos/bttv-android/bttv/releases/latest',parser)
        verifier=read('mod/app/src/main/java/bttv/updater/UpdateVerifier.java')
        for check in ['signature.isVerified()','getSignerCertificates().size()==1','signer.equals(installed.signer)',
                      'p.getLongVersionCode()>installed.code','nativeAndZip','UpdateTransfer.digest(apk,cancel)']:
            self.assertIn(check,verifier)
        self.assertEqual(hashlib.sha256((ROOT/'mod/app/libs/apksig-sdk30.0.3.jar').read_bytes()).hexdigest().upper(),
                         'ADDF02D5B6867CCD87FF00A1FD2AF4F65C0D4F7EB5EA6189EA6E144ADD52CE2C')
    def test_no_installer_from_worker(self):
        controller=read('mod/app/src/main/java/bttv/updater/UpdateController.java')
        self.assertNotIn('startActivity',controller);self.assertIn('WeakReference<Observer>',controller)
        self.assertIn('new ArrayBlockingQueue<>(1)',controller)
        ui=read('mod/app/src/main/java/bttv/updater/UpdateDialog.java')
        self.assertIn('FLAG_GRANT_READ_URI_PERMISSION',ui);self.assertIn('Intent.ACTION_VIEW',ui)
        self.assertIn('installRequest==state.generation',ui);self.assertIn('hasWindowFocus()',ui)
        self.assertIn('installRequest=-1',ui);self.assertNotIn('file://',ui)
    def test_preserved_archive(self):
        self.assertEqual(hashlib.sha256((ROOT/'dist/archive/BTTV_Stage5Q_F_Badges_Test.apk').read_bytes()).hexdigest().upper(),
                         '1203CD3AFE74F75C7FAC52D2F5EC6B22EBBD1E9465C34D26E5F9B0E7EDBBF3FB')
if __name__=='__main__':unittest.main()
