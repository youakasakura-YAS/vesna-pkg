# -*- coding: utf-8 -*-
import io, json

p = r'F:\Vesna-pkg\registry.json'
reg = json.load(io.open(p, encoding='utf-8'))
if 'vesna-mc' not in {x['name'] for x in reg}:
    reg.append({
        "name": "vesna-mc",
        "version": "1.0.0",
        "description": "Minecraft bridge mod generator for Vesna: builds complete Fabric / Forge / NeoForge mods (MC 1.20.1+) that run Vesna scripts on server/player events and commands",
        "author": "youakasakura-YAS",
        "license": "MIT",
        "keywords": ["minecraft", "mod", "fabric", "forge", "neoforge", "bridge"],
        "url": "https://youakasakura-YAS.github.io/vesna-pkg/packages/vesna-mc/vesna-mc-1.0.0.zip",
        "checksum": "30067d1fb84ad3a6eb406618cd6371ed6eca3b14a5386e00a699d18409f1d8c9",
        "dependencies": {}
    })
io.open(p, 'w', encoding='utf-8', newline='\n').write(json.dumps(reg, ensure_ascii=False, indent=2) + '\n')
print('registry entries:', len(reg))
