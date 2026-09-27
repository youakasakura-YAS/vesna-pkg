# -*- coding: utf-8 -*-
import zipfile, os, hashlib, io, json

base = r'F:\Vesna-pkg\packages'
names = ['vesna-java', 'vesna-android', 'vesna-dev']
for n in names:
    d = os.path.join(base, n)
    zpath = os.path.join(d, n + '-1.0.0.zip')
    if os.path.exists(zpath):
        os.remove(zpath)
    with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED) as z:
        for f in ['vesna-pkg.json', n.split('-')[1] + '.ves', 'README.md']:
            fp = os.path.join(d, f)
            if os.path.exists(fp):
                z.write(fp, f)
    h = hashlib.sha256(open(zpath, 'rb').read()).hexdigest()
    print(n, 'zip', os.path.getsize(zpath), 'bytes, sha256', h)
