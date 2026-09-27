# vesna-java

Java 互操作与 Minecraft 模组工具包（for Vesna）。需要 Vesna **2.6+**（依赖 `#dir_walk` / `#url_parse` 等新内置）。

```ves
import java,
```

## 环境

| 函数 | 说明 |
| --- | --- |
| `java_home()` | 返回 `JAVA_HOME` 或 `""` |
| `java_check()` | 检测环境，返回 `{java; javac; home}` |
| `java_path()` | 返回可用的 `java` 可执行文件路径（找不到回退 `"java"`） |
| `java_run(classpath; main_class; args_list)` | 运行 Java 主类（`-cp <cp> <main> [args...]`），返回退出码 |

## 源码与项目生成

| 函数 | 说明 |
| --- | --- |
| `java_class(pkg; name; fields_list)` | 生成 Java 类源码（字段、无参/全参构造器、getter/setter） |
| `gradle_project(dir; name; main_class)` | 生成 Gradle Java 项目（application 插件，`gradle run` 即用） |
| `maven_pom(dir; group; artifact; ver; main_class)` | 生成 Maven `pom.xml` |

## Minecraft 模组（mcmod）

| 函数 | 说明 |
| --- | --- |
| `mod_fabric(dir; id; name; ver; desc; pkg)` | 生成 Fabric 模组骨架：`fabric.mod.json` + 主类 + fabric-loom `build.gradle` |
| `mod_forge(dir; id; name; ver; desc; pkg)` | 生成 Forge 模组骨架：`mods.toml` + 主类 + forgegradle `build.gradle`（1.20.4） |
| `mod_neoforge(dir; id; name; ver; desc; pkg)` | 生成 NeoForge 模组骨架：`neoforge.mods.toml` + 主类 + moddev `build.gradle`（21.1） |
| `mcmeta(modid; name; desc)` | 生成 mcmod 百科条目 JSON |

生成的模组骨架可直接放入 Minecraft 开发环境（还需 Gradle wrapper 与官方 MDK 的 `gradlew` 引导；`mod_*` 系列不调用任何外部命令，仅生成文件）。

## 示例

```ves
import java,
c = java_check(),
if c["java"]-
-print("Java OK: " + c["home"]),
-,
/* 生成一个 Fabric 模组骨架 */
mod_fabric("F:\\MyMod"; "mymod"; "My Mod"; "1.0.0"; "A demo mod"; "com.example.mymod"),
```
