# vesna-email

Send email and run a local mailbox in pure Vesna.

使用纯 Vesna 发送邮件、运行本地邮箱。

## Overview / 概述

`vesna-email` is an extra package (not part of the Vesna core). It implements an SMTP client
(send mail, with optional AUTH LOGIN) and a simplified SMTP server (local mailbox: receive mail
and save it to disk as `.eml` files) — all in Vesna itself, using only the core TCP primitives.
No new builtins, no third-party libraries, almost no shell usage.

`vesna-email` 是额外包（不加入 Vesna 本体）。它实现了 SMTP 客户端（发送邮件，支持可选 AUTH LOGIN
认证）和简化 SMTP 服务端（本地邮箱：接收邮件并以 `.eml` 文件落盘）——全部用 Vesna 自身实现，
仅使用内核 TCP 原语。不新增内置函数、不依赖第三方库、几乎不使用命令。

## Install / 安装

```sh
vpm install vesna-email
```

Then in your script / 然后在脚本中：

```ves
import email,
```

## API

### smtp_send(server; port; user; pass; from; to; subject; body)

Send an email. `server` is the SMTP host, `port` a numeric literal like `'587'`.
Pass empty strings `""` for `user`/`pass` to skip authentication.
Returns `"sent"` on success, or an `"error: ..."` string.

发送邮件。`server` 为 SMTP 服务器，`port` 为数字字面量如 `'587'`。
`user`/`pass` 传空字符串 `""` 表示跳过认证。成功返回 `"sent"`，失败返回 `"error: ..."`。

```ves
r = smtp_send("smtp.example.com"; '587'; "user"; "pass"; "a@example.com"; "b@example.com"; "标题"; "正文"),
print(r),
```

### smtp_server(port; dir)

Start a local mailbox (blocking). Receives mail via a simplified SMTP protocol and saves each
message to `dir\NNNN.eml`. Also answers `AUTH LOGIN` with success.

启动本地邮箱（阻塞）。通过简化 SMTP 协议收信，每封信保存为 `dir\NNNN.eml`。对 `AUTH LOGIN`
认证也返回成功。

```ves
smtp_server('8025'; "C:\\tmp\\mailbox"),
```

You can then point any mail client (or `smtp_send`) at `127.0.0.1:8025`.

之后可用任意邮件客户端（或 `smtp_send`）向 `127.0.0.1:8025` 发信。

### mail_list(dir)

List received mails as a list of `{"file": ...; "size": ...}` dicts.

列出已收邮件，返回 `{"file": ...; "size": ...}` 字典列表。

### mail_read(dir; idx)

Read the `idx`-th mail (1-based) as raw text.

读取第 `idx` 封邮件（从 1 开始）的原始文本。

## Notes / 说明

- This is a working, minimal SMTP implementation for local automation; it does not implement
  STARTTLS or full RFC 5321 coverage. Use `smtp_send` against a real SMTP server for production
  sending (prefer port `'587'` with auth, or port `'25'` without auth where allowed).
  这是面向本地自动化的可运行最小 SMTP 实现，未实现 STARTTLS 或完整 RFC 5321。
  生产环境发送请使用真实 SMTP 服务器（建议 `'587'` 端口 + 认证；或允许时用 `'25'` 无认证）。
- The local mailbox is intentionally simple: it stores raw messages, does not enforce quotas
  or spam checks, and is not a security boundary.
  本地邮箱刻意保持简单：保存原始邮件，不限制配额、不做垃圾邮件检测，也不是安全边界。

## License / 许可证

MIT
