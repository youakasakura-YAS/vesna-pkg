# vesnamc（Vesna 桥接模组 · fabric）

把 Vesna 脚本接入 Minecraft（MC 1.20.1+）。脚本放 config/vesna/scripts/，事件映射在 config/vesna/events.json。

## 前置
- 安装 Vesna（vesna.exe --install），或设置 VESNA_HOME / config/vesna/runtime.properties 的 vesna.path

## 构建
- gradlew build（需 JDK 17+；首次会下载依赖）
- 换 MC 版本：修改 mcver 并调整 build.gradle 中对应版本表（Forge/NeoForge 的 47.x / fabric-api 版本随 MC 变化）

## 使用
- 命令：/vesnamc <script> [args...] 执行脚本；/vesnamc-reload 重载 events.json
- 事件映射：config/vesna/events.json；默认 16 事件 + timers 定时（脚本返回 {"message": ...} 广播，{"actions": [...]} 执行动作）
- 动作系统：message / command / give / kick / effect / tp / sound / log / title / actionbar / set_block / summon / spawn_particle / scoreboard 共 14 类
- 性能：events.json 顶层 "tick_interval" 控制 server_tick 频率（默认 20 tick = 1s）；事件级 "min_interval"（秒）做高频节流
- 共享状态：常驻模式下 resident.ves 顶层 "state" dict 可在事件函数间读写，做跨事件计数/统计
- 脚本协议（单次模式）：argv[1] = JSON payload；stdout 最后一行 JSON 为返回结果

## 常驻进程模式
- events.json 置 "resident": true 后，模组启动常驻 vesna 进程（resident.ves），事件经 stdin/stdout JSON 行协议分发，适合 server_tick 等高频事件；
- 命令 /<modid> <script> 与定时任务仍走单次进程。
