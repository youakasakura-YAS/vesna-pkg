# -*- coding: utf-8 -*-
import zipfile, os, hashlib

base = r'F:\Vesna-pkg\packages'
names = ['vesna-mc']
for n in names:
    d = os.path.join(base, n)
    zpath = os.path.join(d, n + '-1.0.0.zip')
    if os.path.exists(zpath):
        os.remove(zpath)
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
    # list contents
    with zipfile.ZipFile(zpath) as z:
        for i in z.namelist():
            print('  ', i)
