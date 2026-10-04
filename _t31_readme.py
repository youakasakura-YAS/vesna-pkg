# -*- coding: utf-8 -*-
import io

p = r'F:\Vesna-pkg\packages\vesna-mc\README.md'
s = io.open(p, encoding='utf-8').read()

old = '| `server_tick` | tick（秒） | 每秒一次（低频） |'
new = '''| `server_tick` | tick（秒） | 定时触发（`tick_interval` 可调，默认 20 tick） |
| `player_use_block` | player, block, x, y, z, hand | 使用/交互方块 |
| `player_use_item` | player, item, hand | 使用物品 |
| `player_respawn` | player, uuid, op | 玩家重生 |
| `entity_damage` | entity, attacker, amount | 实体受伤（可配 `min_interval` 节流） |
| `player_drop_item` | player, item, count | 丢弃物品 |'''
assert s.count(old) == 1, 'events'
s = s.replace(old, new)

old = '    {"type": "log", "text": "server log"}'
new = '''    {"type": "title", "player": "Alex", "title": "你好", "subtitle": "来自 Vesna"},
    {"type": "actionbar", "player": "Alex", "text": "动作栏消息"},
    {"type": "set_block", "x": 0, "y": 100, "z": 0, "block": "minecraft:diamond_block"},
    {"type": "summon", "entity": "minecraft:creeper", "x": 0, "y": 100, "z": 0},
    {"type": "spawn_particle", "particle": "minecraft:flame", "x": 0, "y": 100, "z": 0, "count": 20},
    {"type": "scoreboard", "player": "Alex", "objective": "kills", "score": 10},
    {"type": "log", "text": "server log"}'''
assert s.count(old) == 1, 'actions'
s = s.replace(old, new)

old = '脚本由 `config/vesna/events.json` 映射事件。`"resident": true` 启用常驻进程模式。生成项目内置以下事件：'
new = '脚本由 `config/vesna/events.json` 映射事件。`"resident": true` 启用常驻进程模式；顶层 `"tick_interval"`（默认 20 tick）控制 `server_tick` 频率；事件级 `"min_interval"`（秒）做高频节流；常驻模式下顶层 `state` dict 可在事件函数间共享数据。生成项目内置以下事件：'
assert s.count(old) == 1, 'intro'
s = s.replace(old, new)

s = s.replace('1.2.0', '1.3.0')
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('README updated')

p2 = r'F:\Vesna-pkg\packages\vesna-mc\vesna-pkg.json'
d = io.open(p2, encoding='utf-8').read()
d = d.replace('"version": "1.2.0"', '"version": "1.3.0"')
d = d.replace('resident process mode (long-lived vesna child, stdin/stdout JSON protocol, event functions _on_<event>)',
              'resident process mode, 16 events, 14 action types, tick_interval / min_interval throttling, shared state dict')
io.open(p2, 'w', encoding='utf-8', newline='\n').write(d)
print('vesna-pkg.json 1.3.0')
