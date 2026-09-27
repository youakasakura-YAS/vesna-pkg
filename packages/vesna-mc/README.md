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

脚本由 `config/vesna/events.json` 映射事件。生成项目内置以下事件：

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
| `server_tick` | tick（秒） | 每秒一次（低频） |

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
    {"type": "log", "text": "server log"}
  ]
}
```

快捷：`{"message": "text"}` 等价于广播消息。脚本里 `payload` 经 `args['1']` 传入（JSON 字符串，用 `#json_decode` 解析）。

## 命令

- `/<modid> <script> [args...]` 直接执行指定脚本
- `/<modid>-reload` 重载 events.json（含 timers）

## 运行机制

- **进程桥**：每个事件/命令/定时触发启动短生命周期 `vesna` 进程（30s 超时）；脚本 `argv[1]` 收 JSON payload，`stdout` 最后一行 JSON 为返回结果。
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
