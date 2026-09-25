#!/usr/bin/env python3
"""Collect the Maven mirror: fetch every artifact in manifest.json into a
plain Maven-layout tree and verify each file against its pinned sha256.

Sources are tried in the order recorded per artifact: Maven Central first
(covers stock releases like the JAXB 2.3.2 line), then the signerry/aarmam
GitHub Packages registries (fork rebuilds that exist nowhere public without
auth). Any valid GitHub token reads these packages - they are all public -
so the workflow's own GITHUB_TOKEN suffices.

Output: $MIRROR_SITE_DIR/maven/<group-path>/<module>/<version>/<file>
(default: <repo>/.github/maven-mirror/site/maven/...) ready for GitHub Pages.

Exits non-zero if any artifact cannot be resolved and verified, so a mirror
seed run can never half-succeed.
"""
import hashlib
import json
import os
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
MANIFEST = os.path.join(HERE, 'manifest.json')
SITE_DIR = os.environ.get('MIRROR_SITE_DIR',
                          os.path.join(HERE, 'site'))
MAVEN_ROOT = os.path.join(SITE_DIR, 'maven')

CENTRAL = 'https://repo1.maven.org/maven2'
TOKEN = os.environ.get('GITHUB_TOKEN', '')


def sha256_bytes(data):
    return hashlib.sha256(data).hexdigest()


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, 'rb') as fh:
        for chunk in iter(lambda: fh.read(1 << 16), b''):
            h.update(chunk)
    return h.hexdigest()


def fetch(url, auth):
    req = urllib.request.Request(url)
    if auth:
        req.add_header('Authorization', f'Bearer {TOKEN}')
    req.add_header('User-Agent', 'eudi-wallet-poc-maven-mirror')
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.read()
    except Exception:
        return None


def source_url(name, path):
    if name == 'central':
        return CENTRAL + '/' + path, False
    if name.startswith('gpr:'):
        return f'https://maven.pkg.github.com/{name[4:]}/{path}', True
    raise ValueError(f'unknown source {name!r}')


def main():
    with open(MANIFEST) as f:
        manifest = json.load(f)
    artifacts = manifest['artifacts']
    print(f'{len(artifacts)} artifacts in manifest')

    fetched = cached = 0
    failures = []
    for art in artifacts:
        dest = os.path.join(MAVEN_ROOT, art['path'])
        if os.path.exists(dest) and sha256_file(dest) == art['sha256']:
            cached += 1
            continue
        got = None
        for name in art['sources']:
            url, auth = source_url(name, art['path'])
            data = fetch(url, auth)
            if data and sha256_bytes(data) == art['sha256']:
                got = data
                print(f'  {name:24s} {art["path"]}')
                break
        if got is None:
            failures.append(art['path'])
            continue
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, 'wb') as fh:
            fh.write(got)
        fetched += 1

    print(f'fetched {fetched}, already present {cached}, failed {len(failures)}')
    if failures:
        print('FAILED to resolve:')
        for p in failures:
            print(f'  {p}')
        sys.exit(1)
    print(f'mirror ready under {MAVEN_ROOT}')


if __name__ == '__main__':
    main()
