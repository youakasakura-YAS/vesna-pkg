# vesna-dev

通用软件开发工具包（for Vesna）。需要 Vesna **2.6+**（依赖 `#dir_walk` 等新内置）。

```ves
import dev,
```

## 项目脚手架

| 函数 | 说明 |
| --- | --- |
| `dev_scaffold(dir; name; lang)` | 生成通用项目骨架（README + MIT LICENSE + .gitignore + src/） |
| `dev_readme(name; desc; lang)` | README 模板（`zh`/`en`） |
| `dev_license_mit(author; year)` | MIT 许可证全文 |
| `dev_gitignore(lang)` | .gitignore 模板（python/java/node/cpp/go/rust 等） |

## 版本与变更

| 函数 | 说明 |
| --- | --- |
| `dev_semver_inc(ver; part)` | 语义化版本自增（`major`/`minor`/`patch`），如 `"1.2.3"` + `minor` → `"1.3.0"` |
| `dev_changelog_add(file; ver; date; entries_list)` | 向 changelog 文件追加版本条目 |

## 工具

| 函数 | 说明 |
| --- | --- |
| `dev_log(msg)` | 返回带时间戳的日志行 `[YYYY-MM-DD HH:MM:SS] msg` |
| `dev_todo_scan(dir)` | 递归扫描源码中的 `TODO`/`FIXME` 注释，返回文件列表 |
| `dev_line_count(dir)` | 递归统计代码行数，返回 `{files; lines}` |
| `dev_env(name; dflt)` | 读取环境变量，未设置时返回默认值 |

## 示例

```ves
import dev,
print(dev_semver_inc("1.2.3"; "minor")),
dev_scaffold("F:\\myproj"; "myproj"; "python"),
st = dev_line_count("F:\\Vesna\\src\\cpp"),
print(st["files"] + " files, " + st["lines"] + " lines"),
```
