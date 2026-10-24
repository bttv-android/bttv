"""Offline, credential-free release-infrastructure regression contracts."""
import copy
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import warnings
import xml.etree.ElementTree as ET
import zipfile

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from prepare_twitch_input import inspect
from release_metadata import block
from stage_production_input import production_paths


class Infrastructure(unittest.TestCase):
    def test_quick_settings_string_registration(self):
        source=(ROOT/'mod/app/src/main/java/bttv/Res.java').read_text()
        body=re.search(r'public enum strings\s*\{([^}]+)\}',source,re.S).group(1)
        entries=[name.strip() for name in body.split(',') if name.strip()]
        self.assertEqual(len(entries),len(set(entries)))
        resources=ET.parse(ROOT/'mod/app/src/main/res/values/strings.xml').getroot()
        layout=(ROOT/'patches/live/res.layout.stream_settings_fragment.xml.patch').read_text()
        for name,value in [('bttv_quick_anonymous_chat','Anonymous Chat'),
                           ('bttv_quick_highlight_keywords','Highlight Keywords'),
                           ('bttv_quick_chat_blacklist','Chat Blacklist')]:
            with self.subTest(name=name):
                self.assertEqual(entries.count(name),1)
                matches=[node for node in resources.findall('string') if node.get('name')==name]
                self.assertEqual(len(matches),1)
                self.assertEqual(matches[0].text,value)
                self.assertEqual(layout.count('android:text="@string/'+name+'"'),1)

    def test_pr_checklist_documentation(self):
        changelog=(ROOT/'CHANGELOG.md').read_text()
        self.assertTrue(changelog.startswith('# Changelog\n\n# Unreleased (master)\n'))
        self.assertIn('Twitch 31.2.0',changelog.split('# 0.43.2-beta',1)[0])
        for name in ['PORTING/_NOTES.md','PORTING/PACKAGE_MIGRATION.md']:
            prefix='\n'.join((ROOT/name).read_text().splitlines()[:10])
            self.assertIn('**Historical porting record.**',prefix)
            self.assertIn('authoritative current Twitch 31.2.0 build process',prefix)
            for label,target in [('README','README.md'),('RELEASING.md','RELEASING.md')]:
                self.assertIn('['+label+'](../'+target+')',prefix)
                self.assertTrue((ROOT/name).parent.joinpath('..',target).resolve().is_file())

    def test_readme_original_assets_and_contributors(self):
        original = subprocess.check_output(['git', 'show', '14e991d2e9d0e708629e54ef9a66499e61d4d01d:README.md'], cwd=ROOT).decode()
        current = (ROOT/'README.md').read_text()
        self.assertEqual(original[:original.index('## Features')], current[:current.index('## Features')])
        gallery = original[original.index('## Contributors ✨'):]
        self.assertEqual(gallery, current[current.index('## Contributors ✨'):current.index('\n## Acknowledgments')])
        for line in original.splitlines():
            if re.match(r'^\[[^]]+\]: ', line):
                self.assertIn(line, current)
        self.assertIn('31.2.0 (3102006)', current)
        self.assertNotIn('Watch Parties', current)
        self.assertNotIn('C:\\BTTV', current)
        self.assertNotIn('Stage 8', current)

    def test_notices_and_pinned_source(self):
        doc=(ROOT/'THIRD_PARTY_NOTICES.md').read_text()
        raw=(ROOT/'mod/app/src/main/res/raw/bttv_third_party_notices.txt').read_text()
        for value in ['Copyright 2017-2018 zjupure', 'Copyright 2013-2018 Cave Rock Software Ltd',
                      'E8B273972EB5A70E888BD3E7D7C5B9B04E600A59D69DEF9136A74D70AE6FCDD3',
                      '4984568367caaf359b82c452bd28b5e192824d1c', 'bttv_apksig_license.txt']:
            self.assertIn(value, doc)
            self.assertIn(value, raw)
        for value in ['Copyright 2014 Google', 'Copyright 2012 Jake Wharton',
                      'Copyright (c) 2013 Xcellent Creations', 'Copyright (c) 2010, Google Inc.',
                      'Additional IP Rights Grant', 'The Public Suffix List']:
            self.assertIn(value, raw)

    def test_production_selection_excludes_diagnostics(self):
        paths=production_paths(ROOT)
        self.assertIn('mod/app/src/main/java/bttv/AnonChat.java', paths)
        self.assertIn('mod/app/src/main/java/bttv/updater/AutoUpdateChecker.java', paths)
        self.assertFalse(any(p.startswith(('diagnostics/','patches/notifications_', 'patches/purchase_ui_diag/')) for p in paths))
        for path in ['../escape', '/tmp/escape', 'diagnostics/not-allowed']:
            with self.assertRaises(ValueError): production_paths(ROOT, [path])

    def test_patch_syntax(self):
        paths=subprocess.check_output(['git','ls-files','patches'],cwd=ROOT).decode().splitlines()
        patches=[p for p in paths if p.endswith('.patch')]
        self.assertGreater(len(patches), 50)
        for path in patches:
            with self.subTest(path=path):
                subprocess.run(['git','apply','--numstat',path],cwd=ROOT,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)

    def test_deleted_tracked_input_is_excluded(self):
        def fixture(root,*args):
            return 'deleted.java\0' if '--deleted' in args else 'retained.java\0deleted.java\0'
        with patch('stage_production_input.git',side_effect=fixture):
            self.assertEqual(production_paths(ROOT),['retained.java'])

    def test_workflow_secret_and_publication_boundaries(self):
        ci=(ROOT/'.github/workflows/ci.yml').read_text()
        docker=(ROOT/'.github/workflows/docker.yml').read_text()
        checks=ci[ci.index('  checks:'):ci.index('  signed_candidate:')]
        self.assertNotIn('secrets.',checks)
        self.assertIn('fetch-depth: 0',checks)
        self.assertIn('git diff --check "$BASE_SHA...HEAD"',checks)
        self.assertNotIn('secrets.',docker)
        self.assertIn("github.event_name == 'workflow_dispatch'",ci)
        self.assertIn("github.ref == 'refs/heads/master'",ci)
        self.assertNotIn('contents: write',ci+docker)
        self.assertNotIn('pull_request_target',ci+docker)
        self.assertNotIn('actions/create-release',ci+docker)
        self.assertNotIn('twitch-19.0.1',ci+docker)

    def test_builder_isolation_and_failure_propagation(self):
        compose=(ROOT/'compose.yaml').read_text()
        self.assertIn('.:/source:ro',compose)
        self.assertNotIn('.bttv-cache',compose)
        self.assertIn('set -e\n',(ROOT/'build').read_text())
        self.assertIn('set -e\n',(ROOT/'buildsource').read_text())
        self.assertIn('30.0.3',(ROOT/'Dockerfile').read_text())
        self.assertEqual((ROOT/'.dockerignore').read_text().splitlines()[0],'**')

    @unittest.skipUnless(os.name=='posix' and shutil.which('bash') and shutil.which('jq'), 'Linux Bash/jq fixture; run separately in the builder')
    def test_tool_failure_propagation_and_signing_argument_boundaries(self):
        with tempfile.TemporaryDirectory(prefix='bttv-shell-fixture-') as temp:
            root=Path(temp);project=root/'project'
            (project/'dist').mkdir(parents=True)
            (project/'res/layout').mkdir(parents=True)
            (project/'AndroidManifest.xml').write_text('<manifest android:versionCode="1" android:versionName="1"/>')
            (project/'res/layout/settings_logout_footer.xml').write_text('<TextView android:text="BTTV old"/>')
            (root/'bttv.manifest.json').write_text('{"version":"0.43.2-beta","versionCode":"1208000338"}')
            java=root/'fake-java'
            java.write_text('''#!/bin/bash
case "$*" in
 *apktool.jar*)
   if [ "$FIXTURE_STAGE" = apktool ]; then exit 7; fi
   printf fixture > ./dist/twitch.apk
   exit 0;;
 *signer.jar*)
   printf '%s\\0' "$@" > "$FIXTURE_ARGS"
   exit 9;;
esac
exit 99
''')
            java.chmod(0o755)
            env=dict(os.environ,JAVA_PATH=str(java),APKTOOL_PATH='apktool.jar',UBER_APK_SIGNER_PATH='signer.jar',
                KS=str(root/'external key.jks'),KSPASS='fixture pass words',FIXTURE_ARGS=str(root/'args'),FIXTURE_STAGE='apktool')
            first=subprocess.run(['bash',str(ROOT/'build'),str(project)],cwd=root,env=env,capture_output=True)
            self.assertEqual(first.returncode,7);self.assertFalse((root/'args').exists())
            self.assertNotIn(b'Done:',first.stdout)
            env['FIXTURE_STAGE']='signer'
            second=subprocess.run(['bash',str(ROOT/'build'),str(project)],cwd=root,env=env,capture_output=True)
            self.assertEqual(second.returncode,9);self.assertNotIn(b'Done:',second.stdout)
            args=(root/'args').read_bytes().decode().split('\0')[:-1]
            self.assertEqual(args[args.index('--ks')+1],env['KS'])
            self.assertEqual(args[args.index('--ksPass')+1],env['KSPASS'])
            self.assertEqual(args[args.index('--ksKeyPass')+1],env['KSPASS'])

    @unittest.skipUnless(os.name=='posix' and shutil.which('bash') and shutil.which('jq'), 'Linux Bash/jq fixture; run separately in the builder')
    def test_signed_artifact_replacement_success_and_failures(self):
        for custom_key in [False, True]:
            for failure, expected in [('', 0), ('remove', 17), ('move', 19), ('signer', 9)]:
                with self.subTest(custom_key=custom_key, failure=failure or 'success'):
                    with tempfile.TemporaryDirectory(prefix='bttv replacement fixture ') as temp:
                        root=Path(temp);project=root/'project with spaces';bin_dir=root/'bin'
                        (project/'dist').mkdir(parents=True);(project/'res/layout').mkdir(parents=True)
                        bin_dir.mkdir()
                        (project/'AndroidManifest.xml').write_text('<manifest android:versionCode="1" android:versionName="1"/>')
                        (project/'res/layout/settings_logout_footer.xml').write_text('<TextView android:text="BTTV old"/>')
                        (root/'bttv.manifest.json').write_text('{"version":"0.43.2-beta","versionCode":"1208000338"}')
                        tools={
                            'fixture-java': '''#!/bin/bash
case "$*" in
 *apktool.jar*) printf 'unsigned fixture, not an APK' > ./dist/twitch.apk;;
 *signer.jar*)
   printf '%s\\0' "$@" > "$FIXTURE_ARGS"
   if [ "$FIXTURE_FAIL" = signer ]; then echo 'fixture signing failed' >&2; exit 9; fi
   if [ -z "$KS" ]; then target=./dist/twitch-aligned-debugSigned.apk; else target=./dist/twitch-aligned-signed.apk; fi
   printf 'signed fixture, not an APK' > "$target";;
 *) exit 99;;
esac
''',
                            'rm': '''#!/bin/bash
if [ "$FIXTURE_FAIL" = remove ]; then echo 'fixture removal failed' >&2; exit 17; fi
exec /bin/rm "$@"
''',
                            'mv': '''#!/bin/bash
printf called > "$FIXTURE_MOVE"
if [ "$FIXTURE_FAIL" = move ]; then echo 'fixture move failed' >&2; exit 19; fi
exec /bin/mv "$@"
''',
                        }
                        for name, contents in tools.items():
                            path=bin_dir/name;path.write_text(contents);path.chmod(0o755)
                        env=dict(os.environ,PATH=str(bin_dir)+os.pathsep+os.environ['PATH'],JAVA_PATH='fixture-java',
                            APKTOOL_PATH='apktool.jar',UBER_APK_SIGNER_PATH='signer.jar',
                            KS=str(root/'external key.jks') if custom_key else '',KSPASS='fixture pass words',
                            FIXTURE_ARGS=str(root/'args'),FIXTURE_FAIL=failure,FIXTURE_MOVE=str(root/'move-called'))
                        result=subprocess.run(['bash',str(ROOT/'build'),str(project)],cwd=root,env=env,capture_output=True)
                        self.assertEqual(result.returncode,expected,result.stderr.decode())
                        self.assertEqual(b'Done:' in result.stdout,not failure)
                        if failure in ('remove', 'signer'):
                            self.assertFalse((root/'move-called').exists())
                            self.assertEqual((project/'dist/twitch.apk').read_text(),'unsigned fixture, not an APK')
                        elif failure == 'move':
                            self.assertTrue((root/'move-called').exists())
                            self.assertFalse((project/'dist/twitch.apk').exists())
                        else:
                            self.assertEqual((project/'dist/twitch.apk').read_text(),'signed fixture, not an APK')
                        args=(root/'args').read_bytes().decode().split('\0')[:-1]
                        if custom_key:
                            self.assertEqual(args[args.index('--ks')+1],env['KS'])
                            self.assertEqual(args[args.index('--ksAlias')+1],'bttv-android')
                            self.assertEqual(args[args.index('--ksPass')+1],env['KSPASS'])
                            self.assertEqual(args[args.index('--ksKeyPass')+1],env['KSPASS'])
                        else:
                            self.assertNotIn('--ks',args)

    def test_manual_tool_path_limitation_documented(self):
        for name in ['README.md', 'RELEASING.md']:
            text=(ROOT/name).read_text()
            self.assertIn('SDK and build-tool installation paths without\nspaces or other whitespace',text)
        readme=(ROOT/'README.md').read_text()
        self.assertLess(readme.index('SDK and build-tool installation paths without'),readme.index('sdkmanager --sdk_root='))


