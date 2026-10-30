# vesna-gui

Build graphical operation platforms (web UI) in pure Vesna.

使用纯 Vesna 编写图形化操作平台（Web 界面）。

## Overview / 概述

`vesna-gui` is an extra package (not part of the Vesna core). It lets Vesna scripts host a full
web application: HTML pages, static assets, JSON APIs, cookie sessions, SSE push, and multipart
file upload — all over a plain TCP HTTP server written in Vesna itself. No new builtins, no
third-party libraries, one thread per connection (`#thread`).

`vesna-gui` 是额外包（不加入 Vesna 本体）。它让 Vesna 脚本可以托管一个完整的 Web 应用：HTML
页面、静态资源、JSON API、Cookie 会话、SSE 推送与 multipart 文件上传——全部通过纯 Vesna 编写的
TCP HTTP 服务提供。不新增内置函数、不依赖第三方库，每连接一线程（`#thread`）。

## Install / 安装

```sh
vpm install vesna-gui
```

Then in your script / 然后在脚本中：

```ves
import vesna-gui,
```

## Server / 服务器

### gui_serve(port; routes; assets)

Start the web server (blocking). `routes` maps `"METHOD /path"` to handler names (path params via
`:name`), `assets` maps paths to raw string content.

启动 Web 服务（阻塞）。`routes` 将 `"METHOD /path"` 映射到处理函数名（路径参数用 `:name`），
`assets` 将路径映射到原始字符串内容。

```ves
routes = {
  "GET /": "page_home";
  "GET /api/u/:id": "api_user";
  "POST /api/todo/add": "api_todo_add"
},
assets = {"/static/app.js": js},
gui_serve('18081'; routes; assets),
```

### Request dict / 请求字典

Every handler receives one argument — the request dict:

每个处理函数接收一个参数——请求字典：

```ves
{
  "method"; "path"; "raw_path"; "query"; "headers"; "cookies";
  "body"; "form"; "files"; "params"     /* params = 路径参数 {id: ...} */
}
```

## Responses / 响应

A handler returns a string (HTML) or a dict; use the helpers below.

处理函数返回字符串（HTML）或字典；使用下面的辅助函数。

```ves
back(gui_page("标题"; inner; {"nav": nav; "current": "/"; "scripts": js})),  /* 完整页面 */
back(gui_json({"ok": true})),                                                /* JSON API   */
back(gui_redirect("/todo")),                                                 /* 302 重定向 */
back(gui_file(path)),                                                        /* 静态文件   */
back(gui_download(path; "name.txt")),                                        /* 附件下载   */
back(gui_404("not found")),                                                  /* 404        */
```

## Sessions / 会话

Cookie-based sessions, in-memory, guarded by `#lock`.

基于 Cookie 的内存会话，由 `#lock` 保护。

```ves
sess = gui_session_data(req),          /* 当前会话 dict（无会话则 {}） */
gui_session_set(req; "user"; "skirk"), /* 写入键值，返回带 Set-Cookie 的响应 */
gui_session_clear(req),                /* 删除会话并清除 Cookie */
```

## Template engine / 模板引擎

```ves
gui_tpl("Hello {{name}}!"; {"name": "<b>x</b>"}),  /* {{x}} 转义，{{!x}} 不转义 */
gui_tpl("{%for i in items%}[{{i}}]{%end%}"; {"items": ["a"; "b"]}),
gui_tpl("{%if on%}ON{%else%}OFF{%end%}"; {"on": "1"}),
```

## SSE push / 实时推送

Route a handler that returns one `data:` line per call; return `"STOP"` to close.

路由一个处理函数，每次调用返回一行 `data:`；返回 `"STOP"` 关闭连接。

```ves
def api_clock(req)-
  back(#date_format(#now(); "%H:%M:%S")),
,
/* route: "GET /api/clock": "api_clock" */
```

## Upload / 上传

`gui_upload_form(action; fields)` builds a multipart form; uploaded files arrive in
`req["files"]` as `{"name"; "b64"; "size"}`.

`gui_upload_form(action; fields)` 生成 multipart 表单；上传文件进入 `req["files"]`，
形如 `{"name"; "b64"; "size"}`。

```ves
gui_upload_form("/api/upload"; [{"name": "file"; "label": "选择文件"; "type": "file"}]),
```

## UI components / UI 组件

```ves
gui_page(title; inner; opts)          /* opts: {"nav"; "current"; "scripts"} */
gui_table(headers; rows)              /* 表格 */
gui_form(action; method; fields; submit)  /* 普通表单 */
gui_upload_form(action; fields)       /* 上传表单 */
gui_input(name; label; type; value)   /* 输入框 */
gui_button(label; onclick)            /* 按钮 */
gui_card(title; body)                 /* 卡片 */
gui_badge(text; kind)                 /* 徽标（默认/green/red/gray） */
gui_link(href; name)                  /* 链接 */
gui_kv(pairs)                         /* 键值对 */
gui_esc(s)                            /* HTML 转义 */
```

## Demo / 演示

`app.ves` is a small web file manager + editor with TODO and SSE clock demos: list, open, save,
delete and upload files under a root dir, plus a cookie-login demo. Run it and open
http://localhost:18081 :

`app.ves` 是一个小型 Web 文件管理器 + 编辑器，附带 TODO 与 SSE 时钟演示：列目录、打开、保存、
删除与上传文件，外加 Cookie 登录演示。运行后访问 http://localhost:18081：

```sh
vesna app.ves
```

## Notes / 说明

- The package implements HTTP over the core TCP primitives
  (`#tcp_listen` / `#tcp_accept` / `#tcp_send` / `#tcp_recv`) plus `#thread` per connection;
  the core `#http_server` is not required.
  本包基于内核 TCP 原语（`#tcp_listen` / `#tcp_accept` / `#tcp_send` / `#tcp_recv`）自行实现
  HTTP，每连接一线程（`#thread`），不依赖本体的 `#http_server`。
- Query strings, urlencoded forms, multipart bodies and cookies are parsed by the package;
  missing keys in `#get` are handled safely (no crash on absent Content-Type / Cookie).
  查询串、urlencoded 表单、multipart 请求体与 Cookie 均由本包解析；`#get` 缺键安全处理
  （无 Content-Type / Cookie 时不会崩溃）。
- Route values are handler **names** (strings), resolved with `#call`; path params via
  `:name` segments are collected into `req["params"]`.
  路由值为处理函数**名**（字符串），经 `#call` 调用；`:name` 路径参数收集到 `req["params"]`。

## Changelog / 更新日志

- **2.0.0**: sessions, template engine (`gui_tpl`), SSE (`gui_serve` → route-level), multipart
  upload (`gui_upload_form` / `req["files"]`), `gui_redirect` / `gui_file` / `gui_download` /
  `gui_404`, path params (`:name`), nav-based `gui_page`, request parsing hardened against
  missing headers/cookies.
  **2.0.0**：会话、模板引擎（`gui_tpl`）、SSE、multipart 上传（`gui_upload_form` /
  `req["files"]`）、`gui_redirect` / `gui_file` / `gui_download` / `gui_404`、路径参数
  （`:name`）、基于 nav 的 `gui_page`，并强化了缺失请求头/Cookie 时的解析容错。

## License / 许可证

MIT
