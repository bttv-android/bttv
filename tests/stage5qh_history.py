"""Stage5Q-H scope and actual native hook contracts. No build/device/network operations."""
from pathlib import Path
import re,subprocess,unittest
R=Path(__file__).resolve().parents[1]
BASE='6e25e23e9c03915a1de827ce07b58b2bd006a66c'
MODIFIED={'mod/app/src/main/java/bttv/emote/PickerRuntime.java','mod/app/src/main/java/bttv/emote/LiveEmotes.java',
          'mod/twitch/src/main/java/bttvcompat/NativePickerBridge.java','patches/live/bttvcompat.NativePickerBridge.smali.patch'}
NEW={'mod/app/src/main/java/bttv/emote/EmoteUsageHistory.java','mod/app/src/main/java/bttv/emote/EmoteUsageStore.java',
     'mod/app/src/test/java/bttv/emote/EmoteUsageHistoryTest.java','patches/live/fbj.smali.patch',
     'tests/stage5qh_history.py','tests/stage5qh_history_host.py'}
def read(p):return (R/p).read_text(encoding='utf-8')
def git(*a):return subprocess.check_output(['git',*a],cwd=R,text=True).strip()
def added(p):return '\n'.join(l[1:] for l in read(p).splitlines() if l.startswith('+') and not l.startswith('+++'))
class HistoryTests(unittest.TestCase):
 def test_scope(self):
  self.assertEqual(git('rev-parse','HEAD'),BASE);self.assertEqual(set(git('diff','--name-only').splitlines()),MODIFIED)
  untracked=set(git('ls-files','--others','--exclude-standard').splitlines())
  diag={p for p in untracked if '/notifications/' in p or '/notifications_' in p or p.startswith('tests/stage5m_')}
  self.assertEqual(len(diag),18);self.assertEqual(untracked-diag,NEW);self.assertFalse(git('diff','--cached','--name-only'))
 def test_native_call_exact_and_local(self):
  p=read('patches/live/fbj.smali.patch');a=added('patches/live/fbj.smali.patch')
  self.assertEqual(a.count('invoke-static'),1);self.assertIn('if-nez v1, :bttv_usage_done',a)
  self.assertIn('invoke-static {p0, p1}, Lbttvcompat/NativePickerBridge;->submitted',a)
  self.assertIn('move-exception v8',a);self.assertIn('.catch Ljava/lang/Throwable;',a)
  self.assertNotRegex(p,r'(?m)^-(?!--).*\S');self.assertNotIn('.locals',a)
  stock=read('.bttv-cache/disass/smali/fbj.smali');method=stock.split('.method public B(',1)[1].split('.end method',1)[0]
  self.assertIn('.locals 10',method);self.assertIn('Lvqf;->e(Ljava/lang/String;)V',method);self.assertIn('Ldwn;->b(',method)
  self.assertIn('instance-of v1, v0, Lqn5;',method);self.assertIn('instance-of v0, v0, Lun5;',method)
 def test_fail_open_native_region(self):
  a=added('patches/live/fbj.smali.patch');region=a.split(':try_start_bttv_usage',1)[1].split(':try_end_bttv_usage',1)[0]
  self.assertEqual(len([x for x in region.splitlines() if x.strip()]),1)
  self.assertNotIn('vqf;',region);self.assertNotIn('dwn;',region)
  self.assertNotIn('move-result',a);self.assertNotIn('return',a)
 def test_one_recording_source_no_traffic(self):
  store=read('mod/app/src/main/java/bttv/emote/EmoteUsageStore.java')
  bridge=added('patches/live/bttvcompat.NativePickerBridge.smali.patch')
  self.assertEqual(bridge.count('EmoteUsageStore;->submitted'),1)
  self.assertIn('Settings.AnonChatEnabled',store);self.assertIn('text.trim().startsWith("/")',store)
  self.assertIn('usageNativeCodes',store);self.assertIn('if(nativeCodes==null)return',store)
  self.assertNotIn('worker.record',read('mod/app/src/main/java/bttv/emote/PickerRuntime.java'))
  for token in ['HttpURLConnection','openConnection','bttv.Network','requestGlobals','ensureChannel']:
   self.assertNotIn(token,store)
  live=read('mod/app/src/main/java/bttv/emote/LiveEmotes.java')
  usage=live.split('static List<LiveEmotePicker.Entry> usageLoaded',1)[1].split('private static boolean nativeRun',1)[0]
  self.assertNotIn('requestGlobals',usage);self.assertIn('findMatch(',usage);self.assertIn('LiveEmotePicker.winner',usage)
 def test_storage_boundaries(self):
  h=read('mod/app/src/main/java/bttv/emote/EmoteUsageHistory.java');s=read('mod/app/src/main/java/bttv/emote/EmoteUsageStore.java')
  for v in ['PER_ACCOUNT = 50','ACCOUNTS = 4','BYTES = 256 * 1024','DISPLAY = 24','Integer.MAX_VALUE','Long.MAX_VALUE','depth>8']:
   self.assertIn(v,h)
  self.assertIn('getSharedPreferences("BTTV_EmoteUsage"',s);self.assertIn('putString("state_v1"',s)
  self.assertIn('.commit()',s);self.assertIn('ArrayBlockingQueue<>(64)',s)
  for v in ['getString("', 'putString("']:
   self.assertNotIn(v,h)
 def test_preserved_native_abis_and_files(self):
  for path in ['patches/live/lpf.smali.patch','patches/live/b6b.smali.patch','patches/live/blp.smali.patch',
               'patches/live/nt6.smali.patch','patches/live/z86.smali.patch','patches/live/f86.smali.patch',
               'patches/live/zy_gif_modes.patch','mod/app/src/main/java/bttv/AnonChat.java',
               'mod/app/src/main/java/bttv/emote/LiveEmotePicker.java']:
   self.assertEqual(read(path),git('show',BASE+':'+path)+'\n',path)
  patches=[p for p in git('ls-files','patches').splitlines() if p.endswith('.patch')]+['patches/live/fbj.smali.patch']
  self.assertEqual(len(patches),53)
  for p in patches:subprocess.run(['git','apply','--numstat',p],cwd=R,check=True,stdout=subprocess.DEVNULL)
if __name__=='__main__':unittest.main(verbosity=2)
