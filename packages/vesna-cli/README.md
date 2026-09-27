# vesna-cli

Command-line utilities for Vesna scripts: argument parsing, ANSI colors, aligned tables, progress bars and stdin prompts.

用 Vesna 编写命令行工具的辅助包：参数解析、ANSI 颜色、对齐表格、进度条与 stdin 交互。

## Install / 安装

```sh
vpm install vesna-cli
```

Then in your script / 然后在脚本中：

```ves
import vesna-cli,
```

## API

### cli_parse(argv)

Parse an argument list (typically `#args()`) into
`{"args": [...]; "flags": {name: value}}`. Supports `--key=value`, `--flag` and `-f`; plain arguments are kept in order.

解析参数列表（通常传 `#args()`）为 `{"args": [...]; "flags": {name: value}}`。支持 `--key=value`、`--flag` 与 `-f`；普通参数按顺序保留。

```ves
p = cli_parse(#args()),
# prints {"args":["x","y"],"flags":{"name":"v","verbose":"true"}}
```

### cli_color(text; code)

Wrap `text` in an ANSI color code, e.g. `"31"` red, `"32"` green, `"33"` yellow, `"36"` cyan. Use with `#platform()` checks if the terminal may not support ANSI.

给文本套上 ANSI 颜色码，如 `"31"` 红、`"32"` 绿、`"33"` 黄、`"36"` 青。终端不支持时可结合 `#platform()` 判断。

### cli_table(headers; rows)

Render an aligned text table. `headers` is a list of column names, `rows` a list of dicts keyed by those names.

渲染对齐的文本表格。`headers` 为列名列表，`rows` 为以列名为键的字典列表。

```ves
print(cli_table(["name"; "age"]; [{"name": "alice"; "age": '3'}; {"name": "bob"; "age": '27'}])),
```

### cli_progress(cur; total; width)

Return a progress-bar string like `[##------] 30%`.

返回进度条字符串，如 `[##------] 30%`。

### cli_confirm(msg)

Print `msg [y/N]:`, read one line from stdin and return `true` on `y`/`yes` (case-insensitive), otherwise `false`.

打印 `msg [y/N]:`，从 stdin 读一行，输入 `y`/`yes`（不区分大小写）返回 `true`，否则 `false`。

### cli_ask(msg)

Print `msg: `, read one line and return it trimmed (empty string on EOF).

打印 `msg: `，读一行并返回去除首尾空白的结果（EOF 时返回空字符串）。

## License / 许可协议
MIT