class InputValidation(unittest.TestCase):
    def setUp(self):
        parent=ROOT/'.tmp';parent.mkdir(exist_ok=True)
        self.temp=tempfile.TemporaryDirectory(prefix='stage9-test-',dir=parent)
        self.assertEqual(Path(self.temp.name).resolve().parent,parent.resolve())
        self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)
        buffer=io.BytesIO()
        with zipfile.ZipFile(buffer,'w') as z:z.writestr('AndroidManifest.xml',b'fixture only')
        self.apk=buffer.getvalue()
        self.pin=self.root/'pins.json'
        self.pin.write_text(json.dumps({'files':{'base.apk':hashlib.sha256(self.apk).hexdigest().upper()}}))

    def bundle(self, names):
        target=self.root/'input.zip'
        with warnings.catch_warnings():
            warnings.simplefilter('ignore',UserWarning)
            with zipfile.ZipFile(target,'w') as z:
                for name,data in names:z.writestr(name,data)
        return target

    def test_exact_payload_accepted(self):
        self.assertEqual(len(inspect(self.bundle([('base.apk',self.apk)]),self.pin)),1)

    def test_different_variant_rejected(self):
        with self.assertRaises(ValueError):inspect(self.bundle([('base.apk',self.apk+b'x')]),self.pin)

    def test_nested_or_missing_payload_rejected(self):
        with self.assertRaises(ValueError):inspect(self.bundle([('nested/base.apk',self.apk)]),self.pin)

    def test_duplicate_payload_rejected(self):
        with self.assertRaises(ValueError):inspect(self.bundle([('base.apk',self.apk),('base.apk',self.apk)]),self.pin)

    def test_hybrid_uses_only_pinned_armv7_from_secondary(self):
        v7='split_config.armeabi_v7a.apk'
        self.pin.write_text(json.dumps({'files':{'base.apk':hashlib.sha256(self.apk).hexdigest().upper(),v7:hashlib.sha256(self.apk).hexdigest().upper()}}))
        secondary=self.root/'secondary.apkm'
        with zipfile.ZipFile(secondary,'w') as z:
            z.writestr('base.apk',b'not selected')
            z.writestr(v7,self.apk)
        found=inspect(self.bundle([('base.apk',self.apk)]),self.pin,secondary)
        self.assertEqual(set(found),{'base.apk',v7})

    def test_hybrid_rejects_unvalidated_armv7(self):
        v7='split_config.armeabi_v7a.apk'
        self.pin.write_text(json.dumps({'files':{'base.apk':hashlib.sha256(self.apk).hexdigest().upper(),v7:'0'*64}}))
        secondary=self.root/'secondary.apkm'
        with zipfile.ZipFile(secondary,'w') as z:z.writestr(v7,self.apk)
        with self.assertRaises(ValueError):inspect(self.bundle([('base.apk',self.apk)]),self.pin,secondary)


