"""Static contract for the verified player gear -> native View Options integration."""
from pathlib import Path
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
source = (ROOT/'mod/app/src/main/java/bttv/settings/StreamQuickSettings.java').read_text()
patch = (ROOT/'patches/live/ao10.smali.patch').read_text()
layout_patch = ROOT/'patches/live/res.layout.stream_settings_fragment.xml.patch'

def check(condition, why):
    if not condition:
        raise AssertionError(why)

for file in (ROOT/'patches').rglob('*.patch'):
    # Notification work is deliberately dormant and outside production staging.
    if file.parent.name.startswith('notifications_'):
        continue
    subprocess.run(['git','apply','--numstat',str(file)], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
check(patch.count('StreamQuickSettings;->bind(Landroid/view/View;)V') == 1, 'one hook')
check('invoke-static/range {p2 .. p2}' in patch, 'actual constructor root, no scratch registers')
check(not any(line.startswith('-') and not line.startswith('---') for line in patch.splitlines()), 'stock code preserved')
for name in ('SplitChatEnabled','ShowDeletedMessagesEnabled','AlwaysShowChatTimestamps','AnonChatEnabled'):
    check('Settings.'+name in source, 'existing entry '+name)
check('setting.entry.set(' in source and 'UserPreferences.Entry.BoolValue(checked)' in source, 'existing write API')
check('AnonChat.reconnect(checked)' in source, 'accepted anon behavior')
for action in ('Highlight.getInstance().openDialog(activity)', 'Blacklist.getInstance().openDialog(activity)', 'SettingsNavigation.open(activity)'):
    check(action in source, 'existing action '+action)
check('WeakReference<View>' in source and 'private final Activity' not in source, 'weak UI ownership')
for guard in ('section.getTag() instanceof Controller','if (!attached)', 'if (refreshing || !attached)',
              'registerOnSharedPreferenceChangeListener(this)', 'unregisterOnSharedPreferenceChangeListener(this)',
              'SettingsNavigation.findActivity(view.getContext())'):
    check(guard in source, 'lifecycle guard '+guard)
for forbidden in ('Handler', 'Timer', 'postDelayed', 'Log.', 'Lum10', 'fm10', 'RxViewDelegate', 'getSharedPreferences("BTTV_QUICK"'):
    check(forbidden not in source, 'no polling/native draft/diagnostic/new storage: '+forbidden)

stock = ROOT/'.bttv-cache/disass'
if stock.exists():
    def read(name): return (stock/name).read_text()
    evidence = {
        'smali_classes2/tmx.smali': ('Lult;->q:Landroid/widget/ImageView;', 'const/4 v1, 0x4', 'Lkmx;-><init>'),
        'smali_classes2/kmx.smali': ('sget-object p1, Lpmx;->a:Lpmx;', 'RxViewDelegate;->u('),
        'smali_classes2/cmx.smali': ('instance-of v6, p1, Lpmx;', '"player_settings_button"', 'Lxlx;->a:Lxlx;'),
        'smali/um10.smali': ('const-class p4, Lxlx;', 'Lyl10;-><init>', 'DisposeOn;->VIEW_DETACHED'),
        'smali/yl10.smali': ('check-cast p1, Lxlx;', 'Lum10;->D2('),
        'smali/bo10.smali': ('Lznj;->n(Landroidx/fragment/app/n;)Lao10;',),
        'smali_classes5/znj.smali': ('const v1, 0x7f0e0470', 'Lao10;-><init>'),
        'smali_classes5/am10.smali': ('instance-of v11, v9, Len10;', 'Lfm10;->l:Z', '"do_not_disturb_mode"'),
    }
    for path, snippets in evidence.items():
        text=read(path)
        for snippet in snippets: check(snippet in text, 'gear/Apply evidence '+path+': '+snippet)
    # Work only on disposable copies, not the proven cached disassembly.
    (ROOT/'.tmp').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='quick-settings-', dir=ROOT/'.tmp') as temp:
        temp=Path(temp)
        for name in ('smali_classes5/ao10.smali','res/layout/stream_settings_fragment.xml'):
            target=temp/name
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(read(name),encoding='utf-8',newline='\n')
        for file in (ROOT/'patches/live/ao10.smali.patch', layout_patch):
            subprocess.run(['git','apply','--check',str(file)],cwd=temp,check=True)
            subprocess.run(['git','apply',str(file)],cwd=temp,check=True)
        original=ET.fromstring(read('res/layout/stream_settings_fragment.xml'))
        updated=ET.parse(temp/'res/layout/stream_settings_fragment.xml').getroot()
        section=next(e for e in updated.iter() if e.get(ANDROID+'id')=='@+id/bttv_quick_settings')
        parent=next(e for e in updated.iter() if section in list(e))
        index=list(parent).index(section)
        check(list(parent)[index+1].get(ANDROID+'id')=='@id/quality_options_header','before native Quality Options')
        check(list(parent)[index-2].get(ANDROID+'id')=='@id/chat_switching_shortcut_section','after Chat Switching Shortcut')
        check(len([e for e in section.iter() if e.tag=='androidx.appcompat.widget.SwitchCompat'])==4,'four native switches')
        check(len([e for e in section.iter() if e.get(ANDROID+'tag') in ('bttv.quick.highlights','bttv.quick.blacklist','bttv.quick.settings')])==3,'three actions')
        heading=list(section)[0]
        check(heading.tag.endswith('.Header') and heading.get('style')=='@style/StreamSectionHeaderStyle','native header')
        check(heading.get(ANDROID+'text')=='BTTV-ANDROID','exact brand heading')
        check(len(list(section))==8,'one heading + seven rows')
        parent.remove(section)
        def shape(e): return (e.tag,dict(e.attrib),[shape(c) for c in e])
        check(shape(updated)==shape(original),'all original native controls/order/Apply untouched')
        assembled=(temp/'smali_classes5/ao10.smali').read_text()
        check(assembled.replace('    invoke-static/range {p2 .. p2}, Lbttv/settings/StreamQuickSettings;->bind(Landroid/view/View;)V\n\n','')==read('smali_classes5/ao10.smali'),'single method-call delta, no register changes')
print('PASS Stage 5P-C static: verified gear/Options/Apply, native section, exact stock preservation, lifecycle/storage/actions, normal patches')
