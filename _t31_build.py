# -*- coding: utf-8 -*-
import zipfile, os, hashlib, json, subprocess, shutil

base = r'F:\Vesna-pkg\packages'
n = 'vesna-mc'
d = os.path.join(base, n)
zpath = os.path.join(d, n + '-1.3.0.zip')
if os.path.exists(zpath):
    os.remove(zpath)
for old in ['vesna-mc-1.1.0.zip', 'vesna-mc-1.2.0.zip', 'vesna-mc-1.0.0.zip']:
    o = os.path.join(d, old)
    if os.path.exists(o):
        os.remove(o)
with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(d):
        for f in files:
            if f.endswith('.zip'):
                continue
            fp = os.path.join(root, f)
            rel = os.path.relpath(fp, d)
            z.write(fp, rel)
h = hashlib.sha256(open(zpath, 'rb').read()).hexdigest()
print(n, 'zip', os.path.getsize(zpath), 'bytes, sha256', h)

home = r'F:\Vesna\_pkghome'
mc = os.path.join(home, 'packages', 'vesna-mc')
if os.path.exists(mc):
    shutil.rmtree(mc)
env = dict(os.environ)
env['VESNA_HOME'] = home
r = subprocess.run([r'F:\Vesna\src\cpp\vesna_test.exe', '--pkg', 'install', zpath],
                   capture_output=True, env=env)
print('install:', r.stdout.decode('utf-8', 'replace').strip().splitlines()[-1])
