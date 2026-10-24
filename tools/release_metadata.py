"""Render updater metadata from an independently verified APK and actual asset JSON.

Does not create a release, upload an asset, change a tag or call any remote API.
"""
import argparse
import json
from pathlib import Path
import re
from verify_release_apk import verify

PACKAGE = 'tv.twitch.bttvandroid.app'


def block(proof, release, previous_code):
    tag = release.get('tag_name')
    version = proof['versionName']
    if not re.fullmatch(r'v[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.-]+)?', tag or '') or tag != 'v' + version:
        raise ValueError('Release tag must exactly match the actual APK version')
    if release.get('prerelease') is not False or not isinstance(release.get('draft'), bool):
        raise ValueError('This updater channel excludes prereleases; draft may be prepared but not discovered')
    if proof['packageName'] != PACKAGE or proof.get('testSigningIdentity') is not False:
        raise ValueError('An approved non-debug release identity is required')
    if type(previous_code) is not int or previous_code < 1 or proof['versionCode'] <= previous_code:
        raise ValueError('Release versionCode must exceed the explicit previous supported code')
    release_assets = release.get('assets', [])
    if len(release_assets) > 64:
        raise ValueError('Release must contain at most 64 total assets for updater compatibility')
    assets = [x for x in release_assets if x.get('content_type') == 'application/vnd.android.package-archive']
    if len(assets) != 1:
        raise ValueError('Exactly one uploaded APK asset required')
    asset = assets[0]
    name, asset_id = asset.get('name', ''), asset.get('id')
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]*\.apk', name) or asset.get('state') != 'uploaded' or type(asset_id) is not int or asset_id <= 0:
        raise ValueError('Invalid uploaded asset identity')
    if len(name) > 180:
        raise ValueError('APK asset filename must be at most 180 characters for updater compatibility')
    if asset.get('size') != proof['sizeBytes']:
        raise ValueError('Uploaded asset size differs from verified APK')
    url = 'https://github.com/bttv-android/bttv/releases/download/' + tag + '/' + name
    if asset.get('browser_download_url') != url:
        raise ValueError('Unexpected asset download URL')
    digest = asset.get('digest')
    if digest and digest.lower() != 'sha256:' + proof['sha256'].lower():
        raise ValueError('Uploaded digest differs from verified APK')
    metadata = {key: proof[key] for key in ['standalone', 'packageName', 'versionName', 'versionCode',
                 'signerSha256', 'minSdk', 'supportedAbis', 'twitchVersion', 'sizeBytes', 'sha256']}
    metadata.update(schemaVersion=1, releaseTag=tag, assetId=asset_id, assetName=name)
    return '```bttv-update\n' + json.dumps(metadata, indent=2) + '\n```\n'


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--apk', required=True, type=Path)
    p.add_argument('--input', required=True, type=Path)
    p.add_argument('--sdk', type=Path)
    p.add_argument('--expected-signer', required=True)
    p.add_argument('--release-json', required=True, type=Path)
    p.add_argument('--previous-code', required=True, type=int)
    p.add_argument('--output', required=True, type=Path)
    a = p.parse_args()
    proof = verify(a.apk, a.input, a.sdk, a.expected_signer)
    text = block(proof, json.loads(a.release_json.read_text()), a.previous_code)
    with a.output.open('x', encoding='utf-8') as stream:
        stream.write(text)
