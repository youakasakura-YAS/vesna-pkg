# vesna-gui

Build graphical operation platforms (web UI) in pure Vesna.

使用纯 Vesna 编写图形化操作平台（Web 界面）。

## Overview / 概述

`vesna-gui` is an extra package (not part of the Vesna core). It lets Vesna scripts host a small
web application: serve HTML pages, static assets and JSON APIs over a plain TCP HTTP server,
all written in Vesna itself — no new builtins, no third-party libraries, almost no shell usage.

`vesna-gui` 是额外包（不加入 Vesna 本体）。它让 Vesna 脚本可以托管一个小型 Web 应用：通过纯 TCP
HTTP 服务提供 HTML 页面、静态资源与 JSON API，全部用 Vesna 自身编写——不新增内置函数、
不依赖第三方库、几乎不使用命令。

## Install / 安装

```sh
vpm install vesna-gui
```

Then in your script / 然后在脚本中：

```ves
import gui,
```

## API

### gui_serve(port; routes; assets)

Start the web server (blocking). `routes` maps paths to handler function names,
`assets` maps paths to raw string content.

启动 Web 服务（阻塞）。`routes` 将路径映射到处理函数名，`assets` 将路径映射到原始字符串内容。

```ves
routes = {"/": "page_home"; "/api/list": "api_list"},
assets = {"/static/app.js": js},
gui_serve('8080'; routes; assets),
```

### gui_page(title; inner)

Return a full HTML page string with the built-in style, sidebar placeholder and a `#main` container.

返回完整 HTML 页面字符串（内置样式、侧栏占位、`#main` 容器）。

```ves
back(gui_page("我的页面"; "<div class=\"main\">content</div>")),
```

### gui_table(headers; rows)

Return an HTML table string. `headers` is a list of column names, `rows` a list of row lists.

返回 HTML 表格字符串。`headers` 为列名列表，`rows` 为行列表。

### gui_side(items; current)

Return a sidebar HTML string. `items` is a list of dicts `{"name": ...; "href": ...}`.

返回侧栏 HTML 字符串。`items` 为 `{"name": ...; "href": ...}` 字典列表。

### gui_esc(s)

HTML-escape a string.

对字符串做 HTML 转义。

### gui_json(v)

`#json_encode` wrapper returning a dict `{"code": '200'; "type": "application/json"; "body": ...}`.

`#json_encode` 的封装，返回 `{"code": '200'; "type": "application/json"; "body": ...}` 字典。

## Demo / 演示

`app.ves` is a small web file manager + editor: list files, create files, open and save text files,
with a Chinese UI. Run it and open http://localhost:18081 :

`app.ves` 是一个小型 Web 文件管理器 + 编辑器：列出文件、新建文件、打开与保存文本文件，中文界面。
运行后访问 http://localhost:18081：

```sh
vesna app.ves
```

## Notes / 说明

- Handler functions receive one argument: the request dict
  `{"method"; "path"; "query"; "headers"; "body"}`.
  处理函数接收一个参数：请求字典 `{"method"; "path"; "query"; "headers"; "body"}`。
- Responses from handlers can be a string (HTML) or a dict
  `{"code": ...; "type": ...; "body": ...}`. Return `gui_json(...)` for JSON APIs.
  处理函数返回值可以是字符串（HTML）或字典 `{"code": ...; "type": ...; "body": ...}`。
  JSON API 请返回 `gui_json(...)`。
- The built-in `#http_server` from the core is not required here; this package implements
  HTTP over the core TCP primitives (`#tcp_listen` / `#tcp_accept` / `#tcp_send` / `#tcp_recv`)
  plus `#thread` per connection.
  本包不依赖本体的 `#http_server`，而是基于内核 TCP 原语
  （`#tcp_listen` / `#tcp_accept` / `#tcp_send` / `#tcp_recv`）自行实现 HTTP，
  每连接一线程（`#thread`）。

## License / 许可证

MIT
