# vesna-mc

Minecraft 桥接模组生成器（for Vesna）。生成 **Fabric / Forge / NeoForge**（MC **1.20.1+**）的完整桥接模组项目，让 Minecraft 服务端事件与命令直接调用 Vesna 脚本。

```ves
import vesna-mc,
mc_bridge("F:\\my-mod"; "fabric"; "vesnamc"; "Vesna MC"; "com.example.vesnamc"; "1.20.1"),
```

## 函数

| 函数 | 说明 |
| --- | --- |
| `mc_bridge(dir; platform; modid; name; pkg; mcver)` | 生成桥接模组项目；`platform` 为 `fabric`/`forge`/`neoforge`，`mcver` 默认 `1.20.1`（可改为 1.20.2+） |

## 生成内容

```
<dir>/
├── build.gradle / settings.gradle / gradle.properties   # 平台构建配置
├── README.md                                            # 用法说明
├── config/vesna/
│   ├── events.json              # 事件 → 脚本映射
│   ├── runtime.properties       # vesna 运行时路径（可留空）
│   └── scripts/                 # 示例脚本（server_started / player_join / hello）
└── src/main/
    ├── java/<pkg>/
    │   ├── VesnaBridge.java     # 进程桥（纯 Java，无外部依赖）
    │   ├── VesnaJson.java       # 极简 JSON 解析/序列化
    │   └── VesnaMod.java        # 平台入口
    └── resources/
        ├── fabric.mod.json / META-INF/mods.toml / META-INF/neoforge.mods.toml
        └── pack.mcmeta
```

## 运行机制

- **协议**：每个事件/命令启动一个短生命周期 `vesna` 进程；脚本 `argv[1]` 收到 JSON payload，`stdout` 最后一行 JSON 为返回结果。
- **定位运行时**：`config/vesna/runtime.properties` 的 `vesna.path` → 环境变量 `VESNA_HOME` → PATH。
- **命令**：`/modid <script> [args...]` 执行脚本；`/modid-reload` 重载 events.json。
- **内置事件**：`server_started`（服务器启动）、`player_join`（玩家加入，payload 含 player/uuid）。脚本返回 `{"message": ...}` 会广播到聊天。

## 构建

```bash
cd <dir>
gradlew build    # 需 JDK 17+；首次下载依赖
```

版本表默认指向 **1.20.1**（Fabric yarn 1.20.1+build.10 / Forge 47.2.0 / NeoForge 47.1.106）。换 MC 版本时同步调整 build.gradle 中对应依赖版本（Fabric API、Forge/NeoForge 47.x 均随 MC 版本变化）。

## 说明

- 桥接类为**纯 Java**（仅 JDK），不依赖任何 MC API，可独立编译验证。
- 当前适合低频事件与命令；每 tick 调用请等待后续常驻进程模式。
- 生成项目需用户本机装 JDK 17+ 与 Vesna 运行时；本包只生成代码，不做构建。
