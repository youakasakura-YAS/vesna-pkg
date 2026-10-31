# vesna-gui

Build graphical operation platforms (web UI) in pure Vesna.

使用纯 Vesna 编写图形化操作平台（Web 界面）。

## Overview / 概述

`vesna-gui` is an extra package (not part of the Vesna core). It lets Vesna scripts host a full
web application: HTML pages, static assets, JSON APIs, cookie sessions, SSE push, and multipart
file upload — all over a plain TCP HTTP server written in Vesna itself. Since 3.0 it also offers a
front-end-style **component model**: a component tree (`gui_h` / `gui_render`), a server-side state
store (`gui_state_*`), and a small SPA runtime (`gui_script_spa`) so interactive pages behave like
a JS front-end framework while all logic stays in Vesna. No new builtins, no third-party
libraries, one thread per connection (`#thread`).

`vesna-gui` 是额外包（不加入 Vesna 本体）。它让 Vesna 脚本可以托管一个完整的 Web 应用：HTML
页面、静态资源、JSON API、Cookie 会话、SSE 推送与 multipart 文件上传——全部通过纯 Vesna 编写的
TCP HTTP 服务提供。自 3.0 起还提供前端式**组件模型**：组件树（`gui_h` / `gui_render`）、
服务端状态存储（`gui_state_*`）与小型 SPA 运行时（`gui_script_spa`），让交互页面像 JS 前端
框架一样工作，而全部逻辑仍在 Vesna 中。不新增内置函数、不依赖第三方库，每连接一线程
（`#thread`）。

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

## Component model 3.0 / 组件模型（3.0）

A front-end-style component model: build a **component tree** with `gui_h`, render it with
`gui_render`, keep UI state in the **server-side store** (`gui_state_*`), and drive events with
the small **SPA runtime** (`gui_script_spa`). All 2.0 APIs remain available.

前端式组件模型：用 `gui_h` 构建**组件树**，用 `gui_render` 渲染，用**服务端状态存储**
（`gui_state_*`）保存界面状态，用小型 **SPA 运行时**（`gui_script_spa`）驱动事件。
所有 2.0 API 保持可用。

### Component tree / 组件树

```ves
/* gui_h(tag; attrs; children) -> vnode；children 为字符串/数字（自动转义）、vnode 或列表 */
v = gui_h("div"; {"class": "col"; "style": "gap:12px"}; [
  gui_h("h3"; {}; "标题"),
  gui_h("button"; {"data-action": "inc"; "data-href": "/api/count/inc"}; "＋1")
]),
inner = gui_render(v),  /* 渲染为 HTML 字符串，文本自动转义 */
```

- 文本子节点自动转义；`attrs` 中的 `on*` 事件属性与 `attrs.html`（原样 HTML）不转义。
- 自闭合标签（input / br / img / meta / link / hr / textarea 除外）自动输出 `<tag/>` 形式。
- 布局辅助：`gui_flex(children; dir; gap)`（`dir` = `"row"` / `"col"`）、`gui_row_html` /
  `gui_col_html`（原样 HTML 行/列）。
- 更多控件：`gui_textarea(name; value)`、`gui_select(name; options; value)`、
  `gui_checkbox(name; checked)`。

### Server-side state / 服务端状态

```ves
gui_state_set("count"; "1"),   /* 写入（#lock 保护） */
gui_state_get("count"),        /* 读取；不存在返回 none */
gui_state_del("count"),        /* 删除 */
```

状态是进程级全局（所有连接共享），在 API 处理器中读写即可实现跨请求持久化，例如计数器：

```ves
def api_count_inc(req)-
  cnt = #int(gui_state_get("count")) + '1',
  gui_state_set("count"; #str(cnt)),
  back(gui_json({"ok": true; "msg": "count = " + #str(cnt); "reload": true})),
,
```

### SPA runtime / SPA 运行时

`gui_script_spa()` 返回一段 JS 字符串，注入页面后自动：

- 拦截所有 `<form>` 提交 → `fetch` POST（FormData），响应 `{"reload": true}` 时刷新页面，
  否则弹 toast 显示 `msg`；
- 拦截所有 `[data-action]` 按钮点击 → `fetch` POST 到 `data-href`（无 href 时回退到表单）；
- 提供全局 `showToast(msg; kind)`。

```ves
back(gui_page("组件化 Demo"; inner; {"scripts": gui_script_spa()})),
```

组件化示例见 `_app3.ves`（计数器 + 待办，端口 18082），全部用 3.0 API 编写。
A full component-model example lives in `_app3.ves` (counter + todo, port 18082), written
entirely with the 3.0 API.

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

- **3.0.0**: front-end-style component model — component tree (`gui_h` / `gui_render`),
  server-side state store (`gui_state_set` / `gui_state_get` / `gui_state_del`, `#lock`-guarded),
  SPA runtime (`gui_script_spa`: form fetch-POST, `[data-action]` buttons, toast),
  new controls (`gui_textarea` / `gui_select` / `gui_checkbox` / `gui_row_html` /
  `gui_col_html` / `gui_flex`), `.flex`/`.flex.col`/`.grow`/`.muted` styles. All 2.0 APIs
  remain unchanged and available.
  **3.0.0**：前端式组件模型——组件树（`gui_h` / `gui_render`）、服务端状态存储
  （`gui_state_set` / `gui_state_get` / `gui_state_del`，`#lock` 保护）、SPA 运行时
  （`gui_script_spa`：表单 fetch-POST、`[data-action]` 按钮、toast）、新控件
  （`gui_textarea` / `gui_select` / `gui_checkbox` / `gui_row_html` / `gui_col_html` /
  `gui_flex`）、`.flex`/`.flex.col`/`.grow`/`.muted` 样式。所有 2.0 API 保持不变并可用。

- **2.0.0**: sessions, template engine (`gui_tpl`), SSE (`gui_serve` → route-level), multipart
  upload (`gui_upload_form` / `req["files"]`), `gui_redirect` / `gui_file` / `gui_download` /
  `gui_404`, path params (`:name`), nav-based `gui_page`, request parsing hardened against
  missing headers/cookies.
  **2.0.0**：会话、模板引擎（`gui_tpl`）、SSE、multipart 上传（`gui_upload_form` /
  `req["files"]`）、`gui_redirect` / `gui_file` / `gui_download` / `gui_404`、路径参数
  （`:name`）、基于 nav 的 `gui_page`，并强化了缺失请求头/Cookie 时的解析容错。

## License / 许可证

MIT
