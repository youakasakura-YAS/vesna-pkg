# -*- coding: utf-8 -*-
import io, json, zipfile, hashlib, os

BASE = r'F:\Vesna-pkg'

def make_pkg(name, entry, version, desc, files, deps=None):
    d = os.path.join(BASE, 'packages', name)
    pkg = {"name": name, "version": version, "description": desc, "entry": entry,
           "dependencies": deps or {}}
    with io.open(os.path.join(d, 'vesna-pkg.json'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(json.dumps(pkg, ensure_ascii=False, indent=2))
    zpath = os.path.join(d, '%s-%s.zip' % (name, version))
    with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED) as z:
        for fn in files:
            z.write(os.path.join(d, fn), fn)
    chk = hashlib.sha256(open(zpath, 'rb').read()).hexdigest().lower()
    url = 'https://youakasakura-YAS.github.io/vesna-pkg/packages/%s/%s-%s.zip' % (name, name, version)
    entry = {"name": name, "version": version, "description": desc, "license": "MIT",
             "url": url, "checksum": chk, "dependencies": deps or {}}
    with io.open(os.path.join(d, 'registry-entry.json'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(json.dumps(entry, ensure_ascii=False, indent=2))
    print(name, 'zip', os.path.getsize(zpath), 'sha256', chk)
    return entry

e1 = make_pkg('vesna-gui', 'gui.ves', '1.0.0',
              'Web GUI framework for Vesna: serve HTML pages, static assets and JSON APIs via pure-Vesna HTTP over TCP',
              ['gui.ves', 'app.ves', 'README.md'])
e2 = make_pkg('vesna-email', 'email.ves', '1.0.0',
              'Email for Vesna: SMTP client (send mail, optional AUTH LOGIN) and a local mailbox server that saves .eml files',
              ['email.ves', 'README.md'])

reg_path = os.path.join(BASE, 'registry.json')
reg = json.load(io.open(reg_path, encoding='utf-8'))
names = {r['name'] for r in reg}
for e in (e1, e2):
    if e['name'] not in names:
        reg.append(e)
        print('registry +=', e['name'])
with io.open(reg_path, 'w', encoding='utf-8', newline='\n') as f:
    f.write(json.dumps(reg, ensure_ascii=False, indent=2))
print('registry entries:', len(reg))
