# -*- coding: utf-8 -*-
import json, io, re

p = r'F:\Vesna-pkg\registry.json'
d = json.load(io.open(p, encoding='utf-8'))
if isinstance(d, dict):
    pk = d.get('packages', d)
else:
    pk = d
item = None
if isinstance(pk, dict):
    item = pk.get('vesna-mc')
else:
    for it in pk:
        if it.get('name') == 'vesna-mc':
            item = it
            break
if item is None:
    raise SystemExit('vesna-mc not found in registry')
old_ver = item.get('version')
item['version'] = '1.3.0'
item['checksum'] = '3826b4fd24e009b015e161366468f01ba8f494d7121e1c14e343b21a121c4135'
if 'url' in item:
    item['url'] = re.sub(r'vesna-mc-\d+\.\d+\.\d+\.zip', 'vesna-mc-1.3.0.zip', item['url'])
io.open(p, 'w', encoding='utf-8', newline='\n').write(json.dumps(d, ensure_ascii=False, indent=2))
print('registry vesna-mc:', old_ver, '-> 1.3.0')
