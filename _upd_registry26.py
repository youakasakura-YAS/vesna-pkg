# -*- coding: utf-8 -*-
import io, json

p = r'F:\Vesna-pkg\registry.json'
reg = json.load(io.open(p, encoding='utf-8'))
existing = {x['name'] for x in reg}
new_entries = [
    {
        "name": "vesna-java",
        "version": "1.0.0",
        "description": "Java interop and Minecraft mod tooling for Vesna: environment check, run classes, Java/Gradle/Maven scaffolding, Fabric/Forge/NeoForge mod skeletons",
        "author": "youakasakura-YAS",
        "license": "MIT",
        "keywords": ["java", "minecraft", "mod", "gradle", "maven"],
        "url": "https://youakasakura-YAS.github.io/vesna-pkg/packages/vesna-java/vesna-java-1.0.0.zip",
        "checksum": "3044a1c15c836edc27eabdd910ff9be2be4e48f62f1868065d438e0250ef5a0b",
        "dependencies": {}
    },
    {
        "name": "vesna-android",
        "version": "1.0.0",
        "description": "Android development toolkit for Vesna: full project scaffolding (Gradle, Manifest, Activity, resources), build helper and signing info",
        "author": "youakasakura-YAS",
        "license": "MIT",
        "keywords": ["android", "gradle", "mobile", "app"],
        "url": "https://youakasakura-YAS.github.io/vesna-pkg/packages/vesna-android/vesna-android-1.0.0.zip",
        "checksum": "6eaeec43508bd9a7cfe5ca4eb26800ce18a7038d92cf785f118c344a9a616a02",
        "dependencies": {}
    },
    {
        "name": "vesna-dev",
        "version": "1.0.0",
        "description": "General software development toolkit for Vesna: project scaffolding, README/LICENSE/.gitignore templates, semver increment, changelog, TODO scan, line counting",
        "author": "youakasakura-YAS",
        "license": "MIT",
        "keywords": ["dev", "scaffold", "semver", "changelog", "tools"],
        "url": "https://youakasakura-YAS.github.io/vesna-pkg/packages/vesna-dev/vesna-dev-1.0.0.zip",
        "checksum": "26e8f7937bdc3450994bb71b9855135c26149773f0fc44a4b349c9c3cb5932f2",
        "dependencies": {}
    },
]
for e in new_entries:
    if e['name'] not in existing:
        reg.append(e)
io.open(p, 'w', encoding='utf-8', newline='\n').write(json.dumps(reg, ensure_ascii=False, indent=2) + '\n')
print('registry entries:', len(reg))