class Metadata(unittest.TestCase):
    def setUp(self):
        self.proof=dict(packageName='tv.twitch.bttvandroid.app',versionName='1.2.3',versionCode=123,
            testSigningIdentity=False,standalone=True,signerSha256='A'*64,minSdk=29,
            supportedAbis=['arm64-v8a','armeabi-v7a'],twitchVersion='31.2.0',sizeBytes=100,sha256='B'*64)
        self.release=dict(tag_name='v1.2.3',draft=True,prerelease=False,assets=[dict(id=12,name='bttv.apk',
            state='uploaded',content_type='application/vnd.android.package-archive',size=100,
            browser_download_url='https://github.com/bttv-android/bttv/releases/download/v1.2.3/bttv.apk',digest='sha256:'+'b'*64)])

    def test_actual_asset_bound_to_block(self):
        text=block(self.proof,self.release,122)
        self.assertTrue(text.startswith('```bttv-update\n'))
        obj=json.loads(text.split('\n',1)[1].rsplit('\n```',1)[0])
        self.assertEqual(obj['assetId'],12);self.assertEqual(obj['signerSha256'],'A'*64)
        self.assertNotIn('testSigningIdentity',obj)

    def test_debug_identity_rejected(self):
        self.proof['testSigningIdentity']=True
        with self.assertRaises(ValueError):block(self.proof,self.release,122)

    def test_wrong_package_rejected(self):
        self.proof['packageName']='tv.twitch.android.app'
        with self.assertRaisesRegex(ValueError,'non-debug release identity'):block(self.proof,self.release,122)

    def test_invalid_uploaded_asset_identity_rejected(self):
        for change in [dict(id=0),dict(id=True),dict(state='new')]:
            with self.subTest(change=change):
                release=copy.deepcopy(self.release);release['assets'][0].update(change)
                with self.assertRaisesRegex(ValueError,'Invalid uploaded asset identity'):block(self.proof,release,122)

    def test_filename_exactly_180_accepted(self):
        asset=self.release['assets'][0];name='a'*176+'.apk'
        asset.update(name=name,browser_download_url='https://github.com/bttv-android/bttv/releases/download/v1.2.3/'+name)
        self.assertEqual(len(name),180)
        self.assertEqual(json.loads(block(self.proof,self.release,122).split('\n',1)[1].rsplit('\n```',1)[0])['assetName'],name)

    def test_filename_181_rejected(self):
        asset=self.release['assets'][0];name='a'*177+'.apk'
        asset.update(name=name,browser_download_url='https://github.com/bttv-android/bttv/releases/download/v1.2.3/'+name)
        with self.assertRaisesRegex(ValueError,'at most 180 characters'):block(self.proof,self.release,122)

    def test_exactly_64_total_assets_accepted(self):
        self.release['assets'].extend(dict(content_type='application/octet-stream') for _ in range(63))
        self.assertEqual(len(self.release['assets']),64)
        self.assertTrue(block(self.proof,self.release,122).startswith('```bttv-update\n'))

    def test_65_total_assets_rejected(self):
        self.release['assets'].extend(dict(content_type='application/octet-stream') for _ in range(64))
        with self.assertRaisesRegex(ValueError,'at most 64 total assets'):block(self.proof,self.release,122)

    def test_reused_code_rejected(self):
        with self.assertRaises(ValueError):block(self.proof,self.release,123)

    def test_mismatched_version_rejected(self):
        self.release['tag_name']='v1.2.4'
        with self.assertRaises(ValueError):block(self.proof,self.release,122)

    def test_ambiguous_asset_rejected(self):
        self.release['assets'].append(copy.deepcopy(self.release['assets'][0]))
        with self.assertRaises(ValueError):block(self.proof,self.release,122)

    def test_wrong_hash_rejected(self):
        self.release['assets'][0]['digest']='sha256:'+'c'*64
        with self.assertRaises(ValueError):block(self.proof,self.release,122)

    def test_wrong_size_rejected(self):
        self.release['assets'][0]['size']=101
        with self.assertRaises(ValueError):block(self.proof,self.release,122)

    def test_untrusted_origin_rejected(self):
        self.release['assets'][0]['browser_download_url']='https://example.com/bttv.apk'
        with self.assertRaises(ValueError):block(self.proof,self.release,122)

    def test_prerelease_excluded(self):
        self.release['prerelease']=True
        with self.assertRaises(ValueError):block(self.proof,self.release,122)


if __name__=='__main__':unittest.main(verbosity=2)
