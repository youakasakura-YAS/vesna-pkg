# -*- coding: utf-8 -*-
import io

p = r'F:\Vesna-pkg\packages\vesna-mc\mc.ves'
s = io.open(p, encoding='utf-8').read()

# 1) copy_config 脚本列表 +5
old = '["server_started.ves"; "server_stopped.ves"; "player_join.ves"; "player_leave.ves"; "player_death.ves"; "player_kill.ves"; "block_break.ves"; "block_place.ves"; "player_chat.ves"; "player_advancement.ves"; "server_tick.ves"; "timer_example.ves"; "hello.ves"]'
new = '["server_started.ves"; "server_stopped.ves"; "player_join.ves"; "player_leave.ves"; "player_death.ves"; "player_kill.ves"; "block_break.ves"; "block_place.ves"; "player_chat.ves"; "player_advancement.ves"; "server_tick.ves"; "timer_example.ves"; "hello.ves"; "player_use_block.ves"; "player_use_item.ves"; "player_respawn.ves"; "entity_damage.ves"; "player_drop_item.ves"]'
assert s.count(old) == 1, 'copy_config'
s = s.replace(old, new)

# 2) resident 事件列表 +5
old = 'events = ["server_started"; "server_stopped"; "player_join"; "player_leave"; "player_death"; "player_kill"; "block_break"; "block_place"; "player_chat"; "player_advancement"; "server_tick"; "timer_example"],'
new = 'events = ["server_started"; "server_stopped"; "player_join"; "player_leave"; "player_death"; "player_kill"; "block_break"; "block_place"; "player_chat"; "player_advancement"; "server_tick"; "timer_example"; "player_use_block"; "player_use_item"; "player_respawn"; "entity_damage"; "player_drop_item"],'
assert s.count(old) == 1, 'resident events'
s = s.replace(old, new)

# 3) resident 顶层 state 共享变量（头注释后加）
old = 'rd = rd + "   事件函数 _on_<event>(payload) 返回结果 dict/actions，主循环负责分发 */\\n",'
new = 'rd = rd + "   事件函数 _on_<event>(payload) 返回结果 dict/actions，主循环负责分发 */\\n",\n-rd = rd + "state = {},\\n",'
assert s.count(old) == 1, 'state'
s = s.replace(old, new)

# 4) 模组版本 ver 1.0.0 -> 1.3.0
old = 'ver = "1.0.0",'
new = 'ver = "1.3.0",'
assert s.count(old) == 1, 'ver'
s = s.replace(old, new)

# 5) README 生成文案：16 事件 / 14 动作 / tick_interval / 节流 / state
old = '''\trd = rd + "- 事件映射：config/vesna/events.json；默认 11 事件 + timers 定时（脚本返回 {\\"message\\": ...} 广播，{\\"actions\\": [...]} 执行动作）\\n",
\trd = rd + "- 脚本协议（单次模式）：argv[1] = JSON payload；stdout 最后一行 JSON 为返回结果\\n\\n",
\trd = rd + "## 常驻进程模式\\n- events.json 置 \\"resident\\": true 后，模组启动常驻 vesna 进程（resident.ves），事件经 stdin/stdout JSON 行协议分发，适合 server_tick 等高频事件；\\n- 命令 /<modid> <script> 与定时任务仍走单次进程。\\n",'''
if s.count(old) == 0:
    # 实际缩进为单个 '-' 前缀（mc_bridge 函数体）
    old = '''-rd = rd + "- 事件映射：config/vesna/events.json；默认 11 事件 + timers 定时（脚本返回 {\\"message\\": ...} 广播，{\\"actions\\": [...]} 执行动作）\\n",
-rd = rd + "- 脚本协议（单次模式）：argv[1] = JSON payload；stdout 最后一行 JSON 为返回结果\\n\\n",
-rd = rd + "## 常驻进程模式\\n- events.json 置 \\"resident\\": true 后，模组启动常驻 vesna 进程（resident.ves），事件经 stdin/stdout JSON 行协议分发，适合 server_tick 等高频事件；\\n- 命令 /<modid> <script> 与定时任务仍走单次进程。\\n",'''
new = '''-rd = rd + "- 事件映射：config/vesna/events.json；默认 16 事件 + timers 定时（脚本返回 {\\"message\\": ...} 广播，{\\"actions\\": [...]} 执行动作）\\n",
-rd = rd + "- 动作系统：message / command / give / kick / effect / tp / sound / log / title / actionbar / set_block / summon / spawn_particle / scoreboard 共 14 类\\n",
-rd = rd + "- 性能：events.json 顶层 \\"tick_interval\\" 控制 server_tick 频率（默认 20 tick = 1s）；事件级 \\"min_interval\\"（秒）做高频节流\\n",
-rd = rd + "- 共享状态：常驻模式下 resident.ves 顶层 \\"state\\" dict 可在事件函数间读写，做跨事件计数/统计\\n",
-rd = rd + "- 脚本协议（单次模式）：argv[1] = JSON payload；stdout 最后一行 JSON 为返回结果\\n\\n",
-rd = rd + "## 常驻进程模式\\n- events.json 置 \\"resident\\": true 后，模组启动常驻 vesna 进程（resident.ves），事件经 stdin/stdout JSON 行协议分发，适合 server_tick 等高频事件；\\n- 命令 /<modid> <script> 与定时任务仍走单次进程。\\n",'''
assert s.count(old) == 1, 'readme'
s = s.replace(old, new)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('mc.ves upgraded')
