# -*- coding: utf-8 -*-
import io, os

tmpl = r'F:\Vesna-pkg\packages\vesna-mc\templates'

# 1) events.json：tick_interval + 5 新事件 + 节流示例
ev = io.open(os.path.join(tmpl, 'events.json'), encoding='utf-8').read()
old = '''{
  "resident": true,
  "server_started": { "script": "server_started.ves" },'''
new = '''{
  "resident": true,
  "tick_interval": 20,
  "server_started": { "script": "server_started.ves" },'''
assert ev.count(old) == 1, 'ev anchor'
ev = ev.replace(old, new)

old = '''  "server_tick": { "script": "server_tick.ves" },'''
new = '''  "server_tick": { "script": "server_tick.ves", "min_interval": 1 },
  "player_use_block": { "script": "player_use_block.ves" },
  "player_use_item": { "script": "player_use_item.ves" },
  "player_respawn": { "script": "player_respawn.ves" },
  "entity_damage": { "script": "entity_damage.ves", "min_interval": 1 },
  "player_drop_item": { "script": "player_drop_item.ves" },'''
assert ev.count(old) == 1, 'ev anchor2'
ev = ev.replace(old, new)
io.open(os.path.join(tmpl, 'events.json'), 'w', encoding='utf-8', newline='\n').write(ev)
print('events.json updated')

# 2) 5 个新事件模板
files = {
 'player_use_block.ves': '''/* 使用/交互方块事件：payload 含 player / block / x / y / z / hand */
args = #args(),
payload = #json_decode(args['1']),
player = payload["player"],
block = payload["block"],
print(#json_encode({"actions": [{"type": "log"; "text": player + " 交互了 " + block}]})),
''',
 'player_use_item.ves': '''/* 使用物品事件：payload 含 player / item / hand */
args = #args(),
payload = #json_decode(args['1']),
player = payload["player"],
item = payload["item"],
print(#json_encode({"actions": [{"type": "log"; "text": player + " 使用了 " + item}]})),
''',
 'player_respawn.ves': '''/* 玩家重生事件：payload 含 player / uuid / op */
args = #args(),
payload = #json_decode(args['1']),
player = payload["player"],
print(#json_encode({"actions": [{"type": "message"; "target": "all"; "text": player + " 重生了！"}]})),
''',
 'entity_damage.ves': '''/* 实体受伤事件：payload 含 entity / attacker / amount */
args = #args(),
payload = #json_decode(args['1']),
entity = payload["entity"],
attacker = payload["attacker"],
amount = payload["amount"],
print(#json_encode({"actions": [{"type": "log"; "text": entity + " 受到 " + #str(amount) + " 伤害（来源 " + attacker + "）"}]})),
''',
 'player_drop_item.ves': '''/* 玩家丢弃物品事件：payload 含 player / item / count */
args = #args(),
payload = #json_decode(args['1']),
player = payload["player"],
item = payload["item"],
print(#json_encode({"actions": [{"type": "log"; "text": player + " 丢弃了 " + item}]})),
''',
}
for name, content in files.items():
    io.open(os.path.join(tmpl, name), 'w', encoding='utf-8', newline='\n').write(content)
    print('written', name)
