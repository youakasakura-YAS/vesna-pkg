# -*- coding: utf-8 -*-
import subprocess, json

exe = r'F:\Vesna\src\cpp\vesna_test.exe'
d = r'F:\Vesna-pkg\_t31_fabric\config\vesna\scripts'

def ask(event, payload):
    req = {'id': 1, 'event': event, 'payload': payload}
    proc = subprocess.Popen([exe, 'resident.ves'], cwd=d,
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)
    proc.stdin.write((json.dumps(req, ensure_ascii=False) + '\n').encode('utf-8'))
    proc.stdin.close()
    out, err = proc.communicate(timeout=15)
    if err:
        return 'ERR ' + err.decode('utf-8', 'replace').strip()
    lines = [l for l in out.decode('utf-8', 'replace').splitlines() if l.strip()]
    return lines[-1] if lines else ''

cases = [
    ('server_started', {}, True),
    ('player_join', {'player': 'Alex', 'uuid': 'abc', 'op': True}, True),
    ('player_death', {'player': 'Alex', 'uuid': 'abc', 'op': True, 'message': 'x'}, True),
    ('player_kill', {'player': 'Alex', 'uuid': 'abc', 'op': True, 'victim': 'Zombie'}, True),
    ('block_break', {'player': 'Alex', 'uuid': 'abc', 'op': True, 'block': 'minecraft:stone', 'x': 1, 'y': 2, 'z': 3}, True),
    ('player_chat', {'player': 'Alex', 'uuid': 'abc', 'op': True, 'message': 'hello world'}, True),
    ('player_chat', {'player': 'Bob', 'uuid': 'd', 'op': False, 'message': 'hi'}, False),
    ('player_advancement', {'player': 'Alex', 'uuid': 'abc', 'op': True, 'advancement': 'a'}, True),
    ('server_tick', {'tick': 300}, True),
    ('server_tick', {'tick': 150}, False),
    ('timer_example', {}, True),
    # 新事件
    ('player_use_block', {'player': 'Alex', 'uuid': 'abc', 'op': True, 'block': 'minecraft:crafting_table', 'x': 1, 'y': 2, 'z': 3, 'hand': 'main_hand'}, True),
    ('player_use_item', {'player': 'Alex', 'uuid': 'abc', 'op': True, 'item': 'minecraft:diamond', 'hand': 'main_hand'}, True),
    ('player_respawn', {'player': 'Alex', 'uuid': 'abc', 'op': True}, True),
    ('entity_damage', {'entity': 'Alex', 'attacker': 'Zombie', 'amount': 4.0}, True),
    ('player_drop_item', {'player': 'Alex', 'uuid': 'abc', 'op': True, 'item': 'minecraft:stick', 'count': 2}, True),
    ('no_such_event', {}, False),
]
ok = 0
for event, payload, expect_out in cases:
    line = ask(event, payload)
    good = True
    parsed = None
    try:
        parsed = json.loads(line)
    except Exception:
        good = False
    if good and 'result' not in parsed:
        good = False
    if good:
        r = parsed.get('result')
        has_out = isinstance(r, dict) and ('message' in r or 'actions' in r)
        if expect_out != has_out:
            good = False
        if event == 'no_such_event':
            good = 'error' in r
    ok += 1 if good else 0
    print(('PASS' if good else 'FAIL'), event, '->', line[:100])
print('----')
print('pass', ok, '/', len(cases))
