"""Stage 7 source boundaries and preserved manual/Anon integration; no network."""
from pathlib import Path
import subprocess
import unittest
import xml.etree.ElementTree as ET

R=Path(__file__).resolve().parents[1]
BASE='d6786c09de56c59c8041ef5d4e0defbe9461b125'
def read(p):return (R/p).read_text(encoding='utf-8')
def old(p):return subprocess.check_output(['git','show',BASE+':'+p],cwd=R).decode().replace('\r\n','\n')
P='mod/app/src/main/java/bttv/'
class AutomaticUpdaterTests(unittest.TestCase):
 def test_existing_startup_bridge_only(self):
  p=P+'Data.java'
  self.assertEqual(read(p),old(p).replace('        bttv.badge.BadgeImages.initialize(Data.ctx);','        bttv.badge.BadgeImages.initialize(Data.ctx);\n        bttv.updater.AutoUpdateChecker.initialize(Data.ctx);'))
 def test_stage6_identity_and_native_hooks_unchanged(self):
  for p in [P+'AnonChat.java',P+'api/AnonChat.java',P+'settings/SettingsActivity.java',P+'settings/StreamQuickSettings.java',
            'patches/live/qo5.smali.patch','patches/live/wp5.smali.patch','patches/live/ilg.smali.patch','patches/live/e66.smali.patch']:
   self.assertEqual(read(p),old(p),p)
 def test_existing_preference_default_and_manual_action(self):
  p=P+'settings/Settings.java';self.assertEqual(read(p),old(p))
  entry=read(p).split('    AutoUpdateChecksEnabled(',1)[1].split('    InvokeUpdateCheck(',1)[0]
  self.assertIn('"bttv_auto_update_checks_enabled"',entry);self.assertIn('BoolValue(true)',entry)
  sections=read(P+'settings/SettingsSections.java');self.assertIn('case AutoUpdateChecksEnabled:',sections);self.assertIn('case InvokeUpdateCheck:',sections)
  strings={e.get('name'):e.text for e in ET.fromstring(read('mod/app/src/main/res/values/strings.xml'))}
  self.assertEqual(strings['bttv_auto_update_checks_enabled'],'Automatically check for updates')
  self.assertEqual(strings['bttv_auto_update_checks_enabled_secondary'],'Check for new BTTV versions periodically and notify me when an update is available.')
 def test_no_manifest_or_build_changes(self):
  for p in ['patches/AndroidManifest.xml.patch','mod/app/build.gradle','bttv.manifest.json','build','buildsource','disassemble']:
   self.assertEqual(read(p),old(p),p)
 def test_legacy_remains_dormant(self):
  for name in ['Updater','UpdaterActivity','UpdaterJobService','Notifications']:
   p=P+'updater/'+name+'.java';self.assertEqual(read(p),old(p))
  for name in ['AutoUpdateChecker','AutoUpdateState']:
   src=read(P+'updater/'+name+'.java')
   for bad in ['UpdaterJobService.','Updater.checkForUpdates','okhttp3','JobScheduler','setPeriodic','startService','Network.get','Log.','AnonChat.']:
    self.assertNotIn(bad,src)
 def test_shared_worker_and_metadata_only(self):
  src=read(P+'updater/AutoUpdateChecker.java');self.assertIn('controller.checkAutomatically(',src)
  self.assertNotIn('.download(',src);self.assertNotIn('startActivity(',src);self.assertNotIn('prepareInstall(',src)
  c=read(P+'updater/UpdateController.java');self.assertEqual(c.count('new ThreadPoolExecutor('),1)
  self.assertIn('automatic==null && (!cleaned',c);self.assertIn('automaticGeneration==',c)
  self.assertIn('if(!automatic.enabled())',c);self.assertIn('UpdateTransfer.metadata(token,source)',c)
  self.assertIn('new ArrayBlockingQueue<>(1)',c);self.assertNotIn('startActivity(',c)
 def test_accepted_parser_and_verifier_security(self):
  self.assertEqual(read(P+'updater/UpdateVerifier.java'),old(P+'updater/UpdateVerifier.java'))
  src=read(P+'updater/UpdateRelease.java')
  for required in ['https://api.github.com/repos/bttv-android/bttv/releases/latest','code<=installed.code','boundedJson(compatibility)',
                   'signer.equals(hash(installed.signer))','PACKAGE.equals','assetUrl(url,tag,assetName)','json.getBoolean("prerelease")']:
   self.assertIn(required,src)
  for required in ['setInstanceFollowRedirects(false)','c.setConnectTimeout(15000)','c.setReadTimeout(20000)','cancel.check()','MAX_METADATA']:
   self.assertIn(required,read(P+'updater/UpdateTransfer.java'))
 def test_manual_dialog_flow_unchanged_except_description(self):
  p=P+'updater/UpdateDialog.java'
  self.assertEqual(read(p),old(p).replace('Official releases only · Manual checks only','Official releases only · Downloads and installation require your approval'))
 def test_foreground_lifecycle_and_safe_notice(self):
  src=read(P+'updater/AutoUpdateChecker.java')
  for required in ['if(instance!=null','ActivityLifecycleCallbacks','onActivityResumed','onActivityDestroyed','WeakReference<Activity>',
                   'main.removeCallbacks(foreground)','postDelayed(foreground,1500)','state.disabled(); controller.cancelAutomatic(); clearBanner();',
                   'activity.hasWindowFocus()','isInPictureInPictureMode()','View update','Later','new UpdateDialog(activity','state.notified(release)']:
   self.assertIn(required,src)
  self.assertNotIn('NotificationManager',src);self.assertNotIn('AlarmManager',src)
 def test_persisted_throttle_and_notice_policy(self):
  src=read(P+'updater/AutoUpdateState.java')
  for required in ['DAY=24*60*60*1000L','BACKOFF=60*60*1000L','!store.enabled() || flight>=0','now-success<DAY',
                   'Math.max(now+BACKOFF,retry)','pending.code>notified','flight!=ticket','store.checked(success,attempt,retryAt)']:
   self.assertIn(required,src)
  self.assertNotIn('getBoolean',src);self.assertNotIn('Thread',src)
if __name__=='__main__':unittest.main()
