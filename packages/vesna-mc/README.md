# vesna-mc

Minecraft 桥接模组生成器（for Vesna）。生成 **Fabric / Forge / NeoForge**（MC **1.20.1+**）的完整桥接模组项目，让 Minecraft 服务端**事件、命令、定时任务**直接调用 Vesna 脚本。

```ves
import vesna-mc,
mc_bridge("F:\\my-mod"; "fabric"; "vesnamc"; "Vesna MC"; "com.example.vesnamc"; "1.20.1"),
```

## 函数

| 函数 | 说明 |
| --- | --- |
| `mc_bridge(dir; platform; modid; name; pkg; mcver)` | 生成桥接模组项目；`platform` 为 `fabric`/`forge`/`neoforge`，`mcver` 默认 `1.20.1`（可改为 1.20.2+） |

## 事件系统

脚本由 `config/vesna/events.json` 映射事件。`"resident": true` 启用常驻进程模式；顶层 `"tick_interval"`（默认 20 tick）控制 `server_tick` 频率；事件级 `"min_interval"`（秒）做高频节流；常驻模式下顶层 `state` dict 可在事件函数间共享数据。生成项目内置以下事件：

| 事件 | payload | 触发时机 |
| --- | --- | --- |
| `server_started` | — | 服务器启动 |
| `server_stopped` | — | 服务器停止 |
| `player_join` | player, uuid, op | 玩家加入 |
| `player_leave` | player, uuid, op | 玩家离开 |
| `player_death` | player, uuid, op, message | 玩家死亡 |
| `player_kill` | player, uuid, op, victim | 玩家击杀实体 |
| `block_break` | player, uuid, op, block, x, y, z | 破坏方块 |
| `block_place` | player, uuid, op, block, x, y, z | 放置方块 |
| `player_chat` | player, uuid, op, message | 聊天消息 |
| `player_advancement` | player, uuid, op, advancement | 达成进度 |
| `server_tick` | tick（秒） | 定时触发（`tick_interval` 可调，默认 20 tick） |
| `player_use_block` | player, block, x, y, z, hand | 使用/交互方块 |
| `player_use_item` | player, item, hand | 使用物品 |
| `player_respawn` | player, uuid, op | 玩家重生 |
| `entity_damage` | entity, attacker, amount | 实体受伤（可配 `min_interval` 节流） |
| `player_drop_item` | player, item, count | 丢弃物品 |

定时任务：`events.json` 的 `"timers"` 数组声明周期脚本（`{"script": "...", "seconds": N}`）。

## 动作系统

脚本返回 JSON 结果，模组执行其中动作：

```json
{
  "actions": [
    {"type": "message", "target": "all", "text": "..."},
    {"type": "message", "target": "player:Alex", "text": "私聊"},
    {"type": "command", "command": "say hi"},
    {"type": "give", "player": "Alex", "item": "minecraft:diamond", "count": 3},
    {"type": "kick", "player": "Alex", "reason": "bye"},
    {"type": "effect", "player": "Alex", "effect": "minecraft:speed", "duration": 30, "level": 2},
    {"type": "tp", "player": "Alex", "x": 0, "y": 100, "z": 0},
    {"type": "sound", "player": "Alex", "sound": "minecraft:block.note_block.pling"},
    {"type": "title", "player": "Alex", "title": "你好", "subtitle": "来自 Vesna"},
    {"type": "actionbar", "player": "Alex", "text": "动作栏消息"},
    {"type": "set_block", "x": 0, "y": 100, "z": 0, "block": "minecraft:diamond_block"},
    {"type": "summon", "entity": "minecraft:creeper", "x": 0, "y": 100, "z": 0},
    {"type": "spawn_particle", "particle": "minecraft:flame", "x": 0, "y": 100, "z": 0, "count": 20},
    {"type": "scoreboard", "player": "Alex", "objective": "kills", "score": 10},
    {"type": "log", "text": "server log"}
  ]
}
```

快捷：`{"message": "text"}` 等价于广播消息。脚本里 `payload` 经 `args['1']` 传入（JSON 字符串，用 `#json_decode` 解析）。

## 命令

- `/<modid> <script> [args...]` 直接执行指定脚本
- `/<modid>-reload` 重载 events.json（含 timers）

## 运行模式

### 常驻进程模式（默认）

`events.json` 置 `"resident": true`（默认已开启）后，模组启动时拉起一个常驻 `vesna` 进程（`config/vesna/scripts/resident.ves`）：

- **协议**：stdin 每行一个 JSON 请求 `{"id": N, "event": "...", "payload": {...}}`；stdout 每行一个 JSON 响应 `{"id": N, "result": {...}}`
- **事件函数**：resident.ves 内置 `_on_<event>(payload)` 函数（由事件脚本模板自动转换，print 改为 return），收到事件即分发
- **动作执行**：响应中的 result 由模组侧 `runActions` 执行（8 类动作与单次模式一致）
- **适用**：`server_tick` 等高频事件——进程常驻、一次启动，无冷启动开销

> 定时任务与 `/modid <script>` 命令仍走单次进程（频率低，无需常驻）。

### 单次进程模式

将 `events.json` 的 `"resident"` 改为 `false`（或删除），回到逐事件启动短生命周期 `vesna` 进程（30s 超时）的旧模式。

## 运行机制

- **进程桥**：常驻模式一个长生命周期进程；单次模式每个事件/命令/定时触发启动短生命周期 `vesna` 进程（30s 超时）；脚本 `argv[1]` 收 JSON payload，`stdout` 最后一行 JSON 为返回结果。
- **定位运行时**：`config/vesna/runtime.properties` 的 `vesna.path` → 环境变量 `VESNA_HOME` → PATH。
- **数据持久化**：脚本可直接读写 `config/vesna/data/` 下的文件（如 `#fwrite`/`#fread` 做计数、配置、经济数据）。

## 构建

```bash
cd <dir>
gradlew build    # 需 JDK 17+；首次下载依赖
```

版本表默认指向 **1.20.1**（Fabric yarn 1.20.1+build.10 / Forge 47.2.0 / NeoForge 47.1.106）。换 MC 版本时同步调整 build.gradle 中对应依赖版本。

## 说明

- 桥接类（`VesnaBridge` / `VesnaJson`）为**纯 Java**（仅 JDK），可独立编译验证；平台入口依赖各 loader API。
- 当前适合低频事件与命令；每 tick 调用请等待后续常驻进程模式。
- 生成项目需用户本机装 JDK 17+ 与 Vesna 运行时；本包只生成代码，不做构建。
